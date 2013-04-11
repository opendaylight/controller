
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.protocol_plugin.openflow.IDataPacketListen;
import org.opendaylight.controller.protocol_plugin.openflow.IDataPacketMux;
import org.opendaylight.controller.protocol_plugin.openflow.IInventoryShimExternalListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.openflow.protocol.OFPhysicalPort;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.core.Config;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.discovery.IDiscoveryService;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.LLDP;
import org.opendaylight.controller.sal.packet.LLDPTLV;
import org.opendaylight.controller.sal.packet.LinkEncap;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;

/**
 * The class describes neighbor discovery service for an OpenFlow network.
 */
public class DiscoveryService implements IInventoryShimExternalListener,
        IDataPacketListen, IContainerListener, CommandProvider {
    private static Logger logger = LoggerFactory
            .getLogger(DiscoveryService.class);
    private IController controller = null;
    private IDiscoveryService discoveryService = null;
    private IPluginInInventoryService pluginInInventoryService = null;
    private IDataPacketMux iDataPacketMux = null;

    private List<NodeConnector> readyListHi = null; // newly added ports go into this list and will be served first
    private List<NodeConnector> readyListLo = null; // come here after served at least once
    private List<NodeConnector> waitingList = null; // staging area during quiet period
    private ConcurrentMap<NodeConnector, Integer> pendingMap = null;// wait for response back
    private ConcurrentMap<NodeConnector, Edge> edgeMap = null; // openflow edges keyed by head connector
    private ConcurrentMap<NodeConnector, Integer> agingMap = null; // aging entries keyed by edge port
    private ConcurrentMap<NodeConnector, Edge> prodMap = null; // production edges keyed by edge port

    private Timer discoveryTimer; // discovery timer
    private DiscoveryTimerTask discoveryTimerTask; // timer task
    private long discoveryTimerTick = 1L * 1000; // per tick in msec
    private int discoveryTimerTickCount = 0; // main tick counter
    private int discoveryBatchMaxPorts = 500; // max # of ports handled in one batch
    private int discoveryBatchRestartTicks = 30; // periodically restart batching process
    private int discoveryBatchPausePeriod = 2; // pause for few secs
    private int discoveryBatchPauseTicks = discoveryBatchRestartTicks - discoveryBatchPausePeriod; // pause after this point
    private int discoveryRetry = 1; // number of retry after initial timeout
    private int discoveryTimeoutTicks = 2; // timeout 2 sec
    private int discoveryAgeoutTicks = 120; // age out 2 min
    private int discoveryConsistencyCheckMultiple = 2; // multiple of discoveryBatchRestartTicks
    private int discoveryConsistencyCheckTickCount = discoveryBatchPauseTicks; // CC tick counter
    private int discoveryConsistencyCheckCallingTimes = 0; // # of times CC gets called
    private int discoveryConsistencyCheckCorrected = 0; // # of cases CC corrected
    private boolean discoveryConsistencyCheckEnabled = true;// enable or disable CC
    private boolean discoveryAgingEnabled = true; // enable or disable aging
    private boolean discoverySpoofingEnabled = true; // enable or disable spoofing neighbor of a production network

    private BlockingQueue<NodeConnector> transmitQ;
    private Thread transmitThread;
    private Boolean throttling = false; // if true, no more batching.
    private volatile Boolean shuttingDown = false;

    private LLDPTLV chassisIdTlv, portIdTlv, ttlTlv, customTlv;

    class DiscoveryTransmit implements Runnable {
        private final BlockingQueue<NodeConnector> transmitQ;

        DiscoveryTransmit(BlockingQueue<NodeConnector> transmitQ) {
            this.transmitQ = transmitQ;
        }

        public void run() {
            while (true) {
                try {
                    NodeConnector nodeConnector = transmitQ.take();
                    RawPacket outPkt = createDiscoveryPacket(nodeConnector);
                    sendDiscoveryPacket(nodeConnector, outPkt);
                    nodeConnector = null;
                } catch (InterruptedException e1) {
                    logger
                            .warn("DiscoveryTransmit interupted", e1
                                    .getMessage());
                    if (shuttingDown)
                        return;
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    class DiscoveryTimerTask extends TimerTask {
        public void run() {
            checkTimeout();
            checkAging();
            doConsistencyCheck();
            doDiscovery();
        }
    }

    private RawPacket createDiscoveryPacket(NodeConnector nodeConnector) {
        String nodeId = HexEncode.longToHexString((Long) nodeConnector
                .getNode().getID());

        // Create LLDP ChassisID TLV
        byte[] cidValue = LLDPTLV.createChassisIDTLVValue(nodeId);
        chassisIdTlv.setType((byte) LLDPTLV.TLVType.ChassisID.getValue())
                .setLength((short) cidValue.length).setValue(cidValue);

        // Create LLDP PortID TLV
        String portId = nodeConnector.getNodeConnectorIDString();
        byte[] pidValue = LLDPTLV.createPortIDTLVValue(portId);
        portIdTlv.setType((byte) LLDPTLV.TLVType.PortID.getValue())
                .setLength((short) pidValue.length).setValue(pidValue);

        // Create LLDP Custom TLV
        byte[] customValue = LLDPTLV.createCustomTLVValue(nodeConnector.toString());
        customTlv.setType((byte) LLDPTLV.TLVType.Custom.getValue())
                .setLength((short) customValue.length).setValue(customValue);

        // Create LLDP Custom Option list
        List<LLDPTLV> customList = new ArrayList<LLDPTLV>();
        customList.add(customTlv);

        // Create discovery pkt
        LLDP discoveryPkt = new LLDP();
        discoveryPkt.setChassisId(chassisIdTlv).setPortId(portIdTlv).setTtl(
                ttlTlv).setOptionalTLVList(customList);

        RawPacket rawPkt = null;
        try {
            // Create ethernet pkt
        	byte[] sourceMac = getSouceMACFromNodeID(nodeId);
            Ethernet ethPkt = new Ethernet();
            ethPkt.setSourceMACAddress(sourceMac).setDestinationMACAddress(
                    LLDP.LLDPMulticastMac).setEtherType(
                    EtherTypes.LLDP.shortValue()).setPayload(discoveryPkt);

            byte[] data = ethPkt.serialize();
            rawPkt = new RawPacket(data);
            rawPkt.setOutgoingNodeConnector(nodeConnector);
        } catch (ConstructionException cex) {
            logger.warn("RawPacket creation caught exception {}", cex
                    .getMessage());
        } catch (Exception e) {
            logger.error("Failed to serialize the LLDP packet: " + e);
        }

        return rawPkt;
    }

    private void sendDiscoveryPacket(NodeConnector nodeConnector,
            RawPacket outPkt) {
        if (nodeConnector == null) {
            logger.debug("Can not send discovery packet out since nodeConnector is null");
            return;
        }

        if (outPkt == null) {
            logger.debug("Can not send discovery packet out since outPkt is null");
            return;
        }

        long sid = (Long) nodeConnector.getNode().getID();
        ISwitch sw = controller.getSwitches().get(sid);

        if (sw == null) {
            logger.debug("Can not send discovery packet out since switch {} is null", sid);
            return;
        }

        if (!sw.isOperational()) {
            logger.debug("Can not send discovery packet out since switch {} is not operational", sw);
            return;
        }

        if (this.iDataPacketMux == null) {
            logger.debug("Can not send discovery packet out since DataPacket service is not available");
            return;
        }

        logger.trace("Sending topology discovery pkt thru {}", nodeConnector);
        this.iDataPacketMux.transmitDataPacket(outPkt);
    }

    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        if (inPkt == null) {
            logger.debug("Ignoring null packet");
            return PacketResult.IGNORED;
        }

        byte[] data = inPkt.getPacketData();
        if (data.length <= 0) {
            logger.trace("Ignoring zero length packet");
            return PacketResult.IGNORED;
        }

        if (!inPkt.getEncap().equals(LinkEncap.ETHERNET)) {
            logger.trace("Ignoring non ethernet packet");
            return PacketResult.IGNORED;
        }

        if (((Short) inPkt.getIncomingNodeConnector().getID())
                .equals(NodeConnector.SPECIALNODECONNECTORID)) {
            logger.trace("Ignoring ethernet packet received on special port: "
                    + inPkt.getIncomingNodeConnector().toString());
            return PacketResult.IGNORED;
        }

        Ethernet ethPkt = new Ethernet();
        try {
            ethPkt.deserialize(data, 0, data.length * NetUtils.NumBitsInAByte);
        } catch (Exception e) {
            logger.warn("Failed to decode LLDP packet from "
                    + inPkt.getIncomingNodeConnector() + ": " + e);
            return PacketResult.IGNORED;
        }
        if (ethPkt.getPayload() instanceof LLDP) {
            NodeConnector dst = inPkt.getIncomingNodeConnector();
            if (!processDiscoveryPacket(dst, ethPkt)) {
                /* Spoof the discovery pkt if not generated from us */
                spoofDiscoveryPacket(dst, ethPkt);
            }
            return PacketResult.CONSUME;
        }
        return PacketResult.IGNORED;
    }

    /*
     * Spoof incoming discovery frames generated by the production network neighbor switch
     */
    private void spoofDiscoveryPacket(NodeConnector dstNodeConnector,
            Ethernet ethPkt) {
        if (!this.discoverySpoofingEnabled) {
            return;
        }

        if ((dstNodeConnector == null) || (ethPkt == null)) {
            logger.trace("Quit spoofing discovery packet: Null node connector or packet");
            return;
        }

        LLDP lldp = (LLDP) ethPkt.getPayload();

        try {
            String nodeId = LLDPTLV.getHexStringValue(lldp.getChassisId().getValue(), lldp.getChassisId().getLength());
            String portId = LLDPTLV.getStringValue(lldp.getPortId().getValue(), lldp.getPortId().getLength());
			byte[] systemNameBytes = null;
	        // get system name if present in the LLDP pkt 
	        for (LLDPTLV lldptlv : lldp.getOptionalTLVList()) {
	        	if (lldptlv.getType() == LLDPTLV.TLVType.SystemName.getValue()) {
	        		systemNameBytes = lldptlv.getValue();
	        		break;
	        	}
	        }
			String nodeName = (systemNameBytes == null) ? nodeId : new String(systemNameBytes);
			Node srcNode = new Node(Node.NodeIDType.PRODUCTION, nodeName);
			NodeConnector srcNodeConnector = NodeConnectorCreator
                    .createNodeConnector(NodeConnector.NodeConnectorIDType.PRODUCTION,
                    		portId, srcNode);

            Edge edge = null;
            Set<Property> props = null;
            edge = new Edge(srcNodeConnector, dstNodeConnector);
            props = getProps(dstNodeConnector);

            updateProdEdge(edge, props);
        } catch (Exception e) {
            logger.warn("Caught exception ", e);
        }
    }

    /*
     * Handle discovery frames generated by our controller
     * @return true if it's a success
     */
    private boolean processDiscoveryPacket(NodeConnector dstNodeConnector,
            Ethernet ethPkt) {
        if ((dstNodeConnector == null) || (ethPkt == null)) {
            logger
                    .trace("Ignoring processing of discovery packet: Null node connector or packet");
            return false;
        }

        logger.trace("Handle discovery packet {} from {}", ethPkt,
                dstNodeConnector);

        LLDP lldp = (LLDP) ethPkt.getPayload();

        List<LLDPTLV> optionalTLVList = lldp.getOptionalTLVList();
        if (optionalTLVList == null) {
            logger.info("The discovery packet with null custom option from {}",
                    dstNodeConnector);
            return false;
        }

        Node srcNode = null;
        NodeConnector srcNodeConnector = null;
        for (LLDPTLV lldptlv : lldp.getOptionalTLVList()) {
            if (lldptlv.getType() == LLDPTLV.TLVType.Custom.getValue()) {
            	String ncString = LLDPTLV.getCustomString(lldptlv.getValue(), lldptlv.getLength());
            	srcNodeConnector = NodeConnector.fromString(ncString);
            	if (srcNodeConnector != null) {
            		srcNode = srcNodeConnector.getNode();
            		/* Check if it's expected */
            		if (isTracked(srcNodeConnector)) {
            			break;
                    } else {
                    	srcNode = null;
                    	srcNodeConnector = null;
                    }
                }
            }
        }

        if ((srcNode == null) || (srcNodeConnector == null)) {
            logger
                    .trace(
                            "Received non-controller generated discovery packet from {}",
                            dstNodeConnector);
            return false;
        }

        // push it out to Topology
        Edge edge = null;
        Set<Property> props = null;
        try {
            edge = new Edge(srcNodeConnector, dstNodeConnector);
            props = getProps(dstNodeConnector);
        } catch (ConstructionException e) {
            logger.error("Caught exception ", e);
        }
        addEdge(edge, props);

        logger.trace("Received discovery packet for Edge {}", edge);

        return true;
    }

    public Map<String, Property> getPropMap(NodeConnector nodeConnector) {
        if (nodeConnector == null) {
            return null;
        }

        if (pluginInInventoryService == null) {
            return null;
        }

        Map<NodeConnector, Map<String, Property>> props = pluginInInventoryService
                .getNodeConnectorProps(false);
        if (props == null) {
            return null;
        }

        return props.get(nodeConnector);
    }

    public Property getProp(NodeConnector nodeConnector, String propName) {
        Map<String, Property> propMap = getPropMap(nodeConnector);
        if (propMap == null) {
            return null;
        }

        Property prop = (Property) propMap.get(propName);
        return prop;
    }

    public Set<Property> getProps(NodeConnector nodeConnector) {
        Map<String, Property> propMap = getPropMap(nodeConnector);
        if (propMap == null) {
            return null;
        }

        Set<Property> props = new HashSet<Property>(propMap.values());
        return props;
    }

    private boolean isEnabled(NodeConnector nodeConnector) {
        if (nodeConnector == null) {
            return false;
        }

        Config config = (Config) getProp(nodeConnector, Config.ConfigPropName);
        State state = (State) getProp(nodeConnector, State.StatePropName);
        return ((config != null) && (config.getValue() == Config.ADMIN_UP)
                && (state != null) && (state.getValue() == State.EDGE_UP));
    }

    private boolean isTracked(NodeConnector nodeConnector) {
        if (readyListHi.contains(nodeConnector)) {
            return true;
        }

        if (readyListLo.contains(nodeConnector)) {
            return true;
        }

        if (pendingMap.keySet().contains(nodeConnector)) {
            return true;
        }

        if (waitingList.contains(nodeConnector)) {
            return true;
        }

        return false;
    }

    private Set<NodeConnector> getWorkingSet() {
        Set<NodeConnector> workingSet = new HashSet<NodeConnector>();
        Set<NodeConnector> removeSet = new HashSet<NodeConnector>();

        for (NodeConnector nodeConnector : readyListHi) {
            if (isOverLimit(workingSet.size())) {
                break;
            }

            workingSet.add(nodeConnector);
            removeSet.add(nodeConnector);
        }
        readyListHi.removeAll(removeSet);

        removeSet.clear();
        for (NodeConnector nodeConnector : readyListLo) {
            if (isOverLimit(workingSet.size())) {
                break;
            }

            workingSet.add(nodeConnector);
            removeSet.add(nodeConnector);
        }
        readyListLo.removeAll(removeSet);

        return workingSet;
    }

    private Boolean isOverLimit(int size) {
        return ((size >= discoveryBatchMaxPorts) && !throttling);
    }

    private void addDiscovery() {
        Map<Long, ISwitch> switches = controller.getSwitches();
        Set<Long> sidSet = switches.keySet();
        if (sidSet == null) {
            return;
        }
        for (Long sid : sidSet) {
            Node node = NodeCreator.createOFNode(sid);
            addDiscovery(node);
        }
    }

    private void addDiscovery(Node node) {
        Map<Long, ISwitch> switches = controller.getSwitches();
        ISwitch sw = switches.get((Long) node.getID());
        List<OFPhysicalPort> ports = sw.getEnabledPorts();
        if (ports == null) {
            return;
        }
        for (OFPhysicalPort port : ports) {
            NodeConnector nodeConnector = NodeConnectorCreator
                    .createOFNodeConnector(port.getPortNumber(), node);
            if (!readyListHi.contains(nodeConnector)) {
                readyListHi.add(nodeConnector);
            }
        }
    }

    private void addDiscovery(NodeConnector nodeConnector) {
        if (isTracked(nodeConnector)) {
            return;
        }

        readyListHi.add(nodeConnector);
    }

    private Set<NodeConnector> getRemoveSet(Collection<NodeConnector> c,
            Node node) {
        Set<NodeConnector> removeSet = new HashSet<NodeConnector>();
        if (c == null) {
            return removeSet;
        }
        for (NodeConnector nodeConnector : c) {
            if (node.equals(nodeConnector.getNode())) {
                removeSet.add(nodeConnector);
            }
        }
        return removeSet;
    }

    private void removeDiscovery(Node node) {
        Set<NodeConnector> removeSet;

        removeSet = getRemoveSet(readyListHi, node);
        readyListHi.removeAll(removeSet);

        removeSet = getRemoveSet(readyListLo, node);
        readyListLo.removeAll(removeSet);

        removeSet = getRemoveSet(waitingList, node);
        waitingList.removeAll(removeSet);

        removeSet = getRemoveSet(pendingMap.keySet(), node);
        for (NodeConnector nodeConnector : removeSet) {
            pendingMap.remove(nodeConnector);
        }

        removeSet = getRemoveSet(edgeMap.keySet(), node);
        for (NodeConnector nodeConnector : removeSet) {
            removeEdge(nodeConnector, false);
        }

        removeSet = getRemoveSet(prodMap.keySet(), node);
        for (NodeConnector nodeConnector : removeSet) {
            removeProdEdge(nodeConnector);
        }
    }

    private void removeDiscovery(NodeConnector nodeConnector) {
        readyListHi.remove(nodeConnector);
        readyListLo.remove(nodeConnector);
        waitingList.remove(nodeConnector);
        pendingMap.remove(nodeConnector);
        removeEdge(nodeConnector, false);
        removeProdEdge(nodeConnector);
    }

    private void checkTimeout() {
        Set<NodeConnector> removeSet = new HashSet<NodeConnector>();
        Set<NodeConnector> retrySet = new HashSet<NodeConnector>();
        int sentCount;

        Set<NodeConnector> pendingSet = pendingMap.keySet();
        if (pendingSet != null) {
            for (NodeConnector nodeConnector : pendingSet) {
                sentCount = pendingMap.get(nodeConnector);
                pendingMap.put(nodeConnector, ++sentCount);
                if (sentCount > getDiscoveryFinalTimeoutInterval()) {
                    // timeout the edge
                    removeSet.add(nodeConnector);
                    logger.trace("Discovery timeout {}", nodeConnector);
                } else if (sentCount % discoveryTimeoutTicks == 0) {
                    retrySet.add(nodeConnector);
                }
            }
        }

        for (NodeConnector nodeConnector : removeSet) {
            removeEdge(nodeConnector);
        }

        for (NodeConnector nodeConnector : retrySet) {
            transmitQ.add(nodeConnector);
        }
    }

    private void checkAging() {
        if (!discoveryAgingEnabled) {
            return;
        }

        Set<NodeConnector> removeSet = new HashSet<NodeConnector>();
        int sentCount;

        Set<NodeConnector> agingSet = agingMap.keySet();
        if (agingSet != null) {
            for (NodeConnector nodeConnector : agingSet) {
                sentCount = agingMap.get(nodeConnector);
                agingMap.put(nodeConnector, ++sentCount);
                if (sentCount > discoveryAgeoutTicks) {
                    // age out the edge
                    removeSet.add(nodeConnector);
                    logger.trace("Discovery age out {}", nodeConnector);
                }
            }
        }

        for (NodeConnector nodeConnector : removeSet) {
            removeProdEdge(nodeConnector);
        }
    }

    private void doDiscovery() {
        if (++discoveryTimerTickCount <= discoveryBatchPauseTicks) {
            for (NodeConnector nodeConnector : getWorkingSet()) {
                pendingMap.put(nodeConnector, 0);
                transmitQ.add(nodeConnector);
            }
        } else if (discoveryTimerTickCount >= discoveryBatchRestartTicks) {
            discoveryTimerTickCount = 0;
            for (NodeConnector nodeConnector : waitingList) {
                if (!readyListLo.contains(nodeConnector))
                    readyListLo.add(nodeConnector);
            }
            waitingList.removeAll(readyListLo);
        }
    }

    private void doConsistencyCheck() {
        if (!discoveryConsistencyCheckEnabled) {
            return;
        }

        if (++discoveryConsistencyCheckTickCount
                % getDiscoveryConsistencyCheckInterval() != 0) {
            return;
        }

        discoveryConsistencyCheckCallingTimes++;

        Set<NodeConnector> removeSet = new HashSet<NodeConnector>();
        Set<NodeConnector> ncSet = edgeMap.keySet();
        if (ncSet == null) {
            return;
        }
        for (NodeConnector nodeConnector : ncSet) {
            if (!isEnabled(nodeConnector)) {
                removeSet.add(nodeConnector);
                discoveryConsistencyCheckCorrected++;
                logger.debug("ConsistencyChecker: remove disabled {}",
                        nodeConnector);
                continue;
            }

            if (!isTracked(nodeConnector)) {
                waitingList.add(nodeConnector);
                discoveryConsistencyCheckCorrected++;
                logger.debug("ConsistencyChecker: add back untracked {}",
                        nodeConnector);
                continue;
            }
        }

        for (NodeConnector nodeConnector : removeSet) {
            removeEdge(nodeConnector, false);
        }

        // remove stale entries
        removeSet.clear();
        for (NodeConnector nodeConnector : waitingList) {
            if (!isEnabled(nodeConnector)) {
                removeSet.add(nodeConnector);
                discoveryConsistencyCheckCorrected++;
                logger.debug("ConsistencyChecker: remove disabled {}",
                        nodeConnector);
            }
        }
        waitingList.removeAll(removeSet);

        // Get a snapshot of all the existing switches
        Map<Long, ISwitch> switches = this.controller.getSwitches();
        for (ISwitch sw : switches.values()) {
            for (OFPhysicalPort port : sw.getEnabledPorts()) {
                Node node = NodeCreator.createOFNode(sw.getId());
                NodeConnector nodeConnector = NodeConnectorCreator
                        .createOFNodeConnector(port.getPortNumber(), node);
                if (!isTracked(nodeConnector)) {
                    waitingList.add(nodeConnector);
                    discoveryConsistencyCheckCorrected++;
                    logger.debug("ConsistencyChecker: add back untracked {}",
                            nodeConnector);
                }
            }
        }
    }

    private void addEdge(Edge edge, Set<Property> props) {
        if (edge == null) {
            return;
        }

        NodeConnector src = edge.getTailNodeConnector();
        if (!src.getType().equals(
                NodeConnector.NodeConnectorIDType.PRODUCTION)) {
            pendingMap.remove(src);
            if (!waitingList.contains(src)) {
                waitingList.add(src);
            }
        } else {
            NodeConnector dst = edge.getHeadNodeConnector();
            agingMap.put(dst, 0);
        }

        // notify routeEngine
        updateEdge(edge, UpdateType.ADDED, props);
        logger.trace("Add edge {}", edge);
    }

    
    /**
     * Update Production Edge
     * 
     * @param edge The Production Edge
     * @param props Properties associated with the edge
     */
    private void updateProdEdge(Edge edge, Set<Property> props) {
    	NodeConnector edgePort = edge.getHeadNodeConnector();
    	
    	/* Do not update in case there is an existing OpenFlow link */
    	if (edgeMap.get(edgePort) != null) {
    		logger.trace("Discarded edge {} since there is an existing OF link {}",
    				edge, edgeMap.get(edgePort));
    		return;
    	}
    	
    	/* Look for any existing Production Edge */
    	Edge oldEdge = prodMap.get(edgePort);    	
    	if (oldEdge == null) {
    		/* Let's add a new one */
    		addEdge(edge, props);
    	} else if (!edge.equals(oldEdge)) {
    		/* Remove the old one first */
    		removeProdEdge(oldEdge.getHeadNodeConnector());
    		/* Then add the new one */
    		addEdge(edge, props);    		
    	} else {
    		/* o/w, just reset the aging timer */
            NodeConnector dst = edge.getHeadNodeConnector();
            agingMap.put(dst, 0);    		
    	}
    }

    /**
     * Remove Production Edge for a given edge port
     * 
     * @param edgePort The OF edge port
     */
    private void removeProdEdge(NodeConnector edgePort) {
        agingMap.remove(edgePort);

        Edge edge = null;
        Set<NodeConnector> prodKeySet = prodMap.keySet();
        if ((prodKeySet != null) && (prodKeySet.contains(edgePort))) {
            edge = prodMap.get(edgePort);
            prodMap.remove(edgePort);
        }

        // notify Topology
        if (this.discoveryService != null) {
            this.discoveryService.notifyEdge(edge, UpdateType.REMOVED, null);
        }
        logger.trace("Remove edge {}", edge);
    }

    /*
     * Remove OpenFlow edge
     */
    private void removeEdge(NodeConnector nodeConnector, boolean stillEnabled) {
        pendingMap.remove(nodeConnector);
        readyListLo.remove(nodeConnector);
        readyListHi.remove(nodeConnector);

        if (stillEnabled) {
            // keep discovering
            if (!waitingList.contains(nodeConnector)) {
                waitingList.add(nodeConnector);
            }
        } else {
            // stop it
            waitingList.remove(nodeConnector);
        }

        Edge edge = null;
        Set<NodeConnector> edgeKeySet = edgeMap.keySet();
        if ((edgeKeySet != null) && (edgeKeySet.contains(nodeConnector))) {
            edge = edgeMap.get(nodeConnector);
            edgeMap.remove(nodeConnector);
        }

        // notify Topology
        if (this.discoveryService != null) {
            this.discoveryService.notifyEdge(edge, UpdateType.REMOVED, null);
        }
        logger.trace("Remove {}", nodeConnector);
    }

    private void removeEdge(NodeConnector nodeConnector) {
        removeEdge(nodeConnector, isEnabled(nodeConnector));
    }

    private void updateEdge(Edge edge, UpdateType type, Set<Property> props) {
        if (discoveryService == null) {
            return;
        }

        this.discoveryService.notifyEdge(edge, type, props);

        NodeConnector src = edge.getTailNodeConnector(), dst = edge
                .getHeadNodeConnector();
        if (!src.getType().equals(
                NodeConnector.NodeConnectorIDType.PRODUCTION)) {
            if (type == UpdateType.ADDED) {
                edgeMap.put(src, edge);
            } else {
                edgeMap.remove(src);
            }
        } else {
            /*
             * Save Production edge into different DB keyed by the Edge port
             */
            if (type == UpdateType.ADDED) {
                prodMap.put(dst, edge);
            } else {
                prodMap.remove(dst);
            }
        }
    }

    private void moreToReadyListHi(NodeConnector nodeConnector) {
        if (readyListLo.contains(nodeConnector)) {
            readyListLo.remove(nodeConnector);
            readyListHi.add(nodeConnector);
        } else if (waitingList.contains(nodeConnector)) {
            waitingList.remove(nodeConnector);
            readyListHi.add(nodeConnector);
        }
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
    }

    private int getDiscoveryConsistencyCheckInterval() {
        return discoveryConsistencyCheckMultiple * discoveryBatchRestartTicks;
    }

    private int getDiscoveryFinalTimeoutInterval() {
        return (discoveryRetry + 1) * discoveryTimeoutTicks;
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---Topology Discovery---\n");
        help.append("\t prlh                   - Print readyListHi entries\n");
        help.append("\t prll                   - Print readyListLo entries\n");
        help.append("\t pwl                    - Print waitingList entries\n");
        help.append("\t ppl                    - Print pendingList entries\n");
        help.append("\t ptick                  - Print tick time in msec\n");
        help.append("\t pcc                    - Print CC info\n");
        help.append("\t psize                  - Print sizes of all the lists\n");
        help.append("\t ptm                    - Print timeout info\n");
        help.append("\t ecc          		   - Enable CC\n");
        help.append("\t dcc          		   - Disable CC\n");
        help.append("\t scc [multiple]         - Set/show CC multiple and interval\n");
        help.append("\t sports [ports] 		   - Set/show max ports per batch\n");
        help.append("\t spause [ticks]         - Set/show pause period\n");
        help.append("\t sdi [ticks]       	   - Set/show discovery interval in ticks\n");
        help.append("\t stm [ticks]            - Set/show per timeout ticks\n");
        help.append("\t sretry [count] 		   - Set/show num of retries\n");
        help.append("\t addsw <swid> 		   - Add a switch\n");
        help.append("\t remsw <swid> 		   - Remove a switch\n");
        help.append("\t page                   - Print aging info\n");
        help.append("\t sage                   - Set/Show aging time limit\n");
        help.append("\t eage          		   - Enable aging\n");
        help.append("\t dage          		   - Disable aging\n");
        help.append("\t pthrot                 - Print throttling\n");
        help.append("\t ethrot          	   - Enable throttling\n");
        help.append("\t dthrot                 - Disable throttling\n");
        return help.toString();
    }

    public void _prlh(CommandInterpreter ci) {
        ci.println("ReadyListHi\n");
        for (NodeConnector nodeConnector : readyListHi) {
            if (nodeConnector == null) {
                continue;
            }
            ci.println(nodeConnector);
        }
    }

    public void _prll(CommandInterpreter ci) {
        ci.println("ReadyListLo\n");
        for (NodeConnector nodeConnector : readyListLo) {
            if (nodeConnector == null) {
                continue;
            }
            ci.println(nodeConnector);
        }
    }

    public void _pwl(CommandInterpreter ci) {
        ci.println("WaitingList\n");
        for (NodeConnector nodeConnector : waitingList) {
            if (nodeConnector == null) {
                continue;
            }
            ci.println(nodeConnector);
        }
    }

    public void _ppl(CommandInterpreter ci) {
        ci.println("PendingList\n");
        for (NodeConnector nodeConnector : pendingMap.keySet()) {
            if (nodeConnector == null) {
                continue;
            }
            ci.println(nodeConnector);
        }
    }

    public void _ptick(CommandInterpreter ci) {
        ci.println("Current timer is " + discoveryTimerTick + " msec per tick");
    }

    public void _pcc(CommandInterpreter ci) {
        if (discoveryConsistencyCheckEnabled) {
            ci.println("ConsistencyChecker is currently enabled");
        } else {
            ci.println("ConsistencyChecker is currently disabled");
        }
        ci.println("Interval " + getDiscoveryConsistencyCheckInterval());
        ci.println("Multiple " + discoveryConsistencyCheckMultiple);
        ci.println("Number of times called "
                + discoveryConsistencyCheckCallingTimes);
        ci.println("Corrected count " + discoveryConsistencyCheckCorrected);
    }

    public void _ptm(CommandInterpreter ci) {
        ci.println("Final timeout ticks " + getDiscoveryFinalTimeoutInterval());
        ci.println("Per timeout ticks " + discoveryTimeoutTicks);
        ci.println("Retry after initial timeout " + discoveryRetry);
    }

    public void _psize(CommandInterpreter ci) {
        ci.println("readyListLo size " + readyListLo.size() + "\n"
                + "readyListHi size " + readyListHi.size() + "\n"
                + "waitingList size " + waitingList.size() + "\n"
                + "pendingMap size " + pendingMap.size() + "\n"
                + "edgeMap size " + edgeMap.size() + "\n" + "prodMap size "
                + prodMap.size() + "\n" + "agingMap size " + agingMap.size());
    }

    public void _page(CommandInterpreter ci) {
        if (this.discoveryAgingEnabled) {
            ci.println("Aging is enabled");
        } else {
            ci.println("Aging is disabled");
        }
        ci.println("Current aging time limit " + this.discoveryAgeoutTicks);
        ci.println("\n");
        ci.println("                           Edge                                 Aging ");
        Collection<Edge> prodSet = prodMap.values();
        if (prodSet == null) {
            return;
        }
        for (Edge edge : prodSet) {
            Integer aging = agingMap.get(edge.getHeadNodeConnector());
            if (aging != null) {
                ci.println(edge + "      " + aging);
            }
        }
        ci.println("\n");
        ci.println("              NodeConnector                 				Edge ");
        Set<NodeConnector> keySet = prodMap.keySet();
        if (keySet == null) {
            return;
        }
        for (NodeConnector nc : keySet) {
            ci.println(nc + "      " + prodMap.get(nc));
        }
        return;
    }

    public void _sage(CommandInterpreter ci) {
        String val = ci.nextArgument();
        if (val == null) {
            ci.println("Please enter aging time limit. Current value "
                    + this.discoveryAgeoutTicks);
            return;
        }
        try {
            this.discoveryAgeoutTicks = Integer.parseInt(val);
        } catch (Exception e) {
            ci.println("Please enter a valid number");
        }
        return;
    }

    public void _eage(CommandInterpreter ci) {
        this.discoveryAgingEnabled = true;
        ci.println("Aging is enabled");
        return;
    }

    public void _dage(CommandInterpreter ci) {
        this.discoveryAgingEnabled = false;
        ci.println("Aging is disabled");
        return;
    }

    public void _scc(CommandInterpreter ci) {
        String val = ci.nextArgument();
        if (val == null) {
            ci.println("Please enter CC multiple. Current multiple "
                    + discoveryConsistencyCheckMultiple + " (interval "
                    + getDiscoveryConsistencyCheckInterval()
                    + ") calling times "
                    + discoveryConsistencyCheckCallingTimes);
            return;
        }
        try {
            discoveryConsistencyCheckMultiple = Integer.parseInt(val);
        } catch (Exception e) {
            ci.println("Please enter a valid number");
        }
        return;
    }

    public void _ecc(CommandInterpreter ci) {
        this.discoveryConsistencyCheckEnabled = true;
        ci.println("ConsistencyChecker is enabled");
        return;
    }

    public void _dcc(CommandInterpreter ci) {
        this.discoveryConsistencyCheckEnabled = false;
        ci.println("ConsistencyChecker is disabled");
        return;
    }

    public void _pspf(CommandInterpreter ci) {
        if (this.discoverySpoofingEnabled) {
            ci.println("Discovery spoofing is enabled");
        } else {
            ci.println("Discovery spoofing is disabled");
        }
        return;
    }

    public void _espf(CommandInterpreter ci) {
        this.discoverySpoofingEnabled = true;
        ci.println("Discovery spoofing is enabled");
        return;
    }

    public void _dspf(CommandInterpreter ci) {
        this.discoverySpoofingEnabled = false;
        ci.println("Discovery spoofing is disabled");
        return;
    }

    public void _spause(CommandInterpreter ci) {
        String val = ci.nextArgument();
        String out = "Please enter pause period less than "
				+ discoveryBatchRestartTicks + ". Current pause period is "
				+ discoveryBatchPausePeriod + " pause tick is "
				+ discoveryBatchPauseTicks + ".";

        if (val != null) {
            try {
                int pause = Integer.parseInt(val);
                if (pause < discoveryBatchRestartTicks) {
                	discoveryBatchPausePeriod = pause;
                    discoveryBatchPauseTicks = discoveryBatchRestartTicks - discoveryBatchPausePeriod;
                    return;
                }
            } catch (Exception e) {
            }
        }

        ci.println(out);
    }

    public void _sdi(CommandInterpreter ci) {
        String val = ci.nextArgument();
        String out = "Please enter discovery interval greater than "
				+ discoveryBatchPausePeriod + ". Current value is "
				+ discoveryBatchRestartTicks + ".";

        if (val != null) {
	        try {
	        	int restart = Integer.parseInt(val);        
	            if (restart > discoveryBatchPausePeriod) {
	            	discoveryBatchRestartTicks = restart;
	            	discoveryBatchPauseTicks = discoveryBatchRestartTicks - discoveryBatchPausePeriod;
	            	return;
	            }
	        } catch (Exception e) {
	        }
        }
        ci.println(out);
    }

    public void _sports(CommandInterpreter ci) {
        String val = ci.nextArgument();
        if (val == null) {
            ci.println("Please enter max ports per batch. Current value is "
                    + discoveryBatchMaxPorts);
            return;
        }
        try {
            discoveryBatchMaxPorts = Integer.parseInt(val);
        } catch (Exception e) {
            ci.println("Please enter a valid number");
        }
        return;
    }

    public void _sretry(CommandInterpreter ci) {
        String val = ci.nextArgument();
        if (val == null) {
            ci.println("Please enter number of retries. Current value is "
                    + discoveryRetry);
            return;
        }
        try {
            discoveryRetry = Integer.parseInt(val);
        } catch (Exception e) {
            ci.println("Please enter a valid number");
        }
        return;
    }

    public void _stm(CommandInterpreter ci) {
        String val = ci.nextArgument();
        String out = "Please enter timeout tick value less than "
                + discoveryBatchRestartTicks + ". Current value is "
                + discoveryTimeoutTicks;
        if (val != null) {
            try {
                int timeout = Integer.parseInt(val);
                if (timeout < discoveryBatchRestartTicks) {
                    discoveryTimeoutTicks = timeout;
                    return;
                }
            } catch (Exception e) {
            }
        }

        ci.println(out);
    }

    public void _addsw(CommandInterpreter ci) {
        String val = ci.nextArgument();
        Long sid;
        try {
            sid = Long.parseLong(val);
            Node node = NodeCreator.createOFNode(sid);
            addDiscovery(node);
        } catch (Exception e) {
            ci.println("Please enter a valid number");
        }
        return;
    }

    public void _remsw(CommandInterpreter ci) {
        String val = ci.nextArgument();
        Long sid;
        try {
            sid = Long.parseLong(val);
            Node node = NodeCreator.createOFNode(sid);
            removeDiscovery(node);
        } catch (Exception e) {
            ci.println("Please enter a valid number");
        }
        return;
    }

    public void _pthrot(CommandInterpreter ci) {
        if (this.throttling) {
            ci.println("Throttling is enabled");
        } else {
            ci.println("Throttling is disabled");
        }
    }

    public void _ethrot(CommandInterpreter ci) {
        this.throttling = true;
        ci.println("Throttling is enabled");
        return;
    }

    public void _dthrot(CommandInterpreter ci) {
        this.throttling = false;
        ci.println("Throttling is disabled");
        return;
    }

    @Override
    public void updateNode(Node node, UpdateType type, Set<Property> props) {
        switch (type) {
        case ADDED:
            addNode(node, props);
            break;
        case REMOVED:
            removeNode(node);
            break;
        default:
            break;
        }
    }

    @Override
    public void updateNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Set<Property> props) {
        Config config = null;
        State state = null;
        boolean enabled = false;

        for (Property prop : props) {
            if (prop.getName().equals(Config.ConfigPropName)) {
                config = (Config) prop;
            } else if (prop.getName().equals(State.StatePropName)) {
                state = (State) prop;
            }
        }
        enabled = ((config != null) && (config.getValue() == Config.ADMIN_UP)
                && (state != null) && (state.getValue() == State.EDGE_UP));

        switch (type) {
        case ADDED:
            if (enabled) {
                addDiscovery(nodeConnector);
                logger.trace("ADDED enabled {}", nodeConnector);
            } else {
                logger.trace("ADDED disabled {}", nodeConnector);
            }
            break;
        case CHANGED:
            if (enabled) {
                addDiscovery(nodeConnector);
                logger.trace("CHANGED enabled {}", nodeConnector);
            } else {
                removeDiscovery(nodeConnector);
                logger.trace("CHANGED disabled {}", nodeConnector);
            }
            break;
        case REMOVED:
            removeDiscovery(nodeConnector);
            logger.trace("REMOVED enabled {}", nodeConnector);
            break;
        default:
            return;
        }
    }

    public void addNode(Node node, Set<Property> props) {
        if (node == null)
            return;

        addDiscovery(node);
    }

    public void removeNode(Node node) {
        if (node == null)
            return;

        removeDiscovery(node);
    }

    public void updateNode(Node node, Set<Property> props) {
    }

    void setController(IController s) {
        this.controller = s;
    }

    void unsetController(IController s) {
        if (this.controller == s) {
            this.controller = null;
        }
    }

    public void setPluginInInventoryService(IPluginInInventoryService service) {
        this.pluginInInventoryService = service;
    }

    public void unsetPluginInInventoryService(IPluginInInventoryService service) {
        this.pluginInInventoryService = null;
    }

    public void setIDataPacketMux(IDataPacketMux service) {
        this.iDataPacketMux = service;
    }

    public void unsetIDataPacketMux(IDataPacketMux service) {
        if (this.iDataPacketMux == service) {
            this.iDataPacketMux = null;
        }
    }

    void setDiscoveryService(IDiscoveryService s) {
        this.discoveryService = s;
    }

    void unsetDiscoveryService(IDiscoveryService s) {
        if (this.discoveryService == s) {
            this.discoveryService = null;
        }
    }

    private void initDiscoveryPacket() {
        // Create LLDP ChassisID TLV
        chassisIdTlv = new LLDPTLV();
        chassisIdTlv.setType((byte) LLDPTLV.TLVType.ChassisID.getValue());

        // Create LLDP PortID TLV
        portIdTlv = new LLDPTLV();
        portIdTlv.setType((byte) LLDPTLV.TLVType.PortID.getValue());

        // Create LLDP TTL TLV
        byte[] ttl = new byte[] { (byte) 120 };
        ttlTlv = new LLDPTLV();
        ttlTlv.setType((byte) LLDPTLV.TLVType.TTL.getValue()).setLength(
                (short) ttl.length).setValue(ttl);

        customTlv = new LLDPTLV();
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        logger.trace("Init called");

        transmitQ = new LinkedBlockingQueue<NodeConnector>();

        readyListHi = new CopyOnWriteArrayList<NodeConnector>();
        readyListLo = new CopyOnWriteArrayList<NodeConnector>();
        waitingList = new CopyOnWriteArrayList<NodeConnector>();
        pendingMap = new ConcurrentHashMap<NodeConnector, Integer>();
        edgeMap = new ConcurrentHashMap<NodeConnector, Edge>();
        agingMap = new ConcurrentHashMap<NodeConnector, Integer>();
        prodMap = new ConcurrentHashMap<NodeConnector, Edge>();

        discoveryTimer = new Timer("DiscoveryService");
        discoveryTimerTask = new DiscoveryTimerTask();

        transmitThread = new Thread(new DiscoveryTransmit(transmitQ));

        initDiscoveryPacket();

        registerWithOSGIConsole();
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        transmitQ = null;
        readyListHi = null;
        readyListLo = null;
        waitingList = null;
        pendingMap = null;
        edgeMap = null;
        agingMap = null;
        prodMap = null;
        discoveryTimer = null;
        discoveryTimerTask = null;
        transmitThread = null;
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
        discoveryTimer.schedule(discoveryTimerTask, discoveryTimerTick,
                discoveryTimerTick);
        transmitThread.start();
    }

    /**
     * Function called after registering the
     * service in OSGi service registry.
     */
    void started() {
        /* get a snapshot of all the existing switches */
        addDiscovery();
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
        shuttingDown = true;
        discoveryTimer.cancel();
        transmitThread.interrupt();
    }

    @Override
    public void tagUpdated(String containerName, Node n, short oldTag,
            short newTag, UpdateType t) {
    }

    @Override
    public void containerFlowUpdated(String containerName,
            ContainerFlow previousFlow, ContainerFlow currentFlow, UpdateType t) {
    }

    @Override
    public void nodeConnectorUpdated(String containerName, NodeConnector p,
            UpdateType t) {
        switch (t) {
        case ADDED:
            moreToReadyListHi(p);
            break;
        default:
            break;
        }
    }

    @Override
    public void containerModeUpdated(UpdateType t) {
        // do nothing
    }
    
    private byte[] getSouceMACFromNodeID(String nodeId) {        
        byte[] cid = HexEncode.bytesFromHexString(nodeId);
        byte[] sourceMac = new byte[6];
        int pos = cid.length - sourceMac.length;

        if (pos >= 0) {
        	System.arraycopy(cid, pos, sourceMac, 0, sourceMac.length);
        }
        
        return sourceMac;
    }
}
