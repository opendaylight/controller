
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.samples.simpleforwarding.internal;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Timer;
import java.util.TimerTask;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.action.SetDlDst;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.routing.IListenRoutingUpdates;
import org.opendaylight.controller.sal.routing.IRouting;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.samples.simpleforwarding.HostNodePair;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements basic L3 forwarding within the managed devices.
 * Forwarding is only done within configured subnets.</br>
 * <br/>
 * The basic flow is that the module listens for new hosts from the
 * {@link org.opendaylight.controller.hosttracker.IfIptoHost HostTracker}
 * service and on discovering a new host it first calls
 * <tt>preparePerHostRules()</tt> to create a set of new rules that must be
 * installed in the network. This is done by repeatedly calling
 * <tt>updatePerHostRuleInSW()</tt> for each switch in the network. Then it
 * installs those rules using <tt>installPerHostRules()</tt>.
 */
public class SimpleForwardingImpl implements IfNewHostNotify,
        IListenRoutingUpdates, IInventoryListener, IListenDataPacket {
    private static Logger log = LoggerFactory.getLogger(SimpleForwardingImpl.class);
    private static short DEFAULT_IPSWITCH_PRIORITY = 1;
    static final String FORWARDING_RULES_CACHE_NAME = "forwarding.ipswitch.rules";
    private IfIptoHost hostTracker;
    private IForwardingRulesManager frm;
    private ITopologyManager topologyManager;
    private IRouting routing;

    /**
     * The set of all forwarding rules: (host) -> (switch -> flowmod). Note that
     * the host includes an attachment point and that while the switch appears
     * to be a switch's port, in actuality it is a special port which just
     * represents the switch.
     */
    private ConcurrentMap<HostNodePair, HashMap<NodeConnector, FlowEntry>> rulesDB;
    private Map<Node, List<FlowEntry>> tobePrunedPos = new HashMap<Node, List<FlowEntry>>();
    private IClusterContainerServices clusterContainerService = null;
    private ISwitchManager switchManager;
    private IDataPacketService dataPacketService;

    /**
     * Ip packets that are punted may not have their destination known by hostTracker at the time it
     * is presented to SimpleForwardingImpl. Instead of dropping the packet, we will keep it around
     * for a 'little' while, to accommodate any transients. See bug 590 for more details.
     */
    private class PendingPacketData {
        private final static byte MAX_AGE = 2;

        public final IPv4 pkt;
        public final NodeConnector incomingNodeConnector;
        private byte age;

        public PendingPacketData(IPv4 pkt, NodeConnector incomingNodeConnector) {
            this.pkt = pkt;
            this.incomingNodeConnector = incomingNodeConnector;
            this.age = 0;
        }
        boolean bumpAgeAndCheckIfTooOld() { return ++age > MAX_AGE; }
    }
    private static final int MAX_PENDING_PACKET_DESTINATIONS = 64;
    private ConcurrentMap<InetAddress, PendingPacketData> pendingPacketDestinations;
    private Timer pendingPacketsAgerTimer;

    private class PendingPacketsAgerTimerHandler extends TimerTask {
        @Override
        public void run() {
            if (pendingPacketDestinations == null) {
                return;
            }
            try {
                Iterator<ConcurrentMap.Entry<InetAddress, PendingPacketData>> iterator =
                        pendingPacketDestinations.entrySet().iterator();
                while (iterator.hasNext()) {
                    ConcurrentHashMap.Entry<InetAddress, PendingPacketData> entry = iterator.next();
                    InetAddress dIP = entry.getKey();
                    PendingPacketData pendingPacketData = entry.getValue();

                    if (pendingPacketData.bumpAgeAndCheckIfTooOld()) {
                        iterator.remove(); // safe to remove while iterating...
                        log.debug("Pending packet for {} has been aged out", dIP);
                    } else {
                        /** Replace the entry for a key only if currently mapped to some value.
                         * This will protect the concurrent map against a race where this thread
                         * would be re-adding an entry that just got taken out.
                         */
                        pendingPacketDestinations.replace(dIP, pendingPacketData);
                    }
                }
            } catch (IllegalStateException e) {
                log.warn("IllegalStateException Received by PendingPacketsAgerTimerHandler from: {}",
                        e.getMessage());
            }
        }
    }

    /**
     * Add punted packet to pendingPackets
     */
    private void addToPendingPackets(InetAddress dIP, IPv4 pkt, NodeConnector incomingNodeConnector) {
        if (pendingPacketDestinations.size() >= MAX_PENDING_PACKET_DESTINATIONS) {
            log.info("Will not pend packet for {}: Too many destinations", dIP);
            return;
        }

        /** TODO: The current implementation allows for up to 1 pending packet per InetAddress.
         * This limitation is done for sake of simplicity. A potential enhancement could be to use a
         * ConcurrentMultiMap instead of ConcurrentMap.
         */
        if (pendingPacketDestinations.containsKey(dIP)) {
            log.trace("Will not pend packet for {}: Already have a packet pending", dIP);
            return;
        }

        PendingPacketData pendingPacketData = new PendingPacketData(pkt, incomingNodeConnector);
        pendingPacketDestinations.put(dIP, pendingPacketData);
        log.debug("Pending packet for {}", dIP);
    }

    /**
     * Send punted packet to given destination. This is invoked when there is a certain level of
     * hope that the destination is known by hostTracker.
     */
    private void sendPendingPacket(InetAddress dIP) {
        PendingPacketData pendingPacketData = pendingPacketDestinations.get(dIP);
        if (pendingPacketData != null) {
            handlePuntedIPPacket(pendingPacketData.pkt, pendingPacketData.incomingNodeConnector, false);
            log.trace("Packet for {} is no longer pending", dIP);
            pendingPacketDestinations.remove(dIP);
        }
    }

    /**
     * Return codes from the programming of the perHost rules in HW
     */
    public enum RulesProgrammingReturnCode {
        SUCCESS, FAILED_FEW_SWITCHES, FAILED_ALL_SWITCHES, FAILED_WRONG_PARAMS
    }
    public void setDataPacketService(IDataPacketService s) {
        log.debug("Setting dataPacketService");
        this.dataPacketService = s;
    }

    public void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }

    public void setRouting(IRouting routing) {
        log.debug("Setting routing");
        this.routing = routing;
    }

    public void unsetRouting(IRouting routing) {
        if (this.routing == routing) {
            this.routing = null;
        }
    }

    public void setTopologyManager(ITopologyManager topologyManager) {
        log.debug("Setting topologyManager");
        this.topologyManager = topologyManager;
    }

    public void unsetTopologyManager(ITopologyManager topologyManager) {
        if (this.topologyManager == topologyManager) {
            this.topologyManager = null;
        }
    }

    public void setHostTracker(IfIptoHost hostTracker) {
        log.debug("Setting HostTracker");
        this.hostTracker = hostTracker;
    }

    public void setForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        log.debug("Setting ForwardingRulesManager");
        this.frm = forwardingRulesManager;
    }

    public void unsetHostTracker(IfIptoHost hostTracker) {
        if (this.hostTracker == hostTracker) {
            this.hostTracker = null;
        }
    }

    public void unsetForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        if (this.frm == forwardingRulesManager) {
            this.frm = null;
        }
    }

    /**
     * Function called when the bundle gets activated
     *
     */
    public void startUp() {
        allocateCaches();
        retrieveCaches();
        nonClusterObjectCreate();
    }

    public void nonClusterObjectCreate() {
        pendingPacketDestinations = new ConcurrentHashMap<InetAddress, PendingPacketData>();

        /* Pending Packets Ager Timer to go off every 6 seconds to implement pending packet aging */
        pendingPacketsAgerTimer = new Timer();
        pendingPacketsAgerTimer.schedule(new PendingPacketsAgerTimerHandler(), 6000, 6000);
    }

    /**
     * Function called when the bundle gets stopped
     *
     */
    public void shutDown() {
        log.debug("Destroy all the host Rules given we are shutting down");
        uninstallPerHostRules();
        destroyCaches();
    }

    private void allocateCaches() {
        if (this.clusterContainerService == null) {
            log.trace("un-initialized clusterContainerService, can't create cache");
            return;
        }

        try {
            clusterContainerService.createCache(FORWARDING_RULES_CACHE_NAME,
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } catch (CacheExistException cee) {
            log.error("\nCache already exists - destroy and recreate if needed");
        } catch (CacheConfigException cce) {
            log.error("\nCache configuration invalid - check cache mode");
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void retrieveCaches() {
        if (this.clusterContainerService == null) {
            log.trace("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }

        rulesDB = (ConcurrentMap<HostNodePair, HashMap<NodeConnector, FlowEntry>>) clusterContainerService
                .getCache(FORWARDING_RULES_CACHE_NAME);
        if (rulesDB == null) {
            log.error("\nFailed to get rulesDB handle");
        }
    }

    private void destroyCaches() {
        if (this.clusterContainerService == null) {
            log.trace("un-initialized clusterContainerService, can't destroy cache");
            return;
        }

        clusterContainerService.destroyCache(FORWARDING_RULES_CACHE_NAME);
    }

    /**
     * Populates <tt>rulesDB</tt> with rules specifying how to reach
     * <tt>host</tt> from <tt>currNode</tt> assuming that:
     * <ul>
     * <li><tt>host</tt> is attached to <tt>rootNode</tt>
     * <li><tt>link</tt> is the next part of the path to reach <tt>rootNode</tt>
     * from <tt>currNode</tt>
     * <li><tt>rulesDB.get(key)</tt> represents the list of rules stored about
     * <tt>host</tt> at <tt>currNode</tt>
     * </ul>
     *
     * @param host
     *            The host to be reached.
     * @param currNode
     *            The current node being processed.
     * @param rootNode
     *            The node to be reached. Really, the switch which host is
     *            attached to.
     * @param link
     *            The link to follow from curNode to get to rootNode
     * @param key
     *            The key to store computed rules at in the rulesDB. For now,
     *            this is a {@link HostNodePair} of host and currNode.
     */
    private void updatePerHostRuleInSW(HostNodeConnector host, Node currNode,
            Node rootNode, Edge link, HostNodePair key) {

        // only the link parameter is optional
        if (host == null || key == null || currNode == null || rootNode == null) {
            return;
        }

        Set<NodeConnector> ports = new HashSet<NodeConnector>();
        // add a special port of type ALL and port 0 to represent the node
        // without specifying a port on that node
        ports.add(NodeConnectorCreator.createNodeConnector(
                NodeConnectorIDType.ALL, NodeConnector.SPECIALNODECONNECTORID,
                currNode));

        HashMap<NodeConnector, FlowEntry> pos = this.rulesDB.get(key);
        if (pos == null) {
            pos = new HashMap<NodeConnector, FlowEntry>();
        }

        for (NodeConnector inPort : ports) {
            // skip the port connected to the target host
            if (currNode.equals(rootNode)
                    && (host.getnodeConnector().equals(inPort))) {
                continue;
            }

            // remove the current rule, if any
            FlowEntry removed_po = pos.remove(inPort);
            Match match = new Match();
            List<Action> actions = new ArrayList<Action>();

            // IP destination based forwarding on /32 entries only!
            match.setField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue());
            match.setField(MatchType.NW_DST, host.getNetworkAddress());

            /* Action for the policy is to forward to a port except on the
             * switch where the host sits, which is to rewrite also the MAC
             * and to forward on the Host port */
            NodeConnector outPort = null;

            if (currNode.equals(rootNode)) {
                /* If we're at the root node, then rewrite the DL addr and
                 * possibly pop the VLAN tag. This allows for MAC rewriting
                 * in the core of the network assuming we can uniquely ID
                 * packets based on IP address. */

                outPort = host.getnodeConnector();
                if (inPort.equals(outPort)) {
                    // TODO: isn't this code skipped already by the above continue?
                    // skip the host port
                    continue;
                }
                actions.add(new SetDlDst(host.getDataLayerAddressBytes()));

                if (!inPort.getType().equals(NodeConnectorIDType.ALL)) {
                    // Container mode: at the destination switch, we need to strip out the tag (VLAN)
                    actions.add(new PopVlan());
                }
            } else {
                // currNode is NOT the rootNode, find the next hop and create a rule
                if (link != null) {
                    outPort = link.getTailNodeConnector();
                    if (inPort.equals(outPort)) {
                        // skip the outgoing port
                        continue;
                    }

                    // If outPort is network link, add VLAN tag
                    if (topologyManager.isInternal(outPort)) {
                        log.debug("outPort {}/{} is internal uplink port",
                                currNode, outPort);
                    } else {
                        log.debug("outPort {}/{} is host facing port",
                                currNode, outPort);
                    }

                    if ((!inPort.getType().equals(NodeConnectorIDType.ALL))
                        && (topologyManager.isInternal(outPort))) {
                        Node nextNode = link.getHeadNodeConnector()
                                            .getNode();
                        // TODO: Replace this with SAL equivalent
                        //short tag = container.getTag((Long)nextNode.getNodeID());
                        short tag = 0;
                        if (tag != 0) {
                            log.debug("adding SET_VLAN {} for traffic " +
                                    "leaving {}/{} toward switch {}",
                                    new Object[] { tag, currNode, outPort,
                                    nextNode});
                            actions.add(new SetVlanId(tag));
                        } else {
                            log.debug("No tag assigned to switch {}", nextNode);
                        }
                    }
                }
            }
            if (outPort != null) {
                actions.add(new Output(outPort));
            }
            if (!inPort.getType().equals(NodeConnectorIDType.ALL)) {
                // include input port in the flow match field
                match.setField(MatchType.IN_PORT, inPort);

                if (topologyManager.isInternal(inPort)) {
                    log.debug("inPort {}/{} is internal uplink port", currNode,
                            inPort);
                } else {
                    log.debug("inPort {}/{} is host facing port", currNode,
                            inPort);
                }

                // for incoming network link; if the VLAN tag is defined, include it for incoming flow matching
                if (topologyManager.isInternal(inPort)) {
                    // TODO: Replace this with SAL equivalent
                    //short tag = container.getTag((Long)currNode.getNodeID());
                    short tag = 0;
                    if (tag != 0) {
                        log.debug("adding MATCH VLAN {} for traffic entering" +
                                "  {}/{}",
                                new Object[] {tag, currNode, inPort});
                        match.setField(MatchType.DL_VLAN, tag);
                    } else {
                        log.debug("No tag assigned to switch {}", currNode);
                    }
                }
            }
            // Make sure the priority for IP switch entries is
            // set to a level just above default drop entries
            Flow flow = new Flow(match, actions);
            flow.setIdleTimeout((short) 0);
            flow.setHardTimeout((short) 0);
            flow.setPriority(DEFAULT_IPSWITCH_PRIORITY);

            String policyName = host.getNetworkAddress().getHostAddress()
                    + "/32";
            String flowName = "["
                    + (!inPort.getType().equals(NodeConnectorIDType.ALL) ?
                       (inPort.getID()).toString()
                       + "," : "")
                    + host.getNetworkAddress().getHostAddress() + "/32 on N "
                    + currNode + "]";
            FlowEntry po = new FlowEntry(policyName, flowName, flow, currNode);

            /* Now save the rule in the DB rule, so on updates from topology we
             * can selectively */
            pos.put(inPort, po);
            this.rulesDB.put(key, pos);
            if (!inPort.getType().equals(NodeConnectorIDType.ALL)) {
                log.debug("Adding Match(inPort = {} , DIP = {})" +
                        " Action(outPort= {}) to node {}",
                        new Object[] { inPort,
                        host.getNetworkAddress().getHostAddress(),
                        outPort, currNode});
                if ((removed_po != null)
                        && (!po.getFlow().getMatch().equals(
                                removed_po.getFlow().getMatch()))) {
                    log.debug("Old Flow match: {}, New Flow match: {}",
                            removed_po.getFlow().getMatch(), po.getFlow()
                                    .getMatch());
                    addTobePrunedPolicy(currNode, removed_po, po);
                }

            } else {
                log.debug("Adding policyMatch(DIP = {}) Action(outPort= {}) " +
                        "to node {}", new Object[] {
                        host.getNetworkAddress().getHostAddress(), outPort,
                        currNode});
            }
        }
    }

    /**
     * Calculate the per-Host rules to be installed in the rulesDB,
     * and that will later on be installed in HW, this routine will
     * implicitly calculate the shortest path tree among the switch
     * to which the host is attached and all the other switches in the
     * network and will automatically create all the rules that allow
     * a /32 destination IP based forwarding, as in traditional IP
     * networks.
     *
     * @param host Host for which we are going to prepare the rules in the rulesDB
     *
     * @return A set of switches touched by the calculation
     */
    private Set<Node> preparePerHostRules(HostNodeConnector host) {
        if (host == null) {
            return null;
        }

        //TODO: race condition! unset* functions can make these null.
        if (this.routing == null) {
            return null;
        }
        if (this.switchManager == null) {
            return null;
        }
        if (this.rulesDB == null) {
            return null;
        }

        Node rootNode = host.getnodeconnectorNode();
        Set<Node> nodes = this.switchManager.getNodes();
        Set<Node> switchesToProgram = new HashSet<Node>();
        HostNodePair key;
        HashMap<NodeConnector, FlowEntry> pos;
        FlowEntry po;

        // for all nodes in the system
        for (Node node : nodes) {
            if (node.equals(rootNode)) {
                // We skip it because for the node with host attached
                // we will process in every case even if there are no
                // routes
                continue;
            }
            List<Edge> links;
            Path res = this.routing.getRoute(node, rootNode);
            if ((res == null) || ((links = res.getEdges()) == null)) {
                // No route from node to rootNode can be found, back out any
                // existing forwarding rules if they exist.
                log.debug("NO Route/Path between SW[{}] --> SW[{}] cleaning " +
                        "potentially existing entries", node, rootNode);
                key = new HostNodePair(host, node);
                pos = this.rulesDB.get(key);
                if (pos != null) {
                    for (Map.Entry<NodeConnector, FlowEntry> e : pos.entrySet()) {
                        po = e.getValue();
                        if (po != null) {
                            // uninstall any existing rules we put in the
                            // ForwardingRulesManager
                            this.frm.uninstallFlowEntry(po);
                        }
                    }
                    this.rulesDB.remove(key);
                }
                continue;
            }

            log.debug("Route between SW[{}] --> SW[{}]", node, rootNode);
            Node currNode = node;
            key = new HostNodePair(host, currNode);

            // for each link in the route from here to there
            for (Edge link : links) {
                if (link == null) {
                    log.error("Could not retrieve the Link");
                    // TODO: should we keep going?
                    continue;
                }

                log.debug(link.toString());

                // Index all the switches to be programmed
                updatePerHostRuleInSW(host, currNode, rootNode, link, key);
                if ((this.rulesDB.get(key)) != null) {
                    /* Calling updatePerHostRuleInSW() doesn't guarantee that
                     * rules will be added in currNode (e.g, there is only one
                     * link from currNode to rootNode This check makes sure that
                     * there are some rules in the rulesDB for the given key
                     * prior to adding switch to switchesToProgram
                     */
                    switchesToProgram.add(currNode);
                }
                currNode = link.getHeadNodeConnector().getNode();
                key = new HostNodePair(host, currNode);
            }
        }

        // This rule will be added no matter if any topology is built
        // or no, it serve as a way to handle the case of a node with
        // multiple hosts attached to it but not yet connected to the
        // rest of the world
        switchesToProgram.add(rootNode);
        updatePerHostRuleInSW(host, rootNode, rootNode, null,
                              new HostNodePair(host, rootNode));

        //      log.debug("Getting out at the end!");
        return switchesToProgram;
    }

    /**
     * Calculate the per-Host rules to be installed in the rulesDB
     * from a specific switch when a host facing port comes up.
     * These rules will later on be installed in HW. This routine
     * will implicitly calculate the shortest path from the switch
     * where the port has come up to the switch where host is ,
     * attached and will automatically create all the rules that allow
     * a /32 destination IP based forwarding, as in traditional IP
     * networks.
     *
     * @param host Host for which we are going to prepare the rules in the rulesDB
     * @param swId Switch ID where the port has come up
     *
     * @return A set of switches touched by the calculation
     */
    private Set<Node> preparePerHostPerSwitchRules(HostNodeConnector host,
            Node node, NodeConnector swport) {
        if ((host == null) || (node == null)) {
            return null;
        }
        if (this.routing == null) {
            return null;
        }
        if (this.switchManager == null) {
            return null;
        }
        if (this.rulesDB == null) {
            return null;
        }

        Node rootNode = host.getnodeconnectorNode();
        Set<Node> switchesToProgram = new HashSet<Node>();
        HostNodePair key;
        Map<NodeConnector, FlowEntry> pos;
        FlowEntry po;
        List<Edge> links;

        Path res = this.routing.getRoute(node, rootNode);
        if ((res == null) || ((links = res.getEdges()) == null)) {
            // the routing service doesn't know how to get there from here
            log.debug("NO Route/Path between SW[{}] --> SW[{}] cleaning " +
                    "potentially existing entries", node, rootNode);
            key = new HostNodePair(host, node);
            pos = this.rulesDB.get(key);
            if (pos != null) {
                for (Map.Entry<NodeConnector, FlowEntry> e : pos.entrySet()) {
                    po = e.getValue();
                    if (po != null) {
                        //Uninstall the policy
                        this.frm.uninstallFlowEntry(po);
                    }
                }
                this.rulesDB.remove(key);
            }
            return null;
        }

        log.debug("Route between SW[{}] --> SW[{}]", node, rootNode);
        Integer curr;
        Node currNode = node;
        key = new HostNodePair(host, currNode);
        Edge link;
        for (curr = 0; curr < links.size(); curr++) {
            link = links.get(curr);
            if (link == null) {
                log.error("Could not retrieve the Link");
                continue;
            }

            log.debug("Link [{}/{}] --> [{}/{}]", new Object[] {
                    currNode, link.getHeadNodeConnector(),
                    link.getHeadNodeConnector().getNode(),
                    link.getTailNodeConnector()});

            // Index all the switches to be programmed
            switchesToProgram.add(currNode);
            updatePerHostRuleInSW(host, currNode, rootNode, link, key);
            break; // come out of the loop for port up case, interested only in programming one switch
        }

        // This rule will be added no matter if any topology is built
        // or no, it serve as a way to handle the case of a node with
        // multiple hosts attached to it but not yet connected to the
        // rest of the world
        // switchesToProgram.add(rootNode);
        //updatePerHostRuleInSW(host, rootNode,
        //                                        rootNode, null,
        //                                        new HostNodePair(host, rootNode),ports);

        //      log.debug("Getting out at the end!");
        return switchesToProgram;
    }

    /**
     * Routine that fetch the per-Host rules from the rulesDB and
     * install in HW, the one having the same match rules will be
     * overwritten silently.
     *
     * @param host host for which we want to install in HW the per-Host rules
     * @param switchesToProgram list of switches to be programmed in
     * HW, usually are them all, but better to be explicit, that list
     * may change with time based on new switch addition/removal
     *
     * @return a return code that convey the programming status of the HW
     */
    private RulesProgrammingReturnCode installPerHostRules(
            HostNodeConnector host, Set<Node> switchesToProgram) {
        RulesProgrammingReturnCode retCode = RulesProgrammingReturnCode.SUCCESS;
        if (host == null || switchesToProgram == null) {
            return RulesProgrammingReturnCode.FAILED_WRONG_PARAMS;
        }
        Map<NodeConnector, FlowEntry> pos;
        FlowEntry po;
        // Now program every single switch
        log.debug("Inside installPerHostRules");
        for (Node swId : switchesToProgram) {
            HostNodePair key = new HostNodePair(host, swId);
            pos = this.rulesDB.get(key);
            if (pos == null) {
                continue;
            }
            for (Map.Entry<NodeConnector, FlowEntry> e : pos.entrySet()) {
                po = e.getValue();
                if (po != null) {
                    // Populate the Policy field now
                    Status poStatus = this.frm.modifyOrAddFlowEntry(po);
                    if (!poStatus.isSuccess()) {
                        log.error("Failed to install policy: "
                                + po.getGroupName() + " ("
                                + poStatus.getDescription() + ")");

                        retCode = RulesProgrammingReturnCode.FAILED_FEW_SWITCHES;
                        // Remove the entry from the DB, it was not installed!
                        this.rulesDB.remove(key);
                    } else {
                        log.debug("Successfully installed policy "
                                + po.toString() + " on switch " + swId);
                    }
                } else {
                    log.error("Cannot find a policy for SW:({}) Host: ({})",
                              swId, host);
                    /* // Now dump every single rule */
                    /* for (HostNodePair dumpkey : this.rulesDB.keySet()) { */
                    /*  po = this.rulesDB.get(dumpkey); */
                    /*  log.debug("Dumping entry H{" + dumpkey.getHost() + "} S{" + dumpkey.getSwitchId() + "} = {" + (po == null ? "null policy" : po)); */
                    /* } */
                }
            }
        }
        log.debug("Leaving installPerHostRules");
        return retCode;
    }

    /**
     * Cleanup all the host rules for a given host
     *
     * @param host Host for which the host rules need to be cleaned
     * up, the host could be null in that case it match all the hosts
     *
     * @return a return code that convey the programming status of the HW
     */
    private RulesProgrammingReturnCode uninstallPerHostRules(HostNodeConnector host) {
        RulesProgrammingReturnCode retCode = RulesProgrammingReturnCode.SUCCESS;
        Map<NodeConnector, FlowEntry> pos;
        FlowEntry po;
        // Now program every single switch
        for (HostNodePair key : this.rulesDB.keySet()) {
            if (host == null || key.getHost().equals(host)) {
                pos = this.rulesDB.get(key);
                for (Map.Entry<NodeConnector, FlowEntry> e : pos.entrySet()) {
                    po = e.getValue();
                    if (po != null) {
                        // Uninstall the policy
                        this.frm.uninstallFlowEntry(po);
                    }
                }
                this.rulesDB.remove(key);
            }
        }
        return retCode;
    }

    /**
     * Cleanup all the host rules for a given node, triggered when the
     * switch disconnects, so there is no reason for Hw cleanup
     * because it's disconnected anyhow
     * TBD - Revisit above stmt in light of CSCus88743
     * @param targetNode Node for which we want to do cleanup
     *
     */
    private void uninstallPerNodeRules(Node targetNode) {
        //RulesProgrammingReturnCode retCode = RulesProgrammingReturnCode.SUCCESS;
        Map<NodeConnector, FlowEntry> pos;
        FlowEntry po;

        // Now program every single switch
        for (HostNodePair key : this.rulesDB.keySet()) {
            Node node = key.getNode();
            if (targetNode == null || node.equals(targetNode)) {
                log.debug("Work on {} host {}", node, key.getHost());
                pos = this.rulesDB.get(key);
                for (Map.Entry<NodeConnector, FlowEntry> e : pos.entrySet()) {
                    po = e.getValue();
                    if (po != null) {
                        // Uninstall the policy
                        this.frm.uninstallFlowEntry(po);
                    }
                }
                log.debug("Remove {}", key);
                this.rulesDB.remove(key);
            }
        }
    }

    /**
     * Cleanup all the host rules currently present in the rulesDB
     *
     * @return a return code that convey the programming status of the HW
     */
    private RulesProgrammingReturnCode uninstallPerHostRules() {
        return uninstallPerHostRules(null);
    }

    @Override
    public void recalculateDone() {
        if (this.hostTracker == null) {
            //Not yet ready to process all the updates
            //TODO: we should make sure that this call is executed eventually
            return;
        }
        Set<HostNodeConnector> allHosts = this.hostTracker.getAllHosts();
        for (HostNodeConnector host : allHosts) {
            Set<Node> switches = preparePerHostRules(host);
            if (switches != null) {
                // This will refresh existing rules, by overwriting
                // the previous ones
                installPerHostRules(host, switches);
                pruneExcessRules(switches);
            }
        }
    }

    void addTobePrunedPolicy(Node swId, FlowEntry po, FlowEntry new_po) {
        List<FlowEntry> pl = tobePrunedPos.get(swId);
        if (pl == null) {
            pl = new LinkedList<FlowEntry>();
            tobePrunedPos.put(swId, pl);
        }
        pl.add(po);
        log.debug("Adding Pruned Policy for SwId: {}", swId);
        log.debug("Old Policy: {}", po);
        log.debug("New Policy: {}", new_po);
    }

    private void pruneExcessRules(Set<Node> switches) {
        for (Node swId : switches) {
            List<FlowEntry> pl = tobePrunedPos.get(swId);
            if (pl != null) {
                log.debug("Policies for Switch: {} in the list to be deleted: {}", swId, pl);
                Iterator<FlowEntry> plIter = pl.iterator();
                //for (Policy po: pl) {
                while (plIter.hasNext()) {
                    FlowEntry po = plIter.next();
                    log.error("Removing Policy, Switch: {} Policy: {}", swId, po);
                    this.frm.uninstallFlowEntry(po);
                    plIter.remove();
                }
            }
            // tobePrunedPos.remove(swId);
        }
    }

    /**
     * A Host facing port has come up in a container. Add rules on the switch where this
     * port has come up for all the known hosts to the controller.
     * @param swId switch id of the port where port came up
     * @param swPort port which came up
     */
    private void updateRulesforHIFup(Node node, NodeConnector swPort) {
        if (this.hostTracker == null) {
            //Not yet ready to process all the updates
            return;
        }
        log.debug("Host Facing Port in a container came up, install the rules for all hosts from this port !");
        Set<HostNodeConnector> allHosts = this.hostTracker.getAllHosts();
        for (HostNodeConnector host : allHosts) {
            if (node.equals(host.getnodeconnectorNode())) {
                /*
                 * This host resides behind the same switch and port for which a port up
                 * message is received. Ideally this should not happen, but if it does,
                 * don't program any rules for this host
                 */
                continue;
            }
            Set<Node> switches = preparePerHostPerSwitchRules(host, node,
                    swPort);
            if (switches != null) {
                // This will refresh existing rules, by overwriting
                // the previous ones
                installPerHostRules(host, switches);
            }
        }

    }

    @Override
    public void notifyHTClient(HostNodeConnector host) {
        if (host == null) {
            return;
        }
        Set<Node> switches = preparePerHostRules(host);
        if (switches != null) {
            installPerHostRules(host, switches);

            // Green light for sending pending packet to this host. Safe to call if there are none.
            sendPendingPacket(host.getNetworkAddress());
        }
    }

    @Override
    public void notifyHTClientHostRemoved(HostNodeConnector host) {
        if (host == null) {
            return;
        }
        uninstallPerHostRules(host);
    }

    @Override
    public void notifyNode(Node node, UpdateType type,
            Map<String, Property> propMap) {
        if (node == null) {
            return;
        }

        switch (type) {
        case REMOVED:
            log.debug("Node {} gone, doing a cleanup", node);
            uninstallPerNodeRules(node);
            break;
        default:
            break;
        }
    }

    @Override
    public void notifyNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Map<String, Property> propMap) {
        if (nodeConnector == null) {
            return;
        }

        boolean up = false;
        switch (type) {
        case ADDED:
            up = true;
            break;
        case REMOVED:
            break;
        case CHANGED:
            State state = (State) propMap.get(State.StatePropName);
            if ((state != null) && (state.getValue() == State.EDGE_UP)) {
                up = true;
            }
            break;
        default:
            return;
        }

        if (up) {
            handleNodeConnectorStatusUp(nodeConnector);
        } else {
            handleNodeConnectorStatusDown(nodeConnector);
        }
    }

    private void handleNodeConnectorStatusUp(NodeConnector nodeConnector) {
        if (topologyManager == null) {
            log.debug("topologyManager is not set yet");
            return;
        }

        if (topologyManager.isInternal(nodeConnector)) {
            log.debug("{} is not a host facing link", nodeConnector);
            return;
        }

        log.debug("{} is up", nodeConnector);
        updateRulesforHIFup(nodeConnector.getNode(), nodeConnector);
    }

    private void handleNodeConnectorStatusDown(NodeConnector nodeConnector) {
        log.debug("{} is down", nodeConnector);
    }

    void setClusterContainerService(IClusterContainerServices s) {
        log.debug("Cluster Service set");
        this.clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (this.clusterContainerService == s) {
            log.debug("Cluster Service removed!");
            this.clusterContainerService = null;
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        startUp();
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
        pendingPacketsAgerTimer.cancel();
        pendingPacketDestinations.clear();
    }

    public void setSwitchManager(ISwitchManager switchManager) {
        this.switchManager = switchManager;
    }

    public void unsetSwitchManager(ISwitchManager switchManager) {
        if (this.switchManager == switchManager) {
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
                log.trace("Handle punted IP packet: {}", formattedPak);
                handlePuntedIPPacket((IPv4) nextPak, inPkt.getIncomingNodeConnector(), true);
            }
        }
        return PacketResult.IGNORED;

    }

    private void handlePuntedIPPacket(IPv4 pkt, NodeConnector incomingNodeConnector, boolean allowAddPending) {
        InetAddress dIP = NetUtils.getInetAddress(pkt.getDestinationAddress());
        if (dIP == null || hostTracker == null) {
            log.debug("Invalid param(s) in handlePuntedIPPacket.. DestIP: {}. hostTracker: {}", dIP, hostTracker);
            return;
        }
        HostNodeConnector destHost = hostTracker.hostFind(dIP);
        /*
         * In cases when incoming and outgoing connectors are in the same node, there is no need
         * to verify that there is a route. Because of that, we will only need routing.getRoute()
         * if we know that src and dst nodes are different.
         */
        if (destHost != null
                && (incomingNodeConnector.getNode().equals(destHost.getnodeconnectorNode()) ||
                    routing == null ||
                    routing.getRoute(incomingNodeConnector.getNode(), destHost.getnodeconnectorNode()) != null)) {

            log.trace("Host {} is at {}", dIP, destHost.getnodeConnector());

            // If SimpleForwarding is aware of this host, it will try to install
            // a path. Forward packet until it's done.
            if (dataPacketService != null) {

                /*
                 * if we know where the host is and there's a path from where this
                 * packet was punted to where the host is, then attempt best effort delivery to the host
                 */
                NodeConnector nc = destHost.getnodeConnector();
                log.trace("Forwarding punted IP received at {} to {}", incomingNodeConnector, nc);
                // re-encode the Ethernet packet (the parent of the IPv4 packet)
                RawPacket rp = this.dataPacketService.encodeDataPacket(pkt.getParent());
                rp.setOutgoingNodeConnector(nc);
                this.dataPacketService.transmitDataPacket(rp);
            }
        } else if (allowAddPending) {
            // If we made it here, let's hang on to the punted packet, with hopes that its destination
            // will become available soon.
            addToPendingPackets(dIP, pkt, incomingNodeConnector);
        } else {
            log.warn("Dropping punted IP packet received at {} to Host {}", incomingNodeConnector, dIP);
        }
    }
}
