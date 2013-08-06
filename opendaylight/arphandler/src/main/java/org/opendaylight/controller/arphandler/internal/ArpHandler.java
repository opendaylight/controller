
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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.opendaylight.controller.arphandler.ARPEvent;
import org.opendaylight.controller.arphandler.ARPReply;
import org.opendaylight.controller.arphandler.ARPRequest;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.connectionmanager.IConnectionManager;
import org.opendaylight.controller.hosttracker.IfHostListener;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.hosttracker.hostAware.IHostFinder;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.packet.ARP;
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

public class ArpHandler implements IHostFinder, IListenDataPacket, ICacheUpdateAware<ARPEvent, Boolean> {
    private static final Logger log = LoggerFactory.getLogger(ArpHandler.class);
    static final String ARP_EVENT_CACHE_NAME = "arphandler.arpRequestReplyEvent";
    private IfIptoHost hostTracker;
    private ISwitchManager switchManager;
    private ITopologyManager topologyManager;
    private IDataPacketService dataPacketService;
    private IClusterContainerServices clusterContainerService;
    private IConnectionManager connectionManager;
    private Set<IfHostListener> hostListeners = new CopyOnWriteArraySet<IfHostListener>();
    private ConcurrentMap<InetAddress, Set<HostNodeConnector>> arpRequestors;
    private ConcurrentMap<InetAddress, Short> countDownTimers;
    private Timer periodicTimer;
    /*
     * A cluster allocated cache. Used for synchronizing ARP request/reply
     * events across all cluster controllers. To raise an event, we put() a specific
     * event object (as key) and all nodes handle it in the entryUpdated callback.
     *
     * In case of ARPReply, we put true value to send replies to any requestors
     * by calling generateAndSendReply
     */
    private ConcurrentMap<ARPEvent, Boolean> arpRequestReplyEvent;

    void setConnectionManager(IConnectionManager cm){
        this.connectionManager = cm;
    }

    void unsetConnectionManager(IConnectionManager cm){
        if (this.connectionManager == cm){
            connectionManager = null;
        }
    }

    void setClusterContainerService(IClusterContainerServices s){
        this.clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (this.clusterContainerService == s) {
            this.clusterContainerService = null;
        }
    }

    void setHostListener(IfHostListener s) {
        if (this.hostListeners != null) {
            this.hostListeners.add(s);
        }
    }

