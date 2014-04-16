/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.l2switch.md.packet;

import org.opendaylight.controller.sample.l2switch.md.addresstracker.AddressTracker;
import org.opendaylight.controller.sample.l2switch.md.flow.FlowWriterService;
import org.opendaylight.controller.sample.l2switch.md.inventory.InventoryService;
import org.opendaylight.controller.sample.l2switch.md.util.InstanceIdentifierUtils;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.LLDP;
import org.opendaylight.controller.sal.packet.LinkEncap;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.address.tracker.rev140402.l2.addresses.L2Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

/**
 * PacketHandler examines Ethernet packets to find L2Addresses (mac, nodeConnector) pairings
 * of the sender and learns them.
 * It also forwards the data packets appropriately dependending upon whether it knows about the
 * target or not.
 */
public class PacketHandler implements PacketProcessingListener {

  private final static Logger _logger = LoggerFactory.getLogger(PacketHandler.class);

  private PacketProcessingService packetProcessingService;
  private AddressTracker addressTracker;
  private FlowWriterService flowWriterService;
  private InventoryService inventoryService;

  public void setAddressTracker(AddressTracker addressTracker) {
    this.addressTracker = addressTracker;
  }

  public void setPacketProcessingService(PacketProcessingService packetProcessingService) {
    this.packetProcessingService = packetProcessingService;
  }

  public void setFlowWriterService(FlowWriterService flowWriterService) {
    this.flowWriterService = flowWriterService;
  }

  public void setInventoryService(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }

  /**
   * The handler function for all incoming packets.
   * @param packetReceived  The incoming packet.
   */
  @Override
  public void onPacketReceived(PacketReceived packetReceived) {

    if(packetReceived == null) return;

    try {
      byte[] payload = packetReceived.getPayload();
      RawPacket rawPacket = new RawPacket(payload);
      NodeConnectorRef ingress = packetReceived.getIngress();

      Packet packet = decodeDataPacket(rawPacket);

      if(!(packet instanceof Ethernet)) return;

      handleEthernetPacket(packet, ingress);

    } catch(Exception e) {
      _logger.error("Failed to handle packet {}", packetReceived, e);
    }
  }

  /**
   * The handler function for Ethernet packets.
   * @param packet  The incoming Ethernet packet.
   * @param ingress  The NodeConnector where the Ethernet packet came from.
   */
  private void handleEthernetPacket(Packet packet, NodeConnectorRef ingress) {
    byte[] srcMac = ((Ethernet) packet).getSourceMACAddress();
    byte[] destMac = ((Ethernet) packet).getDestinationMACAddress();

    if (srcMac  == null || srcMac.length  == 0) return;

    Object enclosedPacket = packet.getPayload();

    if (enclosedPacket instanceof LLDP)
      return; // LLDP packets are handled by OpenFlowPlugin

    // get l2address by src mac
    // if unknown, add l2address
    MacAddress srcMacAddress = toMacAddress(srcMac);
    L2Address src = addressTracker.getAddress(srcMacAddress);
    boolean isSrcKnown = (src != null);
    if (!isSrcKnown) {
      addressTracker.addAddress(srcMacAddress, ingress);
    }

    // get host by dest mac
    // if known set dest known to true
    MacAddress destMacAddress = toMacAddress(destMac);
    L2Address dest = addressTracker.getAddress(destMacAddress);
    boolean isDestKnown = (dest != null);

    byte[] payload = packet.getRawPayload();
    // if (src and dest known)
    // sendpacket to dest and add src<->dest flow
    if(isSrcKnown & isDestKnown) {
      flowWriterService.addMacToMacFlowsUsingShortestPath(srcMacAddress, src.getNodeConnectorRef(),
          destMacAddress, dest.getNodeConnectorRef());
      sendPacketOut(payload, getControllerNodeConnector(dest.getNodeConnectorRef()), dest.getNodeConnectorRef());
    } else {
      // if (dest unknown)
      // sendpacket to external links minus ingress
      floodExternalPorts(payload, ingress);
    }
  }

  /**
   * Floods the specified payload on external ports, which are ports not connected to switches.
   * @param payload  The payload to be flooded.
   * @param ingress  The NodeConnector where the payload came from.
   */
  private void floodExternalPorts(byte[] payload, NodeConnectorRef ingress) {
    List<NodeConnectorRef> externalPorts = inventoryService.getExternalNodeConnectors();
    externalPorts.remove(ingress);

    for (NodeConnectorRef egress : externalPorts) {
      sendPacketOut(payload, getControllerNodeConnector(egress), egress);
    }
  }

  /**
   * Sends the specified packet on the specified port.
   * @param payload  The payload to be sent.
   * @param ingress  The NodeConnector where the payload came from.
   * @param egress  The NodeConnector where the payload will go.
   */
  private void sendPacketOut(byte[] payload, NodeConnectorRef ingress, NodeConnectorRef egress) {
    if (ingress == null || egress == null)  return;
    InstanceIdentifier<Node> egressNodePath = InstanceIdentifierUtils.getNodePath(egress.getValue());
    TransmitPacketInput input = new TransmitPacketInputBuilder() //
        .setPayload(payload) //
        .setNode(new NodeRef(egressNodePath)) //
        .setEgress(egress) //
        .setIngress(ingress) //
        .build();
    packetProcessingService.transmitPacket(input);
  }

  /**
   * Decodes an incoming packet.
   * @param raw  The raw packet to be decoded.
   * @return  The decoded form of the raw packet.
   */
  private Packet decodeDataPacket(RawPacket raw) {
    if(raw == null) {
      return null;
    }
    byte[] data = raw.getPacketData();
    if(data.length <= 0) {
      return null;
    }
    if(raw.getEncap().equals(LinkEncap.ETHERNET)) {
      Ethernet res = new Ethernet();
      try {
        res.deserialize(data, 0, data.length * NetUtils.NumBitsInAByte);
        res.setRawPayload(raw.getPacketData());
      } catch(Exception e) {
        _logger.warn("Failed to decode packet: {}", e.getMessage());
      }
      return res;
    }
    return null;
  }

  /**
   * Creates a MacAddress object out of a byte array.
   * @param dataLinkAddress  The byte-array form of a MacAddress
   * @return  MacAddress of the specified dataLinkAddress.
   */
  private MacAddress toMacAddress(byte[] dataLinkAddress) {
    return new MacAddress(HexEncode.bytesToHexStringFormat(dataLinkAddress));
  }

  /**
   * Gets the NodeConnector that connects the controller & switch for a specified switch port/node connector.
   * @param nodeConnectorRef  The nodeConnector of a switch.
   * @return  The NodeConnector that that connects the controller & switch.
   */
  private NodeConnectorRef getControllerNodeConnector(NodeConnectorRef nodeConnectorRef) {
    NodeConnectorRef controllerSwitchNodeConnector = null;
    HashMap<String, NodeConnectorRef> controllerSwitchConnectors = inventoryService.getControllerSwitchConnectors();
    InstanceIdentifier<Node> nodePath = InstanceIdentifierUtils.getNodePath(nodeConnectorRef.getValue());
    if (nodePath != null) {
      NodeKey nodeKey = InstanceIdentifierUtils.getNodeKey(nodePath);
      if (nodeKey != null) {
        controllerSwitchNodeConnector = controllerSwitchConnectors.get(nodeKey.getId().getValue());
      }
    }
    return controllerSwitchNodeConnector;
  }
}
