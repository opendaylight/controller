/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.opendaylight.controller.protocol_plugin.openflow.IDiscoveryListener;
import org.opendaylight.controller.protocol_plugin.openflow.IInventoryProvider;
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
public class DiscoveryService implements IInventoryShimExternalListener, IDataPacketListen, IContainerListener,
        CommandProvider {
    private static Logger logger = LoggerFactory.getLogger(DiscoveryService.class);
    private IController controller = null;
    private IDiscoveryListener discoveryListener = null;
    private IInventoryProvider inventoryProvider = null;
    private IDataPacketMux iDataPacketMux = null;
    // High priority list containing newly added ports which will be served first
    private List<NodeConnector> readyListHi = null;
    // List containing all the ports which will be served periodically
    private List<NodeConnector> readyListLo = null;
    // Staging area during quiet period
    private List<NodeConnector> stagingList = null;
    // Wait for next discovery packet. The map contains the time elapsed since
    // the last received LLDP frame on each node connector
    private ConcurrentMap<NodeConnector, Integer> holdTime = null;
    // Allow one more retry for newly added ports. This map contains the time
    // period elapsed since last discovery pkt transmission on the port.
    private ConcurrentMap<NodeConnector, Integer> elapsedTime = null;
    // OpenFlow edges keyed by head connector
    private ConcurrentMap<NodeConnector, Edge> edgeMap = null;
    // The map contains aging entry keyed by head connector of Production edge
    private ConcurrentMap<NodeConnector, Integer> agingMap = null;
    // Production edges keyed by head connector
    private ConcurrentMap<NodeConnector, Edge> prodMap = null;

    private Timer discoveryTimer;
    private DiscoveryTimerTask discoveryTimerTask;
    private final static long discoveryTimerTick = 2L * 1000; // per tick in msec
    private int discoveryTimerTickCount = 0; // main tick counter
    // Max # of ports handled in one batch
    private int discoveryBatchMaxPorts;
    // Periodically restart batching process
    private int discoveryBatchRestartTicks;
    private int discoveryBatchPausePeriod = 2;
    // Pause after this point
    private int discoveryBatchPauseTicks;
    private int discoveryTimeoutTicks;
    private int discoveryThresholdTicks;
    private int discoveryAgeoutTicks;
    // multiple of discoveryBatchRestartTicks
    private int discoveryConsistencyCheckMultiple = 2;
    // CC tick counter
    private int discoveryConsistencyCheckTickCount;
    // # of times CC gets called
    private int discoveryConsistencyCheckCallingTimes = 0;
    // # of cases CC corrected
    private int discoveryConsistencyCheckCorrected = 0;
    // Enable or disable CC
    private boolean discoveryConsistencyCheckEnabled = true;
    // Enable or disable aging
    private boolean discoveryAgingEnabled = true;
    // Global flag to enable or disable LLDP snooping
    private boolean discoverySnoopingEnabled = true;
    // The list of ports that will not do LLDP snooping
    private List<NodeConnector> discoverySnoopingDisableList;
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

        @Override
        public void run() {
            while (true) {
                try {
                    NodeConnector nodeConnector = transmitQ.take();
                    RawPacket outPkt = createDiscoveryPacket(nodeConnector);
                    sendDiscoveryPacket(nodeConnector, outPkt);
                    nodeConnector = null;
                } catch (InterruptedException e1) {
                    logger.warn("DiscoveryTransmit interupted", e1.getMessage());
                    if (shuttingDown) {
                        return;
                    }
                } catch (Exception e2) {
                    logger.error("", e2);
                }
            }
        }
    }

    class DiscoveryTimerTask extends TimerTask {
        @Override
        public void run() {
            checkTimeout();
            checkAging();
            doConsistencyCheck();
            doDiscovery();
        }
    }

    public enum DiscoveryPeriod {
        INTERVAL        (300),
        AGEOUT          (120),
        THRESHOLD       (10);

        private int time;   // sec
        private int tick;   // tick

        DiscoveryPeriod(int time) {
            this.time = time;
            this.tick = time2Tick(time);
        }

        public int getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
            this.tick = time2Tick(time);
        }

        public int getTick() {
            return tick;
        }

        public void setTick(int tick) {
            this.time = tick2Time(tick);
            this.tick = tick;
        }

        private int time2Tick(int time) {
            return (int) (time / (discoveryTimerTick / 1000));
        }

        private int tick2Time(int tick) {
            return (int) (tick * (discoveryTimerTick / 1000));
        }
    }

    private RawPacket createDiscoveryPacket(NodeConnector nodeConnector) {
        String nodeId = HexEncode.longToHexString((Long) nodeConnector.getNode().getID());

        // Create LLDP ChassisID TLV
        byte[] cidValue = LLDPTLV.createChassisIDTLVValue(nodeId);
        chassisIdTlv.setType(LLDPTLV.TLVType.ChassisID.getValue()).setLength((short) cidValue.length)
                .setValue(cidValue);

        // Create LLDP PortID TLV
        String portId = nodeConnector.getNodeConnectorIDString();
        byte[] pidValue = LLDPTLV.createPortIDTLVValue(portId);
        portIdTlv.setType(LLDPTLV.TLVType.PortID.getValue()).setLength((short) pidValue.length).setValue(pidValue);

        // Create LLDP Custom TLV
        byte[] customValue = LLDPTLV.createCustomTLVValue(nodeConnector.toString());
        customTlv.setType(LLDPTLV.TLVType.Custom.getValue()).setLength((short) customValue.length)
                .setValue(customValue);

        // Create LLDP Custom Option list
        List<LLDPTLV> customList = new ArrayList<LLDPTLV>();
        customList.add(customTlv);

        // Create discovery pkt
        LLDP discoveryPkt = new LLDP();
        discoveryPkt.setChassisId(chassisIdTlv).setPortId(portIdTlv).setTtl(ttlTlv).setOptionalTLVList(customList);

        RawPacket rawPkt = null;
        try {
            // Create ethernet pkt
            byte[] sourceMac = getSourceMACFromNodeID(nodeId);
            Ethernet ethPkt = new Ethernet();
            ethPkt.setSourceMACAddress(sourceMac).setDestinationMACAddress(LLDP.LLDPMulticastMac)
                    .setEtherType(EtherTypes.LLDP.shortValue()).setPayload(discoveryPkt);

            byte[] data = ethPkt.serialize();
            rawPkt = new RawPacket(data);
            rawPkt.setOutgoingNodeConnector(nodeConnector);
        } catch (ConstructionException cex) {
            logger.warn("RawPacket creation caught exception {}", cex.getMessage());
        } catch (Exception e) {
            logger.error("Failed to serialize the LLDP packet: " + e);
        }

        return rawPkt;
    }

    private void sendDiscoveryPacket(NodeConnector nodeConnector, RawPacket outPkt) {
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

        if (((Short) inPkt.getIncomingNodeConnector().getID()).equals(NodeConnector.SPECIALNODECONNECTORID)) {
            logger.trace("Ignoring ethernet packet received on special port: "
                    + inPkt.getIncomingNodeConnector().toString());
            return PacketResult.IGNORED;
        }

        Ethernet ethPkt = new Ethernet();
        try {
            ethPkt.deserialize(data, 0, data.length * NetUtils.NumBitsInAByte);
        } catch (Exception e) {
            logger.warn("Failed to decode LLDP packet from {}: {}", inPkt.getIncomingNodeConnector(), e);
            return PacketResult.IGNORED;
        }

        if (ethPkt.getPayload() instanceof LLDP) {
            NodeConnector dst = inPkt.getIncomingNodeConnector();
            if (isEnabled(dst)) {
                if (!processDiscoveryPacket(dst, ethPkt)) {
                    // Snoop the discovery pkt if not generated from us
                    snoopDiscoveryPacket(dst, ethPkt);
                }
                return PacketResult.CONSUME;
            }
        }
        return PacketResult.IGNORED;
    }

    /*
     * Snoop incoming discovery frames generated by the production network
     * neighbor switch
     */
    private void snoopDiscoveryPacket(NodeConnector dstNodeConnector, Ethernet ethPkt) {
        if (!this.discoverySnoopingEnabled || discoverySnoopingDisableList.contains(dstNodeConnector)) {
            logger.trace("Discarded received discovery packet on {} since snooping is turned off", dstNodeConnector);
            return;
        }

        if ((dstNodeConnector == null) || (ethPkt == null)) {
            logger.trace("Quit snooping discovery packet: Null node connector or packet");
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
            String nodeName = (systemNameBytes == null) ? nodeId
                    : new String(systemNameBytes, Charset.defaultCharset());
            Node srcNode = new Node(Node.NodeIDType.PRODUCTION, nodeName);
            NodeConnector srcNodeConnector = NodeConnectorCreator.createNodeConnector(
                    NodeConnector.NodeConnectorIDType.PRODUCTION, portId, srcNode);

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
     *
     * @return true if it's a success
     */
    private boolean processDiscoveryPacket(NodeConnector dstNodeConnector, Ethernet ethPkt) {
        if ((dstNodeConnector == null) || (ethPkt == null)) {
            logger.trace("Ignoring processing of discovery packet: Null node connector or packet");
            return false;
        }

        logger.trace("Handle discovery packet {} from {}", ethPkt, dstNodeConnector);

        LLDP lldp = (LLDP) ethPkt.getPayload();

        List<LLDPTLV> optionalTLVList = lldp.getOptionalTLVList();
        if (optionalTLVList == null) {
            logger.info("The discovery packet with null custom option from {}", dstNodeConnector);
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
                }
            }
        }

        if ((srcNode == null) || (srcNodeConnector == null)) {
            logger.trace("Received non-controller generated discovery packet from {}", dstNodeConnector);
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

        if (inventoryProvider == null) {
            return null;
        }

        Map<NodeConnector, Map<String, Property>> props = inventoryProvider.getNodeConnectorProps(false);
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

        Property prop = propMap.get(propName);
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
        return ((config != null) && (config.getValue() == Config.ADMIN_UP) && (state != null) && (state.getValue() == State.EDGE_UP));
    }

    private boolean isTracked(NodeConnector nodeConnector) {
        if (readyListHi.contains(nodeConnector)) {
            return true;
        }

        if (readyListLo.contains(nodeConnector)) {
            return true;
        }

        if (holdTime.keySet().contains(nodeConnector)) {
            return true;
        }

        if (stagingList.contains(nodeConnector)) {
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

            // Put it in the map and start the timer. It may need retry.
            elapsedTime.put(nodeConnector, 0);
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
        ISwitch sw = switches.get(node.getID());
        List<OFPhysicalPort> ports = sw.getEnabledPorts();
        if (ports == null) {
            return;
        }
        for (OFPhysicalPort port : ports) {
            NodeConnector nodeConnector = NodeConnectorCreator.createOFNodeConnector(port.getPortNumber(), node);
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

    private Set<NodeConnector> getRemoveSet(Collection<NodeConnector> c, Node node) {
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

        removeSet = getRemoveSet(stagingList, node);
        stagingList.removeAll(removeSet);

        removeSet = getRemoveSet(holdTime.keySet(), node);
        for (NodeConnector nodeConnector : removeSet) {
            holdTime.remove(nodeConnector);
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
        stagingList.remove(nodeConnector);
        holdTime.remove(nodeConnector);
        removeEdge(nodeConnector, false);
        removeProdEdge(nodeConnector);
    }

    private void checkTimeout() {
        Set<NodeConnector> removeSet = new HashSet<NodeConnector>();
        int ticks;

        Set<NodeConnector> monitorSet = holdTime.keySet();
        if (monitorSet != null) {
            for (NodeConnector nodeConnector : monitorSet) {
                ticks = holdTime.get(nodeConnector);
                holdTime.put(nodeConnector, ++ticks);
                if (ticks >= discoveryTimeoutTicks) {
                    // timeout the edge
                    removeSet.add(nodeConnector);
                    logger.trace("Discovery timeout {}", nodeConnector);
                }
            }
        }

        for (NodeConnector nodeConnector : removeSet) {
            removeEdge(nodeConnector);
        }

        Set<NodeConnector> retrySet = new HashSet<NodeConnector>();
        Set<NodeConnector> ncSet = elapsedTime.keySet();
        if ((ncSet != null) && (ncSet.size() > 0)) {
            for (NodeConnector nodeConnector : ncSet) {
                ticks = elapsedTime.get(nodeConnector);
                elapsedTime.put(nodeConnector, ++ticks);
                if (ticks >= discoveryThresholdTicks) {
                    retrySet.add(nodeConnector);
                }
            }

            for (NodeConnector nodeConnector : retrySet) {
                // Allow one more retry
                readyListLo.add(nodeConnector);
                elapsedTime.remove(nodeConnector);
            }
        }
    }

    private void checkAging() {
        if (!discoveryAgingEnabled) {
            return;
        }

        Set<NodeConnector> removeSet = new HashSet<NodeConnector>();
        int ticks;

        Set<NodeConnector> agingSet = agingMap.keySet();
        if (agingSet != null) {
            for (NodeConnector nodeConnector : agingSet) {
                ticks = agingMap.get(nodeConnector);
                agingMap.put(nodeConnector, ++ticks);
                if (ticks > discoveryAgeoutTicks) {
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
                transmitQ.add(nodeConnector);
                // Move to staging area after it's served
                if (!stagingList.contains(nodeConnector)) {
                    stagingList.add(nodeConnector);
                }
            }
        } else if (discoveryTimerTickCount >= discoveryBatchRestartTicks) {
            discoveryTimerTickCount = 0;
            for (NodeConnector nodeConnector : stagingList) {
                if (!readyListLo.contains(nodeConnector)) {
                    readyListLo.add(nodeConnector);
                }
            }
            stagingList.removeAll(readyListLo);
        }
    }

    private void doConsistencyCheck() {
        if (!discoveryConsistencyCheckEnabled) {
            return;
        }

        if (++discoveryConsistencyCheckTickCount % getDiscoveryConsistencyCheckInterval() != 0) {
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
                logger.debug("ConsistencyChecker: remove disabled {}", nodeConnector);
                continue;
            }

            if (!isTracked(nodeConnector)) {
                stagingList.add(nodeConnector);
                discoveryConsistencyCheckCorrected++;
                logger.debug("ConsistencyChecker: add back untracked {}", nodeConnector);
                continue;
            }
        }

        for (NodeConnector nodeConnector : removeSet) {
            removeEdge(nodeConnector, false);
        }

        // remove stale entries
        removeSet.clear();
        for (NodeConnector nodeConnector : stagingList) {
            if (!isEnabled(nodeConnector)) {
                removeSet.add(nodeConnector);
                discoveryConsistencyCheckCorrected++;
                logger.debug("ConsistencyChecker: remove disabled {}", nodeConnector);
            }
        }
        stagingList.removeAll(removeSet);

        // Get a snapshot of all the existing switches
        Map<Long, ISwitch> switches = this.controller.getSwitches();
        for (ISwitch sw : switches.values()) {
            for (OFPhysicalPort port : sw.getEnabledPorts()) {
                Node node = NodeCreator.createOFNode(sw.getId());
                NodeConnector nodeConnector = NodeConnectorCreator.createOFNodeConnector(port.getPortNumber(), node);
                if (!isTracked(nodeConnector)) {
                    stagingList.add(nodeConnector);
                    discoveryConsistencyCheckCorrected++;
                    logger.debug("ConsistencyChecker: add back untracked {}", nodeConnector);
                }
            }
        }
    }

    private void addEdge(Edge edge, Set<Property> props) {
        if (edge == null) {
            return;
        }

        NodeConnector src = edge.getTailNodeConnector();
        NodeConnector dst = edge.getHeadNodeConnector();
        if (!src.getType().equals(NodeConnector.NodeConnectorIDType.PRODUCTION)) {
            holdTime.put(dst, 0);
        } else {
            agingMap.put(dst, 0);
        }
        elapsedTime.remove(src);

        // notify
        updateEdge(edge, UpdateType.ADDED, props);
        logger.trace("Add edge {}", edge);
    }

    /**
     * Update Production Edge
     *
     * @param edge
     *            The Production Edge
     * @param props
     *            Properties associated with the edge
     */
    private void updateProdEdge(Edge edge, Set<Property> props) {
        NodeConnector edgePort = edge.getHeadNodeConnector();

        /* Do not update in case there is an existing OpenFlow link */
        if (edgeMap.get(edgePort) != null) {
            logger.trace("Discarded edge {} since there is an existing OF link {}", edge, edgeMap.get(edgePort));
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
     * @param edgePort
     *            The OF edge port
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
        if (this.discoveryListener != null) {
            this.discoveryListener.notifyEdge(edge, UpdateType.REMOVED, null);
        }
        logger.trace("Remove edge {}", edge);
    }

    /*
     * Remove OpenFlow edge
     */
    private void removeEdge(NodeConnector nodeConnector, boolean stillEnabled) {
        holdTime.remove(nodeConnector);
        readyListLo.remove(nodeConnector);
        readyListHi.remove(nodeConnector);

        if (stillEnabled) {
            // keep discovering
            if (!stagingList.contains(nodeConnector)) {
                stagingList.add(nodeConnector);
            }
        } else {
            // stop it
            stagingList.remove(nodeConnector);
        }

        Edge edge = null;
        Set<NodeConnector> edgeKeySet = edgeMap.keySet();
        if ((edgeKeySet != null) && (edgeKeySet.contains(nodeConnector))) {
            edge = edgeMap.get(nodeConnector);
            edgeMap.remove(nodeConnector);
        }

        // notify Topology
        if (this.discoveryListener != null) {
            this.discoveryListener.notifyEdge(edge, UpdateType.REMOVED, null);
        }
        logger.trace("Remove {}", nodeConnector);
    }

    private void removeEdge(NodeConnector nodeConnector) {
        removeEdge(nodeConnector, isEnabled(nodeConnector));
    }

    private void updateEdge(Edge edge, UpdateType type, Set<Property> props) {
        if (discoveryListener == null) {
            return;
        }

        this.discoveryListener.notifyEdge(edge, type, props);

        NodeConnector src = edge.getTailNodeConnector(), dst = edge.getHeadNodeConnector();
        if (!src.getType().equals(NodeConnector.NodeConnectorIDType.PRODUCTION)) {
            if (type == UpdateType.ADDED) {
                edgeMap.put(dst, edge);
            } else {
                edgeMap.remove(dst);
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

    private void moveToReadyListHi(NodeConnector nodeConnector) {
        if (readyListLo.contains(nodeConnector)) {
            readyListLo.remove(nodeConnector);
        } else if (stagingList.contains(nodeConnector)) {
            stagingList.remove(nodeConnector);
        }
        readyListHi.add(nodeConnector);
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this, null);
    }

    private int getDiscoveryConsistencyCheckInterval() {
        return discoveryConsistencyCheckMultiple * discoveryBatchRestartTicks;
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---Topology Discovery---\n");
        help.append("\t prlh                            - Print readyListHi entries\n");
        help.append("\t prll                            - Print readyListLo entries\n");
        help.append("\t psl                             - Print stagingList entries\n");
        help.append("\t pht                             - Print hold time\n");
        help.append("\t pet                             - Print elapsed time\n");
        help.append("\t ptick                           - Print tick time in msec\n");
        help.append("\t pcc                             - Print CC info\n");
        help.append("\t psize                           - Print sizes of all the lists\n");
        help.append("\t ptm                             - Print timeout info\n");
        help.append("\t ecc                             - Enable CC\n");
        help.append("\t dcc                             - Disable CC\n");
        help.append("\t scc [multiple]                  - Set/show CC multiple and interval\n");
        help.append("\t sports [ports]                  - Set/show max ports per batch\n");
        help.append("\t spause [ticks]                  - Set/show pause period\n");
        help.append("\t sdi [ticks]                     - Set/show discovery interval in ticks\n");
        help.append("\t addsw <swid>                    - Add a switch\n");
        help.append("\t remsw <swid>                    - Remove a switch\n");
        help.append("\t page                            - Print aging info\n");
        help.append("\t sage                            - Set/Show aging time limit\n");
        help.append("\t eage                            - Enable aging\n");
        help.append("\t dage                            - Disable aging\n");
        help.append("\t pthrot                          - Print throttling\n");
        help.append("\t ethrot                          - Enable throttling\n");
        help.append("\t dthrot                          - Disable throttling\n");
        help.append("\t psnp                            - Print LLDP snooping\n");
        help.append("\t esnp <all|nodeConnector>        - Enable LLDP snooping\n");
        help.append("\t dsnp <all|nodeConnector>        - Disable LLDP snooping\n");
        return help.toString();
    }

    private List<NodeConnector> sortList(Collection<NodeConnector> ncs) {
        List<String> ncStrArray = new ArrayList<String>();
        for (NodeConnector nc : ncs) {
            ncStrArray.add(nc.toString());
        }
        Collections.sort(ncStrArray);

        List<NodeConnector> sortedNodeConnectors = new ArrayList<NodeConnector>();
        for (String ncStr : ncStrArray) {
            sortedNodeConnectors.add(NodeConnector.fromString(ncStr));
        }

        return sortedNodeConnectors;
    }

    public void _prlh(CommandInterpreter ci) {
        ci.println("readyListHi\n");
        for (NodeConnector nodeConnector : sortList(readyListHi)) {
            if (nodeConnector == null) {
                continue;
            }
            ci.println(nodeConnector);
        }
        ci.println("Total number of Node Connectors: " + readyListHi.size());
    }

    public void _prll(CommandInterpreter ci) {
        ci.println("readyListLo\n");
        for (NodeConnector nodeConnector : sortList(readyListLo)) {
            if (nodeConnector == null) {
                continue;
            }
            ci.println(nodeConnector);
        }
        ci.println("Total number of Node Connectors: " + readyListLo.size());
    }

    public void _psl(CommandInterpreter ci) {
        ci.println("stagingList\n");
        for (NodeConnector nodeConnector : sortList(stagingList)) {
            if (nodeConnector == null) {
                continue;
            }
            ci.println(nodeConnector);
        }
        ci.println("Total number of Node Connectors: " + stagingList.size());
    }

    public void _pht(CommandInterpreter ci) {
        ci.println("          NodeConnector            Last rx LLDP (sec)");
        for (ConcurrentMap.Entry<NodeConnector, Integer> entry: holdTime.entrySet()) {
            ci.println(entry.getKey() + "\t\t" + entry.getValue() * (discoveryTimerTick / 1000));
        }
        ci.println("\nSize: " + holdTime.size() + "\tTimeout: " + discoveryTimeoutTicks * (discoveryTimerTick / 1000)
                + " sec");
    }

    public void _pet(CommandInterpreter ci) {
        ci.println("          NodeConnector            Elapsed Time (sec)");
        for (ConcurrentMap.Entry<NodeConnector, Integer> entry: elapsedTime.entrySet()) {
            ci.println(entry.getKey() + "\t\t" + entry.getValue() * (discoveryTimerTick / 1000));
        }
        ci.println("\nSize: " + elapsedTime.size() + "\tThreshold: " + DiscoveryPeriod.THRESHOLD.getTime() + " sec");
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
        ci.println("Number of times called " + discoveryConsistencyCheckCallingTimes);
        ci.println("Corrected count " + discoveryConsistencyCheckCorrected);
    }

    public void _ptm(CommandInterpreter ci) {
        ci.println("Timeout " + discoveryTimeoutTicks + " ticks, " + discoveryTimerTick / 1000 + " sec per tick.");
    }

    public void _psize(CommandInterpreter ci) {
        ci.println("readyListLo size " + readyListLo.size() + "\n" + "readyListHi size " + readyListHi.size() + "\n"
                + "stagingList size " + stagingList.size() + "\n" + "holdTime size " + holdTime.size() + "\n"
                + "edgeMap size " + edgeMap.size() + "\n" + "prodMap size " + prodMap.size() + "\n" + "agingMap size "
                + agingMap.size() + "\n" + "elapsedTime size " + elapsedTime.size());
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
        ci.println("              NodeConnector                                                 Edge ");
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
            ci.println("Please enter aging time limit. Current value " + this.discoveryAgeoutTicks);
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
            ci.println("Please enter CC multiple. Current multiple " + discoveryConsistencyCheckMultiple
                    + " (interval " + getDiscoveryConsistencyCheckInterval() + ") calling times "
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

    public void _psnp(CommandInterpreter ci) {
        if (this.discoverySnoopingEnabled) {
            ci.println("Discovery snooping is globally enabled");
        } else {
            ci.println("Discovery snooping is globally disabled");
        }

        ci.println("\nDiscovery snooping is locally disabled on these ports");
        for (NodeConnector nodeConnector : discoverySnoopingDisableList) {
            ci.println(nodeConnector);
        }
        return;
    }

    public void _esnp(CommandInterpreter ci) {
        String val = ci.nextArgument();

        if (val == null) {
            ci.println("Usage: esnp <all|nodeConnector>");
        } else if (val.equalsIgnoreCase("all")) {
            this.discoverySnoopingEnabled = true;
            ci.println("Discovery snooping is globally enabled");
        } else {
            NodeConnector nodeConnector = NodeConnector.fromString(val);
            if (nodeConnector != null) {
                discoverySnoopingDisableList.remove(nodeConnector);
                ci.println("Discovery snooping is locally enabled on port " + nodeConnector);
            } else {
                ci.println("Entered invalid NodeConnector " + val);
            }
        }
        return;
    }

    public void _dsnp(CommandInterpreter ci) {
        String val = ci.nextArgument();

        if (val == null) {
            ci.println("Usage: dsnp <all|nodeConnector>");
        } else if (val.equalsIgnoreCase("all")) {
            this.discoverySnoopingEnabled = false;
            ci.println("Discovery snooping is globally disabled");
        } else {
            NodeConnector nodeConnector = NodeConnector.fromString(val);
            if (nodeConnector != null) {
                discoverySnoopingDisableList.add(nodeConnector);
                ci.println("Discovery snooping is locally disabled on port " + nodeConnector);
            } else {
                ci.println("Entered invalid NodeConnector " + val);
            }
        }
        return;
    }

    public void _spause(CommandInterpreter ci) {
        String val = ci.nextArgument();
        String out = "Please enter pause period less than " + discoveryBatchRestartTicks + ". Current pause period is "
                + discoveryBatchPausePeriod + " ticks, pause at " + discoveryBatchPauseTicks + " ticks, "
                + discoveryTimerTick / 1000 + " sec per tick.";

        if (val != null) {
            try {
                int pause = Integer.parseInt(val);
                if (pause < discoveryBatchRestartTicks) {
                    discoveryBatchPausePeriod = pause;
                    discoveryBatchPauseTicks = getDiscoveryPauseInterval();
                    return;
                }
            } catch (Exception e) {
            }
        }

        ci.println(out);
    }

    public void _sdi(CommandInterpreter ci) {
        String val = ci.nextArgument();
        String out = "Please enter discovery interval in ticks. Current value is " + discoveryBatchRestartTicks + " ticks, "
                + discoveryTimerTick / 1000 + " sec per tick.";

        if (val != null) {
            try {
                int ticks = Integer.parseInt(val);
                DiscoveryPeriod.INTERVAL.setTick(ticks);
                discoveryBatchRestartTicks = getDiscoveryInterval();
                discoveryBatchPauseTicks = getDiscoveryPauseInterval();
                discoveryTimeoutTicks = getDiscoveryTimeout();
                return;
            } catch (Exception e) {
            }
        }
        ci.println(out);
    }

    public void _sports(CommandInterpreter ci) {
        String val = ci.nextArgument();
        if (val == null) {
            ci.println("Please enter max ports per batch. Current value is " + discoveryBatchMaxPorts);
            return;
        }
        try {
            discoveryBatchMaxPorts = Integer.parseInt(val);
        } catch (Exception e) {
            ci.println("Please enter a valid number");
        }
        return;
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
    public void updateNodeConnector(NodeConnector nodeConnector, UpdateType type, Set<Property> props) {
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
        enabled = ((config != null) && (config.getValue() == Config.ADMIN_UP) && (state != null) && (state.getValue() == State.EDGE_UP));

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
        if (node == null) {
            return;
        }

        addDiscovery(node);
    }

    public void removeNode(Node node) {
        if (node == null) {
            return;
        }

        removeDiscovery(node);
    }

    void setController(IController s) {
        this.controller = s;
    }

    void unsetController(IController s) {
        if (this.controller == s) {
            this.controller = null;
        }
    }

    public void setInventoryProvider(IInventoryProvider service) {
        this.inventoryProvider = service;
    }

    public void unsetInventoryProvider(IInventoryProvider service) {
        this.inventoryProvider = null;
    }

    public void setIDataPacketMux(IDataPacketMux service) {
        this.iDataPacketMux = service;
    }

    public void unsetIDataPacketMux(IDataPacketMux service) {
        if (this.iDataPacketMux == service) {
            this.iDataPacketMux = null;
        }
    }

    void setDiscoveryListener(IDiscoveryListener s) {
        this.discoveryListener = s;
    }

    void unsetDiscoveryListener(IDiscoveryListener s) {
        if (this.discoveryListener == s) {
            this.discoveryListener = null;
        }
    }

    private void initDiscoveryPacket() {
        // Create LLDP ChassisID TLV
        chassisIdTlv = new LLDPTLV();
        chassisIdTlv.setType(LLDPTLV.TLVType.ChassisID.getValue());

        // Create LLDP PortID TLV
        portIdTlv = new LLDPTLV();
        portIdTlv.setType(LLDPTLV.TLVType.PortID.getValue());

        // Create LLDP TTL TLV
        byte[] ttl = new byte[] { (byte) 0, (byte) 120 };
        ttlTlv = new LLDPTLV();
        ttlTlv.setType(LLDPTLV.TLVType.TTL.getValue()).setLength((short) ttl.length).setValue(ttl);

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
        stagingList = new CopyOnWriteArrayList<NodeConnector>();
        holdTime = new ConcurrentHashMap<NodeConnector, Integer>();
        elapsedTime = new ConcurrentHashMap<NodeConnector, Integer>();
        edgeMap = new ConcurrentHashMap<NodeConnector, Edge>();
        agingMap = new ConcurrentHashMap<NodeConnector, Integer>();
        prodMap = new ConcurrentHashMap<NodeConnector, Edge>();
        discoverySnoopingDisableList = new CopyOnWriteArrayList<NodeConnector>();

        discoveryBatchRestartTicks = getDiscoveryInterval();
        discoveryBatchPauseTicks = getDiscoveryPauseInterval();
        discoveryTimeoutTicks = getDiscoveryTimeout();
        discoveryThresholdTicks = getDiscoveryThreshold();
        discoveryAgeoutTicks = getDiscoveryAgeout();
        discoveryConsistencyCheckTickCount = discoveryBatchPauseTicks;
        discoveryBatchMaxPorts = getDiscoveryBatchMaxPorts();

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
        stagingList = null;
        holdTime = null;
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
        discoveryTimer.schedule(discoveryTimerTask, discoveryTimerTick, discoveryTimerTick);
        transmitThread.start();
    }

    /**
     * Function called after registering the service in OSGi service registry.
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
    public void tagUpdated(String containerName, Node n, short oldTag, short newTag, UpdateType t) {
    }

    @Override
    public void containerFlowUpdated(String containerName, ContainerFlow previousFlow, ContainerFlow currentFlow,
            UpdateType t) {
    }

    @Override
    public void nodeConnectorUpdated(String containerName, NodeConnector p, UpdateType t) {
        switch (t) {
        case ADDED:
            moveToReadyListHi(p);
            break;
        default:
            break;
        }
    }

    @Override
    public void containerModeUpdated(UpdateType t) {
        // do nothing
    }

    private byte[] getSourceMACFromNodeID(String nodeId) {
        byte[] cid = HexEncode.bytesFromHexString(nodeId);
        byte[] sourceMac = new byte[6];
        int pos = cid.length - sourceMac.length;

        if (pos >= 0) {
            System.arraycopy(cid, pos, sourceMac, 0, sourceMac.length);
        }

        return sourceMac;
    }

    private int getDiscoveryTicks(DiscoveryPeriod dp, String val) {
        if (dp == null) {
            return 0;
        }

        if (val != null) {
            try {
                dp.setTime(Integer.parseInt(val));
            } catch (Exception e) {
            }
        }

        return dp.getTick();
    }

    /**
     * This method returns the interval which determines how often the discovery
     * packets will be sent.
     *
     * @return The discovery interval in ticks
     */
    private int getDiscoveryInterval() {
        String intvl = System.getProperty("of.discoveryInterval");
        return getDiscoveryTicks(DiscoveryPeriod.INTERVAL, intvl);
    }

    /**
     * This method returns the timeout value in receiving subsequent discovery packets on a port.
     *
     * @return The discovery timeout in ticks
     */
    private int getDiscoveryTimeout() {
        String val = System.getProperty("of.discoveryTimeoutMultiple");
        int multiple = 2;

        if (val != null) {
            try {
                multiple = Integer.parseInt(val);
            } catch (Exception e) {
            }
        }
        return getDiscoveryInterval() * multiple + 3;
    }

    /**
     * This method returns the user configurable threshold value
     *
     * @return The discovery threshold value in ticks
     */
    private int getDiscoveryThreshold() {
        String val = System.getProperty("of.discoveryThreshold");
        return getDiscoveryTicks(DiscoveryPeriod.THRESHOLD, val);
    }

    /**
     * This method returns the discovery entry aging time in ticks.
     *
     * @return The aging time in ticks
     */
    private int getDiscoveryAgeout() {
        return getDiscoveryTicks(DiscoveryPeriod.AGEOUT, null);
    }

    /**
     * This method returns the pause interval
     *
     * @return The pause interval in ticks
     */
    private int getDiscoveryPauseInterval() {
        if (discoveryBatchRestartTicks > discoveryBatchPausePeriod) {
            return discoveryBatchRestartTicks - discoveryBatchPausePeriod;
        } else {
            return discoveryBatchRestartTicks - 1;
        }
    }

    /**
     * This method returns the user configurable maximum number of ports handled
     * in one discovery batch.
     *
     * @return The maximum number of ports
     */
    private int getDiscoveryBatchMaxPorts() {
        String val = System.getProperty("of.discoveryBatchMaxPorts");
        int ports = 1024;

        if (val != null) {
            try {
                ports = Integer.parseInt(val);
            } catch (Exception e) {
            }
        }
        return ports;
    }
}
