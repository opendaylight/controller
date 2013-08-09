/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.reactiveforwarding.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;


import org.apache.felix.dm.Component;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.packet.ARP;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.routing.IRouting;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plain simple reactive forwarding service which received PACKET_IN and
 * uses Dijkstra's module (IRouting interface) to determine the shortest
 * path between source and destination and provision the switches
 *
 */
public class ReactiveForwardingService implements IListenDataPacket {

    /*
     * Logger instance
     */
    private static Logger lbsLogger = LoggerFactory.getLogger(ReactiveForwardingService.class);


    /*
     * REQUIRED for decoding the packet
     */
    private IDataPacketService dataPacketService = null;

    /*
     * REQUIRED for host identification from the packet
     */
    private IfIptoHost hostTracker;

    /*
     * REQUIRED for the flow-programming
     */
    private IForwardingRulesManager ruleManager;

    /*
     * REQUIRED for getting the path between source and destination determined from packet
     */
    private IRouting routing;

    /*
     * Load balancer application installs all flows with priority 2.
     */
    private static short LB_IPSWITCH_PRIORITY = 2;

    /*
     * Name of the container where this application will register.
     */
    private String containerName = null;

    /*
     * Set/unset methods for the service instance that load balancer
     * service requires
     */
    public String getContainerName() {
        if (containerName == null)
            return GlobalConstants.DEFAULT.toString();
        return containerName;
    }

    void setDataPacketService(IDataPacketService s) {
        this.dataPacketService = s;
    }

    void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }

    public void setRouting(IRouting routing) {
        this.routing = routing;
    }

    public void unsetRouting(IRouting routing) {
        if (this.routing == routing) {
            this.routing = null;
        }
    }

    public void setHostTracker(IfIptoHost hostTracker) {
        lbsLogger.debug("Setting HostTracker");
        this.hostTracker = hostTracker;
    }

    public void unsetHostTracker(IfIptoHost hostTracker) {
        if (this.hostTracker == hostTracker) {
            this.hostTracker = null;
        }
    }

    public void setForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        lbsLogger.debug("Setting ForwardingRulesManager");
        this.ruleManager = forwardingRulesManager;
    }

    public void unsetForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        if (this.ruleManager == forwardingRulesManager) {
            this.ruleManager = null;
        }
    }

    /**
     * This method receives first packet of flows for which there is no
     * matching flow rule installed on the switch. IP addresses used for VIPs
     * are not supposed to be used by any real/virtual host in the network.
     * Hence, any forwarding/routing service will not install any flows rules matching
     * these VIPs. This ensures that all the flows destined for VIPs will not find a match
     * in the switch and will be forwarded to the load balancing service.
     * Service will decide where to route this traffic based on the load balancing
     * policy of the VIP's attached pool and will install appropriate flow rules
     * in a reactive manner.
     */
    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt){

        if (inPkt == null) {
            return PacketResult.IGNORED;
        }

        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        Node clientNode = inPkt.getIncomingNodeConnector().getNode();

        if (formattedPak instanceof Ethernet) {


            Object ipPkt = formattedPak.getPayload();

            if (ipPkt instanceof ARP){
            }
            else{

                lbsLogger.debug("RFWD >> got non-ARP ether packet");

                Ethernet ether = (Ethernet) formattedPak;
                byte[] destMacAddr = ether.getDestinationMACAddress();
                byte[] srcMacAddr = ether.getSourceMACAddress();

                //Byte[] dstMacAddr = ArrayUtils.toObject(destMacAddr);
                //Byte[] srMacAddr = ArrayUtils.toObject(srcMacAddr);

                lbsLogger.debug("RFWD >> Dst Mac Address {}", destMacAddr.toString());
                lbsLogger.debug("RFWD >> Src Mac Address {}", srcMacAddr.toString());

                //lbsLogger.debug("RFWD >> ip-pkt has dst IP as {}", destIPAddr.getHostAddress());
                //lbsLogger.debug("RFWD >> ip-pkt has src IP as {}", srcIPAddr.getHostAddress());

                installDummyFlowForCBench(clientNode, null, null, srcMacAddr, destMacAddr);

            }



            lbsLogger.debug("flow sent to cbench !!");



            if (ipPkt instanceof IPv4) {



                IPv4 ipv4Pkt = (IPv4)ipPkt;

                lbsLogger.debug("RFWD >> got IP Packet");

                //InetAddress destIPAddr = InetAddress.getByAddress(NetUtils.intToByteArray4(ipv4Pkt.getDestinationAddress()));
                //InetAddress srcIPAddr = InetAddress.getByAddress(NetUtils.intToByteArray4(ipv4Pkt.getSourceAddress()));






                lbsLogger.debug("RFWD >> got IPv4 packet");

            }

        }
        return PacketResult.IGNORED;
    }


    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init(Component c) {
        Dictionary<?, ?> props = c.getServiceProperties();
        if (props != null) {
            this.containerName = (String) props.get("containerName");

            lbsLogger.info("Running container name:" + this.containerName);
        }else {

            // In the Global instance case the containerName is empty
            this.containerName = "";
        }
        //lbsLogger.info(configManager.toString());
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
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
    }


    private boolean installDummyFlowForCBench(Node originatorSwitch, InetAddress srcIp, InetAddress dstIp, byte[] srcMac, byte[] dstMac){




        Match match = new Match();
        List<Action> actions = new ArrayList<Action>();

        //match.setField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue());
        //match.setField(MatchType.NW_SRC, srcIp);
        //match.setField(MatchType.NW_DST, dstIp);

        match.setField(MatchType.DL_SRC, srcMac);
        //match.setField(MatchType.DL_DST, dstMac);

        actions.add(new Drop());

        Flow flow = new Flow(match, actions);
        flow.setIdleTimeout((short) 5);
        flow.setHardTimeout((short) 0);
        flow.setPriority(LB_IPSWITCH_PRIORITY++);

        String policyName = "cbenchpolicy";
        String flowName ="cbenchflow";

        FlowEntry fEntry = new FlowEntry(policyName, flowName, flow, originatorSwitch);

        //lbsLogger.debug("Install flow entry {} on node {}",fEntry.toString(),originatorSwitch.toString());




        //if(!this.ruleManager.checkFlowEntryConflict(fEntry)){
            if(this.ruleManager.installFlowEntry(fEntry).isSuccess()){
                return true;
            }else{
                lbsLogger.error("Error in installing flow entry to node : {}",originatorSwitch);
            }

        //}else{
        //    lbsLogger.error("Conflicting flow entry exists : {}",fEntry.toString());
        //}

        return false;


    }


}
