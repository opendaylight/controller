/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import org.eclipse.xtext.xbase.lib.Exceptions;
import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.packet.IPluginOutDataPacketService;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;

@SuppressWarnings("all")
public class DataPacketAdapter implements PacketProcessingListener {
  private IPluginOutDataPacketService _dataPacketPublisher;
  
  public IPluginOutDataPacketService getDataPacketPublisher() {
    return this._dataPacketPublisher;
  }
  
  public void setDataPacketPublisher(final IPluginOutDataPacketService dataPacketPublisher) {
    this._dataPacketPublisher = dataPacketPublisher;
  }
  
  public void onPacketReceived(final PacketReceived packet) {
    final RawPacket inPacket = DataPacketAdapter.toRawPacket(packet);
    IPluginOutDataPacketService _dataPacketPublisher = this.getDataPacketPublisher();
    _dataPacketPublisher.receiveDataPacket(inPacket);
  }
  
  public static RawPacket toRawPacket(final PacketReceived received) {
    try {
      byte[] _payload = received.getPayload();
      RawPacket _rawPacket = new RawPacket(_payload);
      final RawPacket ret = _rawPacket;
      NodeConnectorRef _ingress = received.getIngress();
      NodeConnector _aDNodeConnector = NodeMapping.toADNodeConnector(_ingress);
      ret.setIncomingNodeConnector(_aDNodeConnector);
      return ret;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