    void unsetHostListener(IfHostListener s) {
        if (this.hostListeners != null) {
            this.hostListeners.remove(s);
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

    public void setHostTracker(IfIptoHost hostTracker) {
        log.debug("Setting HostTracker");
        this.hostTracker = hostTracker;
    }

    public void unsetHostTracker(IfIptoHost s) {
        log.debug("UNSetting HostTracker");
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
            .setProtocolAddressLength((byte) 4)
            .setOpCode(ARP.REPLY)
            .setSenderHardwareAddress(sMAC)
            .setSenderProtocolAddress(senderIP)
            .setTargetHardwareAddress(tMAC)
            .setTargetProtocolAddress(targetIP);

        Ethernet ethernet = new Ethernet();
        ethernet.setSourceMACAddress(sMAC)
            .setDestinationMACAddress(tMAC)
            .setEtherType(EtherTypes.ARP.shortValue())
            .setPayload(arp);

        RawPacket destPkt = this.dataPacketService.encodeDataPacket(ethernet);
        destPkt.setOutgoingNodeConnector(p);

        this.dataPacketService.transmitDataPacket(destPkt);
    }

    protected void handleARPPacket(Ethernet eHeader, ARP pkt, NodeConnector p) {

        byte[] sourceMAC = eHeader.getSourceMACAddress();
        byte[] targetMAC = eHeader.getDestinationMACAddress();
        /*
         * Sanity Check; drop ARP packets originated by the controller itself.
         * This is to avoid continuous flooding
         */
        if (Arrays.equals(sourceMAC, getControllerMAC())) {
            if (log.isDebugEnabled()) {
                log.debug("Receive a self originated ARP pkt (srcMAC {}) --> DROP",
                        HexEncode.bytesToHexString(sourceMAC));
            }
            return;
        }

        InetAddress targetIP, sourceIP;
        try {
            targetIP = InetAddress.getByAddress(pkt.getTargetProtocolAddress());
            sourceIP = InetAddress.getByAddress(pkt.getSenderProtocolAddress());
        } catch (UnknownHostException e1) {
            log.debug("Invalid host in ARP packet: {}", e1.getMessage());
            return;
        }

        Subnet subnet = null;
        if (switchManager != null) {
            subnet = switchManager.getSubnetByNetworkAddress(sourceIP);
        }
        if (subnet == null) {
            log.debug("ARPHandler: can't find subnet matching {}, drop packet", sourceIP);
            return;
        }

        // Make sure that the host is a legitimate member of this subnet
        if (!subnet.hasNodeConnector(p)) {
            log.debug("{} showing up on {} does not belong to {}",
                    new Object[] { sourceIP, p, subnet });
            return;
        }

        HostNodeConnector requestor = null;
        if (NetUtils.isUnicastMACAddr(sourceMAC) && p.getNode() != null) {
            try {
                requestor = new HostNodeConnector(sourceMAC, sourceIP, p, subnet.getVlan());
            } catch (ConstructionException e) {
                log.debug("Received ARP packet with invalid MAC: {}", sourceMAC);
                return;
            }
            /*
             * Learn host from the received ARP REQ/REPLY, inform Host Tracker
             */
            log.trace("Inform Host tracker of new host {}", requestor.getNetworkAddress());
            for (IfHostListener listener : this.hostListeners) {
                listener.hostListener(requestor);
            }
        }

        /*
         * OpCode != request -> ARP Reply. If there are hosts (in
         * arpRequestors) waiting for the ARP reply for this sourceIP, it's
         * time to generate the reply and send it to these hosts.
         *
         * If sourceIP==targetIP, it is a Gratuitous ARP. If there are hosts (in
         * arpRequestors) waiting for the ARP reply for this sourceIP, it's time
         * to generate the reply and send it to these hosts
         */

        if (pkt.getOpCode() != ARP.REQUEST || sourceIP.equals(targetIP)) {
            // Raise a reply event so that any waiting requestors will be sent a reply
            // the true value indicates we should generate replies to requestors across the cluster
            log.trace("Received ARP reply packet from {}, reply to all requestors.", sourceIP);
            arpRequestReplyEvent.put(new ARPReply(sourceIP, sourceMAC), true);
            return;
        }

        /*
         * ARP Request Handling:
         * If targetIP is the IP of the subnet, reply with ARP REPLY
         * If targetIP is a known host, PROXY ARP (by sending ARP REPLY) on behalf of known target hosts.
         * For unknown target hosts, generate and send an ARP request to ALL switches/ports using
         * the IP address defined in the subnet as source address
         */
        /*
         * If target IP is gateway IP, Send ARP reply
         */
        if ((targetIP.equals(subnet.getNetworkAddress()))
                && (NetUtils.isBroadcastMACAddr(targetMAC) || Arrays.equals(targetMAC, getControllerMAC()))) {
            if (connectionManager.isLocal(p.getNode())){
                if (log.isTraceEnabled()){
                    log.trace("Received local ARP req. for default gateway. Replying with controller MAC: {}", getControllerMAC());
                }
                sendARPReply(p, getControllerMAC(), targetIP, pkt.getSenderHardwareAddress(), sourceIP);
            } else {
                log.trace("Received non-local ARP req. for default gateway. Raising reply event");
                arpRequestReplyEvent.put(
                        new ARPReply(p, targetIP, getControllerMAC(), sourceIP, pkt.getSenderHardwareAddress()), false);
            }
            return;
        }


        HostNodeConnector host = hostTracker.hostQuery(targetIP);
        // unknown host, initiate ARP request
        if (host == null) {
            // add the requestor to the list so that we can replay the reply
            // when the host responds
            if (requestor != null) {
                Set<HostNodeConnector> requestorSet = arpRequestors.get(targetIP);
                if (requestorSet == null) {
                    requestorSet = Collections.newSetFromMap(new ConcurrentHashMap<HostNodeConnector, Boolean>());
                    arpRequestors.put(targetIP, requestorSet);
                }
                requestorSet.add(requestor);
                countDownTimers.put(targetIP, (short) 2); // reset timeout to 2sec
            }
            //Raise a bcast request event, all controllers need to send one
            log.trace("Sending a bcast ARP request for {}", targetIP);
            arpRequestReplyEvent.put(new ARPRequest(targetIP, subnet), false);

        } else {
            /*
             * Target host known (across the cluster), send ARP REPLY make sure that targetMAC
             * matches the host's MAC if it is not broadcastMAC
             */
            if (NetUtils.isBroadcastMACAddr(targetMAC) || Arrays.equals(host.getDataLayerAddressBytes(), targetMAC)) {
                log.trace("Received ARP req. for known host {}, sending reply...", targetIP);
                if (connectionManager.isLocal(p.getNode())) {
                    sendARPReply(p,
                            host.getDataLayerAddressBytes(),
                            host.getNetworkAddress(),
                            pkt.getSenderHardwareAddress(),
                            sourceIP);
                } else {
                    arpRequestReplyEvent.put(new ARPReply(
                            p,
                            host.getNetworkAddress(),
                            host.getDataLayerAddressBytes(),
                            sourceIP,
                            pkt.getSenderHardwareAddress()), false);
                }
            } else {
                /*
                 * Target MAC has been changed. For now, discard it.
                 * TODO: We may need to send unicast ARP REQUEST on behalf of
                 * the target back to the sender to trigger the sender to update
                 * its table
                 */
            }
        }
    }

    /*
     *  Send a broadcast ARP Request to the switch/ ports  using
     *  the networkAddress of the subnet as sender IP
     *  the controller's MAC as sender MAC
     *  the targetIP as the target Network Address
     */
    protected void sendBcastARPRequest(InetAddress targetIP, Subnet subnet) {
        Set<NodeConnector> nodeConnectors;
        if (subnet.isFlatLayer2()) {
            nodeConnectors = new HashSet<NodeConnector>();
            for (Node n : this.switchManager.getNodes()) {
                nodeConnectors.addAll(this.switchManager.getUpNodeConnectors(n));
            }
        } else {
            nodeConnectors = subnet.getNodeConnectors();
        }

        for (NodeConnector p : nodeConnectors) {

            //fiter out any non-local or internal ports
            if (! connectionManager.isLocal(p.getNode()) || topologyManager.isInternal(p)) {
                continue;
            }
            ARP arp = new ARP();
            byte[] senderIP = subnet.getNetworkAddress().getAddress();
            byte[] targetIPByte = targetIP.getAddress();
            arp.setHardwareType(ARP.HW_TYPE_ETHERNET)
               .setProtocolType(EtherTypes.IPv4.shortValue())
               .setHardwareAddressLength((byte) 6)
               .setProtocolAddressLength((byte) 4)
               .setOpCode(ARP.REQUEST)
               .setSenderHardwareAddress(getControllerMAC())
               .setSenderProtocolAddress(senderIP)
               .setTargetHardwareAddress(
                       new byte[] { (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 })
               .setTargetProtocolAddress(targetIPByte);

            Ethernet ethernet = new Ethernet();
            ethernet.setSourceMACAddress(getControllerMAC())
                    .setDestinationMACAddress(new byte[] {(byte) -1,
                                                          (byte) -1,
                                                          (byte) -1,
                                                          (byte) -1,
                                                          (byte) -1,
                                                          (byte) -1 })
                    .setEtherType(EtherTypes.ARP.shortValue()).setPayload(arp);

            // TODO For now send port-by-port, see how to optimize to
            // send to multiple ports at once
            RawPacket destPkt = this.dataPacketService.encodeDataPacket(ethernet);
            destPkt.setOutgoingNodeConnector(p);

            this.dataPacketService.transmitDataPacket(destPkt);
        }
    }

    /*
     * Send a unicast ARP Request to the known host on a specific switch/port as
     * defined in the host.
     * The sender IP is the networkAddress of the subnet
     * The sender MAC is the controller's MAC
     */
    protected void sendUcastARPRequest(HostNodeConnector host, Subnet subnet) {

        NodeConnector outPort = host.getnodeConnector();
        if (outPort == null) {
            log.error("Failed sending UcastARP because cannot extract output port from Host: {}", host);
            return;
        }

        byte[] senderIP = subnet.getNetworkAddress().getAddress();
        byte[] targetIP = host.getNetworkAddress().getAddress();
        byte[] targetMAC = host.getDataLayerAddressBytes();
        ARP arp = new ARP();
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET)
            .setProtocolType(EtherTypes.IPv4.shortValue())
            .setHardwareAddressLength((byte) 6)
            .setProtocolAddressLength((byte) 4)
            .setOpCode(ARP.REQUEST)
            .setSenderHardwareAddress(getControllerMAC())
            .setSenderProtocolAddress(senderIP)
            .setTargetHardwareAddress(targetMAC)
            .setTargetProtocolAddress(targetIP);

        Ethernet ethernet = new Ethernet();
        ethernet.setSourceMACAddress(getControllerMAC())
                .setDestinationMACAddress(targetMAC)
                .setEtherType(EtherTypes.ARP.shortValue())
                .setPayload(arp);

        RawPacket destPkt = this.dataPacketService.encodeDataPacket(ethernet);
        destPkt.setOutgoingNodeConnector(outPort);

        this.dataPacketService.transmitDataPacket(destPkt);
    }

    public void find(InetAddress networkAddress) {
        log.trace("Received find IP {}", networkAddress);

        Subnet subnet = null;
        if (switchManager != null) {
            subnet = switchManager.getSubnetByNetworkAddress(networkAddress);
        }
        if (subnet == null) {
            log.debug("Can't find subnet matching IP {}", networkAddress);
            return;
        }

        // send a broadcast ARP Request to this IP
        arpRequestReplyEvent.put(new ARPRequest(networkAddress, subnet), false);
    }

    /*
     * Probe the host by sending a unicast ARP Request to the host
     */
    public void probe(HostNodeConnector host) {
        log.trace("Received probe host {}", host);

        Subnet subnet = null;
        if (switchManager != null) {
            subnet = switchManager.getSubnetByNetworkAddress(host
                    .getNetworkAddress());
        }
        if (subnet == null) {
            log.debug("can't find subnet matching {}", host.getNetworkAddress());
            return;
        }

        if (connectionManager.isLocal(host.getnodeconnectorNode())){
            log.trace("Send a ucast ARP req. to: {}", host);
            sendUcastARPRequest(host, subnet);
        } else {
            log.trace("Raise a ucast ARP req. event to: {}", host);
            arpRequestReplyEvent.put(new ARPRequest(host, subnet), false);
        }
    }

    /*
     * An IP packet is punted to the controller, this means that the
     * destination host is not known to the controller.
     * Need to discover it by sending a Broadcast ARP Request
     */
    protected void handlePuntedIPPacket(IPv4 pkt, NodeConnector p) {

        InetAddress dIP = NetUtils.getInetAddress(pkt.getDestinationAddress());
        if (dIP == null) {
           return;
        }

        Subnet subnet = null;
        if (switchManager != null) {
            subnet = switchManager.getSubnetByNetworkAddress(dIP);
        }
        if (subnet == null) {
            log.debug("Can't find subnet matching {}, drop packet", dIP);
            return;
        }
        log.trace("Punted IP pkt from {}, sending bcast ARP event...", dIP);
        /*
         * unknown destination host, initiate bcast ARP request
         */
        arpRequestReplyEvent.put(new ARPRequest(dIP, subnet), false);
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

        allocateCaches();
        retrieveCaches();
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void retrieveCaches() {
        ConcurrentMap<?,?> map;

        if (this.clusterContainerService == null){
            log.error("Cluster service unavailable, can't retieve ARPHandler caches!");
            return;
        }

        map = clusterContainerService.getCache(ARP_EVENT_CACHE_NAME);
        if (map != null){
            this.arpRequestReplyEvent = (ConcurrentMap<ARPEvent, Boolean>) map;
        } else {
            log.error("Cache allocation failed for {}", ARP_EVENT_CACHE_NAME);
        }
    }

    @SuppressWarnings("deprecation")
    private void allocateCaches() {
        if (clusterContainerService == null){
            nonClusterObjectCreate();
            log.error("Clustering service unavailable. Allocated non-cluster caches for ARPHandler.");
            return;
        }

        try{
            clusterContainerService.createCache(ARP_EVENT_CACHE_NAME,
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheConfigException e){
            log.error("ARPHandler cache configuration invalid!");
        } catch (CacheExistException e){
            log.debug("ARPHandler cache exists, skipped allocation.");
        }

    }

    private void nonClusterObjectCreate(){
        arpRequestReplyEvent = new ConcurrentHashMap<ARPEvent, Boolean>();
    }
    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        startPeriodicTimer();
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop(){
    }

    void stopping() {
        cancelPeriodicTimer();
    }

    void setSwitchManager(ISwitchManager s) {
        log.debug("SwitchManager service set.");
        this.switchManager = s;
    }

    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            log.debug("SwitchManager service UNset.");
            this.switchManager = null;
        }
    }

    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        if (inPkt == null) {
            return PacketResult.IGNORED;
        }
        log.trace("Received a frame of size: {}", inPkt.getPacketData().length);
        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        if (formattedPak instanceof Ethernet) {
            Object nextPak = formattedPak.getPayload();
            if (nextPak instanceof IPv4) {
                log.trace("Handle IP packet: {}", formattedPak);
                handlePuntedIPPacket((IPv4) nextPak, inPkt.getIncomingNodeConnector());
            } else if (nextPak instanceof ARP) {
                log.trace("Handle ARP packet: {}", formattedPak);
                handleARPPacket((Ethernet) formattedPak, (ARP) nextPak, inPkt
                        .getIncomingNodeConnector());
            }
        }
        return PacketResult.IGNORED;
    }

    private void startPeriodicTimer() {
        this.periodicTimer = new Timer("ArpHandler Periodic Timer");
        this.periodicTimer.scheduleAtFixedRate(new TimerTask() {
            @SuppressWarnings("deprecation")
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
                for (InetAddress tIP : expiredTargets) {
                    countDownTimers.remove(tIP);
                    // Remove the requestor(s) who have been waiting for the ARP
                    // reply from this target for more than 1sec
                    arpRequestors.remove(tIP);
                    log.debug("ARP reply was not received from {}", tIP);
                }

                // Clean up ARP event cache
                try {
                    if (clusterContainerService.amICoordinator() && ! arpRequestReplyEvent.isEmpty()){
                        arpRequestReplyEvent.clear();
                    }
                } catch (Exception e){
                    log.warn("ARPHandler: A cluster member failed to clear event cache.");
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
            log.trace("Sending ARP Reply with src {}/{}, target {}/{}",
                    new Object[] { sourceMAC, sourceIP, host.getDataLayerAddressBytes(), host.getNetworkAddress() });

            if (connectionManager.isLocal(host.getnodeconnectorNode())){
                sendARPReply(host.getnodeConnector(),
                        sourceMAC,
                        sourceIP,
                        host.getDataLayerAddressBytes(),
                        host.getNetworkAddress());
            } else {
                arpRequestReplyEvent.put(
                        new ARPReply(
                            host.getnodeConnector(),
                            sourceIP,
                            sourceMAC,
                            host.getNetworkAddress(),
                            host.getDataLayerAddressBytes()), false);
            }
        }
    }


    @Override
    public void entryUpdated(ARPEvent key, Boolean new_value, String cacheName, boolean originLocal) {
        if (key instanceof ARPRequest) {
            ARPRequest req = (ARPRequest) key;
            // If broadcast request
            if (req.getHost() == null) {
                sendBcastARPRequest(req.getTargetIP(), req.getSubnet());

            //If unicast and local, send reply
            } else if (connectionManager.isLocal(req.getHost().getnodeconnectorNode())) {
                sendUcastARPRequest(req.getHost(), req.getSubnet());
            }
        } else if (key instanceof ARPReply) {
            ARPReply rep = (ARPReply) key;
            // New reply received by controller, notify all awaiting requestors across the cluster
            if (new_value) {
                generateAndSendReply(rep.getTargetIP(), rep.getTargetMac());

            // Otherwise, a specific reply. If local, send out.
            } else if (connectionManager.isLocal(rep.getPort().getNode())) {
                sendARPReply(rep.getPort(),
                        rep.getSourceMac(),
                        rep.getSourceIP(),
                        rep.getTargetMac(),
                        rep.getTargetIP());
            }
        }
    }

    @Override
    public void entryCreated(ARPEvent key, String cacheName, boolean originLocal) {
        // nothing to do
    }
    @Override
    public void entryDeleted(ARPEvent key, String cacheName, boolean originLocal) {
        // nothing to do
    }
}
