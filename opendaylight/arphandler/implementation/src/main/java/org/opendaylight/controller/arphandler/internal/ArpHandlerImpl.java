/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/*
 *
 */
package org.opendaylight.controller.arphandler.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.arphandler.IArpHandler;
import org.opendaylight.controller.hosttracker.IfHostListener;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.packet.ARP;
import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Subnet;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArpHandlerImpl implements IArpHandler, IListenDataPacket {
    private static final Logger logger = LoggerFactory
            .getLogger(ArpHandlerImpl.class);
    private IfIptoHost hostTracker = null;
    private ISwitchManager switchManager = null;
    private ITopologyManager topologyManager;
    private IDataPacketService dataPacketService = null;
    private Set<IfHostListener> hostListener = Collections
            .synchronizedSet(new HashSet<IfHostListener>());
    private ConcurrentHashMap<InetAddress, Set<HostNodeConnector>> arpRequestors;
    private ConcurrentHashMap<InetAddress, Short> countDownTimers;
    private Timer periodicTimer;

    void setHostListener(IfHostListener s) {
        if (this.hostListener != null) {
            this.hostListener.add(s);
        }
    }

    void unsetHostListener(IfHostListener s) {
        if (this.hostListener != null) {
            this.hostListener.remove(s);
        }
    }

    void setDataPacketService(IDataPacketService s) {
        this.dataPacketService = s;
    }

    void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }

    public IfIptoHost getHostTracker() {
        return hostTracker;
    }

    public void setHostTracker(IfIptoHost hostTracker) {
        logger.debug("Setting HostTracker");
        this.hostTracker = hostTracker;
    }

    public void unsetHostTracker(IfIptoHost s) {
        logger.debug("UNSetting HostTracker");
        if (this.hostTracker == s) {
            this.hostTracker = null;
        }
    }

    public void setTopologyManager(ITopologyManager tm) {
        this.topologyManager = tm;
    }

    public void unsetTopologyManager(ITopologyManager tm) {
        if (this.topologyManager == tm) {
            this.topologyManager = null;
        }
    }

    protected void sendARPReply(NodeConnector p, byte[] sMAC, InetAddress sIP,
            byte[] tMAC, InetAddress tIP) {
        byte[] senderIP = sIP.getAddress();
        byte[] targetIP = tIP.getAddress();
        ARP arp = new ARP();
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET)
                .setProtocolType(EtherTypes.IPv4.shortValue())
                .setHardwareAddressLength((byte) 6)
                .setProtocolAddressLength((byte) 4).setOpCode(ARP.REPLY)
                .setSenderHardwareAddress(sMAC)
                .setSenderProtocolAddress(senderIP)
                .setTargetHardwareAddress(tMAC)
                .setTargetProtocolAddress(targetIP);

        Ethernet ethernet = new Ethernet();
        ethernet.setSourceMACAddress(sMAC).setDestinationMACAddress(tMAC)
                .setEtherType(EtherTypes.ARP.shortValue()).setPayload(arp);

        RawPacket destPkt = this.dataPacketService.encodeDataPacket(ethernet);
        destPkt.setOutgoingNodeConnector(p);

        this.dataPacketService.transmitDataPacket(destPkt);
    }

    private boolean isBroadcastMAC(byte[] mac) {
        if (BitBufferHelper.toNumber(mac) == 0xffffffffffffL) { // TODO:
                                                                // implement
                                                                // this in our
                                                                // Ethernet
            return true;
        }
        return false;
    }

    private boolean isUnicastMAC(byte[] mac) {
        if ((BitBufferHelper.toNumber(mac) & 0x010000000000L) == 0) {
            return true;
        }
        return false;
    }

    protected void handleARPPacket(Ethernet eHeader, ARP pkt, NodeConnector p) {
        if (pkt.getOpCode() == 0x1) {
            logger.debug("Received ARP REQUEST Packet from NodeConnector: {}",
                    p);
        } else {
            logger.debug("Received ARP REPLY Packet from NodeConnector: {}", p);
        }
        InetAddress targetIP = null;
        try {
            targetIP = InetAddress.getByAddress(pkt.getTargetProtocolAddress());
        } catch (UnknownHostException e1) {
            return;
        }
        InetAddress sourceIP = null;
        try {
            sourceIP = InetAddress.getByAddress(pkt.getSenderProtocolAddress());
        } catch (UnknownHostException e1) {
            return;
        }
        byte[] targetMAC = eHeader.getDestinationMACAddress();
        byte[] sourceMAC = eHeader.getSourceMACAddress();

        /*
         * Sanity Check; drop ARP packets originated by the controller itself.
         * This is to avoid continuous flooding
         */
        if (Arrays.equals(sourceMAC, getControllerMAC())) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Receive the self originated packet (srcMAC {}) --> DROP",
                        HexEncode.bytesToHexString(sourceMAC));
            }
            return;
        }

        Subnet subnet = null;
        if (switchManager != null) {
            subnet = switchManager.getSubnetByNetworkAddress(sourceIP);
        }
        if (subnet == null) {
            logger.debug("can't find subnet matching {}, drop packet", sourceIP);
            return;
        }
        logger.debug("Found {} matching {}", subnet, sourceIP);
        /*
         * Make sure that the host is a legitimate member of this subnet
         */
        if (!subnet.hasNodeConnector(p)) {
            logger.debug("{} showing up on {} does not belong to {}",
                    new Object[] { sourceIP, p, subnet });
            return;
        }

        HostNodeConnector requestor = null;
        if (isUnicastMAC(sourceMAC)) {
            // TODO For not this is only OPENFLOW but we need to fix this
            if (p.getType().equals(NodeConnector.NodeConnectorIDType.OPENFLOW)) {
                try {
                    requestor = new HostNodeConnector(sourceMAC, sourceIP, p,
                            subnet.getVlan());
                } catch (ConstructionException e) {
                    return;
                }
                /*
                 * Learn host from the received ARP REQ/REPLY, inform Host
                 * Tracker
                 */
                logger.debug("Inform Host tracker of new host {}",
                        requestor.getNetworkAddress());
                synchronized (this.hostListener) {
                    for (IfHostListener listener : this.hostListener) {
                        listener.hostListener(requestor);
                    }
                }
            }
        }
        /*
         * Gratuitous ARP. If there are hosts (in arpRequestors) waiting for the
         * ARP reply for this sourceIP, it's time to generate the reply and it
         * to these hosts
         */
        if (sourceIP.equals(targetIP)) {
            generateAndSendReply(sourceIP, sourceMAC);
            return;
        }

        /*
         * ARP Reply. If there are hosts (in arpRequesttors) waiting for the ARP
         * reply for this sourceIP, it's time to generate the reply and it to
         * these hosts
         */
        if (pkt.getOpCode() != ARP.REQUEST) {
            generateAndSendReply(sourceIP, sourceMAC);
            return;
        }

        /*
         * ARP Request Handling: If targetIP is the IP of the subnet, reply with
         * ARP REPLY If targetIP is a known host, PROXY ARP (by sending ARP
         * REPLY) on behalf of known target hosts. For unknown target hosts,
         * generate and send an ARP request to ALL switches/ports using the IP
         * address defined in the subnet as source address
         */
        /*
         * Send ARP reply if target IP is gateway IP
         */
        if ((targetIP.equals(subnet.getNetworkAddress()))
                && (isBroadcastMAC(targetMAC) || Arrays.equals(targetMAC,
                        getControllerMAC()))) {
            sendARPReply(p, getControllerMAC(), targetIP,
                    pkt.getSenderHardwareAddress(), sourceIP);
            return;
        }

        /*
         * unknown host, initiate ARP request
         */
        HostNodeConnector host = hostTracker.hostQuery(targetIP);
        if (host == null) {
            // add the requestor to the list so that we can replay the reply
            // when the host responds
            if (requestor != null) {
                Set<HostNodeConnector> requestorSet = arpRequestors
                        .get(targetIP);
                if ((requestorSet == null) || requestorSet.isEmpty()) {
                    requestorSet = new HashSet<HostNodeConnector>();
                    countDownTimers.put(targetIP, (short) 2); // set max timeout
                                                              // to 2sec
                }
                requestorSet.add(requestor);
                arpRequestors.put(targetIP, requestorSet);
            }
            sendBcastARPRequest(targetIP, subnet);
            return;
        }
        /*
         * Known target host, send ARP REPLY make sure that targetMAC matches
         * the host's MAC if it is not broadcastMAC
         */
        if (isBroadcastMAC(targetMAC)
                || Arrays.equals(host.getDataLayerAddressBytes(), targetMAC)) {
            sendARPReply(p, host.getDataLayerAddressBytes(),
                    host.getNetworkAddress(), pkt.getSenderHardwareAddress(),
                    sourceIP);
            return;
        } else {
            /*
             * target target MAC has been changed. For now, discard it. TODO: We
             * may need to send unicast ARP REQUEST on behalf of the target back
             * to the sender to trigger the sender to update its table
             */
            return;
        }
    }

    /*
     * Send a broadcast ARP Request to the switch/ ports using the
     * networkAddress of the subnet as sender IP the controller's MAC as sender
     * MAC the targetIP as the target Network Address
     */
    protected void sendBcastARPRequest(InetAddress targetIP, Subnet subnet) {
        Set<NodeConnector> nodeConnectors;
        if (subnet.isFlatLayer2()) {
            nodeConnectors = new HashSet<NodeConnector>();
            for (Node n : this.switchManager.getNodes()) {
                nodeConnectors
                        .addAll(this.switchManager.getUpNodeConnectors(n));
            }
        } else {
            nodeConnectors = subnet.getNodeConnectors();
        }
        for (NodeConnector p : nodeConnectors) {
            if (topologyManager.isInternal(p)) {
                continue;
            }
            ARP arp = new ARP();
            byte[] senderIP = subnet.getNetworkAddress().getAddress();
            byte[] targetIPB = targetIP.getAddress();
            arp.setHardwareType(ARP.HW_TYPE_ETHERNET)
                    .setProtocolType(EtherTypes.IPv4.shortValue())
                    .setHardwareAddressLength((byte) 6)
                    .setProtocolAddressLength((byte) 4)
                    .setOpCode(ARP.REQUEST)
                    .setSenderHardwareAddress(getControllerMAC())
                    .setSenderProtocolAddress(senderIP)
                    .setTargetHardwareAddress(
                            new byte[] { (byte) 0, (byte) 0, (byte) 0,
                                    (byte) 0, (byte) 0, (byte) 0 })
                    .setTargetProtocolAddress(targetIPB);

            Ethernet ethernet = new Ethernet();
            ethernet.setSourceMACAddress(getControllerMAC())
                    .setDestinationMACAddress(
                            new byte[] { (byte) -1, (byte) -1, (byte) -1,
                                    (byte) -1, (byte) -1, (byte) -1 })
                    .setEtherType(EtherTypes.ARP.shortValue()).setPayload(arp);

            // TODO For now send port-by-port, see how to optimize to
            // send to multiple ports at once
            RawPacket destPkt = this.dataPacketService
                    .encodeDataPacket(ethernet);
            destPkt.setOutgoingNodeConnector(p);

            this.dataPacketService.transmitDataPacket(destPkt);
        }
    }

    /*
     * Send a unicast ARP Request to the known host on a specific switch/port as
     * defined in the host. The sender IP is the networkAddress of the subnet
     * The sender MAC is the controller's MAC
     */
    protected void sendUcastARPRequest(HostNodeConnector host, Subnet subnet) {
        // Long swID = host.getnodeconnectornodeId();
        // Short portID = host.getnodeconnectorportId();
        // Node n = NodeCreator.createOFNode(swID);
        Node n = host.getnodeconnectorNode();
        if (n == null) {
            logger.error("cannot send UcastARP because cannot extract node "
                    + "from HostNodeConnector: {}", host);
            return;
        }
        NodeConnector outPort = host.getnodeConnector();
        if (outPort == null) {
            logger.error("cannot send UcastARP because cannot extract "
                    + "outPort from HostNodeConnector: {}", host);
            return;
        }

        byte[] senderIP = subnet.getNetworkAddress().getAddress();
        byte[] targetIP = host.getNetworkAddress().getAddress();
        byte[] targetMAC = host.getDataLayerAddressBytes();
        ARP arp = new ARP();
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET)
                .setProtocolType(EtherTypes.IPv4.shortValue())
                .setHardwareAddressLength((byte) 6)
                .setProtocolAddressLength((byte) 4).setOpCode(ARP.REQUEST)
                .setSenderHardwareAddress(getControllerMAC())
                .setSenderProtocolAddress(senderIP)
                .setTargetHardwareAddress(targetMAC)
                .setTargetProtocolAddress(targetIP);

        Ethernet ethernet = new Ethernet();
        ethernet.setSourceMACAddress(getControllerMAC())
                .setDestinationMACAddress(targetMAC)
                .setEtherType(EtherTypes.ARP.shortValue()).setPayload(arp);

        RawPacket destPkt = this.dataPacketService.encodeDataPacket(ethernet);
        destPkt.setOutgoingNodeConnector(outPort);

        this.dataPacketService.transmitDataPacket(destPkt);
    }

    public void find(InetAddress networkAddress) {
        logger.debug("Received find IP {}", networkAddress);

        Subnet subnet = null;
        if (switchManager != null) {
            subnet = switchManager.getSubnetByNetworkAddress(networkAddress);
        }
        if (subnet == null) {
            logger.debug("can't find subnet matching IP {}", networkAddress);
            return;
        }
        logger.debug("found subnet {}", subnet);

        // send a broadcast ARP Request to this interface
        sendBcastARPRequest(networkAddress, subnet);
    }

    /*
     * Probe the host by sending a unicast ARP Request to the host
     */
    public void probe(HostNodeConnector host) {
        logger.debug("Received probe host {}", host);

        Subnet subnet = null;
        if (switchManager != null) {
            subnet = switchManager.getSubnetByNetworkAddress(host
                    .getNetworkAddress());
        }
        if (subnet == null) {
            logger.debug("can't find subnet matching {}",
                    host.getNetworkAddress());
            return;
        }
        sendUcastARPRequest(host, subnet);
    }

    /*
     * An IP packet is punted to the controller, this means that the destination
     * host is not known to the controller. Need to discover it by sending a
     * Broadcast ARP Request
     */
    protected void handlePuntedIPPacket(IPv4 pkt, NodeConnector p) {
        InetAddress dIP = null;
        try {
            dIP = InetAddress.getByAddress(NetUtils.intToByteArray4(pkt
                    .getDestinationAddress()));
        } catch (UnknownHostException e1) {
            return;
        }

        Subnet subnet = null;
        if (switchManager != null) {
            subnet = switchManager.getSubnetByNetworkAddress(dIP);
        }
        if (subnet == null) {
            logger.debug("can't find subnet matching {}, drop packet", dIP);
            return;
        }
        logger.debug("Found {} matching {}", subnet, dIP);
        /*
         * unknown destination host, initiate ARP request
         */
        sendBcastARPRequest(dIP, subnet);
        return;
    }

    public byte[] getControllerMAC() {
        if (switchManager == null) {
            return null;
        }
        return switchManager.getControllerMAC();
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     * 
     */
    void init() {
        arpRequestors = new ConcurrentHashMap<InetAddress, Set<HostNodeConnector>>();
        countDownTimers = new ConcurrentHashMap<InetAddress, Short>();
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     * 
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     * 
     */
    void start() {
        startPeriodicTimer();
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     * 
     */
    void stop() {
        cancelPeriodicTimer();
    }

    void setSwitchManager(ISwitchManager s) {
        logger.debug("SwitchManager set");
        this.switchManager = s;
    }

    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            logger.debug("SwitchManager removed!");
            this.switchManager = null;
        }
    }

    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        if (inPkt == null) {
            return PacketResult.IGNORED;
        }
        logger.trace("Received a frame of size: {}",
                inPkt.getPacketData().length);
        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        if (formattedPak instanceof Ethernet) {
            Object nextPak = formattedPak.getPayload();
            if (nextPak instanceof IPv4) {
                handlePuntedIPPacket((IPv4) nextPak,
                        inPkt.getIncomingNodeConnector());
                logger.trace("Handled IP packet");
            }
            if (nextPak instanceof ARP) {
                handleARPPacket((Ethernet) formattedPak, (ARP) nextPak,
                        inPkt.getIncomingNodeConnector());
                logger.trace("Handled ARP packet");
            }
        }
        return PacketResult.IGNORED;
    }

    private void startPeriodicTimer() {
        this.periodicTimer = new Timer("ArpHandler Periodic Timer");
        this.periodicTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Set<InetAddress> targetIPs = countDownTimers.keySet();
                Set<InetAddress> expiredTargets = new HashSet<InetAddress>();
                for (InetAddress t : targetIPs) {
                    short tick = countDownTimers.get(t);
                    tick--;
                    if (tick <= 0) {
                        expiredTargets.add(t);
                    } else {
                        countDownTimers.replace(t, tick);
                    }
                }
                for (InetAddress t : expiredTargets) {
                    countDownTimers.remove(t);
                    // remove the requestor(s) who have been waited for the ARP
                    // reply from this target for more than 1sec
                    arpRequestors.remove(t);
                    logger.debug("{} didn't respond to ARP request", t);
                }
            }
        }, 0, 1000);
    }

    private void cancelPeriodicTimer() {
        if (this.periodicTimer != null) {
            this.periodicTimer.cancel();
        }
    }

    private void generateAndSendReply(InetAddress sourceIP, byte[] sourceMAC) {
        Set<HostNodeConnector> hosts = arpRequestors.remove(sourceIP);
        if ((hosts == null) || hosts.isEmpty()) {
            return;
        }
        countDownTimers.remove(sourceIP);
        for (HostNodeConnector host : hosts) {
            logger.debug(
                    "Sending ARP Reply with src {}/{}, target {}/{}",
                    new Object[] { sourceMAC, sourceIP,
                            host.getDataLayerAddressBytes(),
                            host.getNetworkAddress() });
            sendARPReply(host.getnodeConnector(), sourceMAC, sourceIP,
                    host.getDataLayerAddressBytes(), host.getNetworkAddress());
        }
    }
}
