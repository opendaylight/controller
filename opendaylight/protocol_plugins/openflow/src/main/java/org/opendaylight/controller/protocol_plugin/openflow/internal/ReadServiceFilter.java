
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.protocol_plugin.openflow.IOFStatisticsListener;
import org.opendaylight.controller.protocol_plugin.openflow.IOFStatisticsManager;
import org.opendaylight.controller.protocol_plugin.openflow.IReadFilterInternalListener;
import org.opendaylight.controller.protocol_plugin.openflow.IReadServiceFilter;
import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.ActionType;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.connection.IPluginOutConnectionService;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.NodeTableCreator;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.protocol.statistics.OFTableStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Read Service shim layer which is in charge of filtering the flow statistics
 * based on container. It is a Global instance.
 */
public class ReadServiceFilter implements IReadServiceFilter, IContainerListener, IOFStatisticsListener {
    private static final Logger logger = LoggerFactory
            .getLogger(ReadServiceFilter.class);
    private IController controller = null;
    private IOFStatisticsManager statsMgr = null;
    private ConcurrentMap<String, Set<NodeConnector>> containerToNc;
    private ConcurrentMap<String, Set<Node>> containerToNode;
    private ConcurrentMap<String, Set<NodeTable>> containerToNt;
    private ConcurrentMap<String, Set<ContainerFlow>> containerFlows;
    private ConcurrentMap<String, IReadFilterInternalListener> readFilterInternalListeners;

    public void setController(IController core) {
        this.controller = core;
    }

    public void unsetController(IController core) {
        if (this.controller == core) {
            this.controller = null;
        }
    }

    public void setReadFilterInternalListener(Map<?, ?> props, IReadFilterInternalListener s) {
        if (props == null) {
            logger.error("Failed setting Read Filter Listener, property map is null.");
            return;
        }
        String containerName = (String) props.get("containerName");
        if (containerName == null) {
            logger.error("Failed setting Read Filter Listener, container name not supplied.");
            return;
        }
        if ((this.readFilterInternalListeners != null) && !this.readFilterInternalListeners.containsValue(s)) {
            this.readFilterInternalListeners.put(containerName, s);
            logger.trace("Added Read Filter Listener for container {}", containerName);
        }
    }

    public void unsetReadFilterInternalListener(Map<?, ?> props, IReadFilterInternalListener s) {
        if (props == null) {
            logger.error("Failed unsetting Read Filter Listener, property map is null.");
            return;
        }
        String containerName = (String) props.get("containerName");
        if (containerName == null) {
            logger.error("Failed unsetting Read Filter Listener, containerName not supplied");
            return;
        }
        if ((this.readFilterInternalListeners != null) && this.readFilterInternalListeners.get(containerName) != null
                && this.readFilterInternalListeners.get(containerName).equals(s)) {
            this.readFilterInternalListeners.remove(containerName);
            logger.trace("Removed Read Filter Listener for container {}", containerName);
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        containerToNc = new ConcurrentHashMap<String, Set<NodeConnector>>();
        containerToNt = new ConcurrentHashMap<String, Set<NodeTable>>();
        containerToNode = new ConcurrentHashMap<String, Set<Node>>();
        containerFlows = new ConcurrentHashMap<String, Set<ContainerFlow>>();
        readFilterInternalListeners = new ConcurrentHashMap<String, IReadFilterInternalListener>();
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

    public void setService(IOFStatisticsManager service) {
        this.statsMgr = service;
    }

    public void unsetService(IOFStatisticsManager service) {
        this.statsMgr = null;
    }

    IPluginOutConnectionService connectionPluginOutService;
    void setIPluginOutConnectionService(IPluginOutConnectionService s) {
        connectionPluginOutService = s;
    }

    void unsetIPluginOutConnectionService(IPluginOutConnectionService s) {
        if (connectionPluginOutService == s) {
            connectionPluginOutService = null;
        }
    }

    @Override
    public FlowOnNode readFlow(String container, Node node, Flow flow, boolean cached) {

        if (controller == null) {
            // Avoid to provide cached statistics if controller went down.
            // They are not valid anymore anyway
            logger.error("Internal plugin error");
            return null;
        }

        long sid = (Long) node.getID();
        OFMatch ofMatch = new FlowConverter(flow).getOFMatch();
        List<OFStatistics> ofList;
        if (cached == true){
            ofList = statsMgr.getOFFlowStatistics(sid, ofMatch, flow.getPriority());
        } else {
            ofList = statsMgr.queryStatistics(sid, OFStatisticsType.FLOW, ofMatch);
            for (OFStatistics ofStat : ofList) {
                if (((OFFlowStatisticsReply)ofStat).getPriority() == flow.getPriority()){
                    ofList = new ArrayList<OFStatistics>(1);
                    ofList.add(ofStat);
                    break;
                }
            }
        }

        // Convert and filter the statistics per container
        List<FlowOnNode> flowOnNodeList = new FlowStatisticsConverter(ofList).getFlowOnNodeList(node);
        List<FlowOnNode> filteredList = filterFlowListPerContainer(container, node, flowOnNodeList);

        return (filteredList == null || filteredList.isEmpty()) ? null : filteredList.get(0);
    }

    @Override
    public List<FlowOnNode> readAllFlow(String container, Node node,
            boolean cached) {

        long sid = (Long) node.getID();
        List<OFStatistics> ofList = (cached == true) ? statsMgr
                .getOFFlowStatistics(sid) : statsMgr.queryStatistics(sid,
                OFStatisticsType.FLOW, null);

        // Convert and filter the statistics per container
        List<FlowOnNode> flowOnNodeList = new FlowStatisticsConverter(ofList).getFlowOnNodeList(node);
        List<FlowOnNode> filteredList = filterFlowListPerContainer(container, node, flowOnNodeList);

        return (filteredList == null) ? null : filteredList;

    }

    @Override
    public NodeDescription readDescription(Node node, boolean cached) {

        if (controller == null) {
            logger.error("Internal plugin error");
            return null;
        }

        long sid = (Long) node.getID();
        List<OFStatistics> ofList = (cached == true) ? statsMgr
                .getOFDescStatistics(sid) : statsMgr.queryStatistics(sid,
                        OFStatisticsType.DESC, null);

        return new DescStatisticsConverter(ofList).getHwDescription();
    }

    /**
     * Filters a list of FlowOnNode elements based on the container
     *
     * @param container
     * @param nodeId
     * @param list
     * @return
     */
    public List<FlowOnNode> filterFlowListPerContainer(String container,
            Node nodeId, List<FlowOnNode> list) {
        if (list == null) {
            return null;
        }

        // Create new filtered list of flows
        List<FlowOnNode> newList = new ArrayList<FlowOnNode>();

        for (FlowOnNode target : list) {
            // Check whether the described flow (match + actions) belongs to this container
            if (flowBelongToContainer(container, nodeId, target.getFlow())) {
                newList.add(target);
            }
        }

        return newList;
    }

    /**
     * Filters a list of OFStatistics elements based on the container
     *
     * @param container
     * @param nodeId
     * @param list
     * @return
     */
    public List<OFStatistics> filterPortListPerContainer(String container, long switchId, List<OFStatistics> list) {
        if (list == null) {
            return null;
        }

        // Create new filtered list of flows
        List<OFStatistics> newList = new ArrayList<OFStatistics>();

        for (OFStatistics stat : list) {
            OFPortStatisticsReply target = (OFPortStatisticsReply) stat;
            NodeConnector nc = NodeConnectorCreator.createOFNodeConnector(
                    target.getPortNumber(), NodeCreator.createOFNode(switchId));
            if (containerOwnsNodeConnector(container, nc)) {
                newList.add(target);
            }
        }

        return newList;
    }


    public List<OFStatistics> filterTableListPerContainer(
            String container, long switchId, List<OFStatistics> list) {
        if (list == null) {
            return null;
        }

        // Create new filtered list of node tables
        List<OFStatistics> newList = new ArrayList<OFStatistics>();

        for (OFStatistics stat : list) {
            OFTableStatistics target = (OFTableStatistics) stat;
            NodeTable nt = NodeTableCreator.createOFNodeTable(target.getTableId(), NodeCreator.createOFNode(switchId));
            if (containerOwnsNodeTable(container, nt)) {
                newList.add(target);
            }
        }

        return newList;
    }

    /**
     * Returns whether the specified flow (flow match + actions)
     * belongs to the container
     *
     * @param container
     * @param node
     * @param flow
     * @return true if it belongs
     */
    public boolean flowBelongToContainer(String container, Node node, Flow flow) {
        // All flows belong to the default container
        if (container.equals(GlobalConstants.DEFAULT.toString())) {
            return true;
        }
        return (flowPortsBelongToContainer(container, node, flow) &&
                flowVlanBelongsToContainer(container, node, flow) &&
                isFlowAllowedByContainer(container, flow));
    }

    /**
     * Returns whether the passed NodeConnector belongs to the container
     *
     * @param container container name
     * @param p     node connector to test
     * @return          true if belongs false otherwise
     */
    public boolean containerOwnsNodeConnector(String container, NodeConnector p) {
        // All node connectors belong to the default container
        if (container.equals(GlobalConstants.DEFAULT.toString())) {
            return true;
        }
        Set<NodeConnector> portSet = containerToNc.get(container);
        return (portSet == null) ? false : portSet.contains(p);
    }

    /**
     * Returns whether the passed NodeConnector belongs to the container
     *
     * @param container container name
     * @param table     node table to test
     * @return          true if belongs false otherwise
     */
    public boolean containerOwnsNodeTable(String container, NodeTable table) {
        // All node table belong to the default container
        if (container.equals(GlobalConstants.DEFAULT.toString())) {
            return true;
        }
        Set<NodeTable> tableSet = containerToNt.get(container);
        return (tableSet == null) ? false : tableSet.contains(table);
    }

    /**
     * Returns whether the container flows allow the passed flow
     *
     * @param container
     * @param match
     * @return
     */
    private boolean isFlowAllowedByContainer(String container, Flow flow) {
        Set<ContainerFlow> cFlowSet = this.containerFlows.get(container);
        if (cFlowSet == null || cFlowSet.isEmpty()) {
            return true;
        }
        for (ContainerFlow cFlow : cFlowSet) {
            if (cFlow.allowsFlow(flow)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the vlan field in the flow match is the same
     * of the static vlan configured for the container
     *
     * @param container
     * @param node
     * @param flow
     * @return
     */
    private boolean flowVlanBelongsToContainer(String container, Node node, Flow flow) {
        return true; // Always true for now
    }

    /**
     * Check whether the ports in the flow match and flow actions for
     * the specified node belong to the container
     *
     * @param container
     * @param node
     * @param flow
     * @return
     */
    private boolean flowPortsBelongToContainer(String container, Node node,
            Flow flow) {
        Match m = flow.getMatch();
        if (m.isPresent(MatchType.IN_PORT)) {
            NodeConnector inPort = (NodeConnector) m.getField(MatchType.IN_PORT).getValue();
            // If the incoming port is specified, check if it belongs to
            if (!containerOwnsNodeConnector(container, inPort)) {
                return false;
            }
        }

        // If an outgoing port is specified, it must belong to this container
        for (Action action : flow.getActions()) {
            if (action.getType() == ActionType.OUTPUT) {
                NodeConnector outPort = ((Output) action).getPort();
                if (!containerOwnsNodeConnector(container, outPort)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void containerFlowUpdated(String containerName, ContainerFlow previousFlow,
            ContainerFlow currentFlow, UpdateType t) {
        Set<ContainerFlow> cFlowSet = containerFlows.get(containerName);
        switch (t) {
        case ADDED:
            if (cFlowSet == null) {
                cFlowSet = new HashSet<ContainerFlow>();
                containerFlows.put(containerName, cFlowSet);
            }
            cFlowSet.add(currentFlow);
        case CHANGED:
            break;
        case REMOVED:
            if (cFlowSet != null) {
                cFlowSet.remove(currentFlow);
            }
            break;
        default:
            break;
        }
    }

    @Override
    public void nodeConnectorUpdated(String containerName, NodeConnector p, UpdateType type) {

        switch (type) {
        case ADDED:
            if (!containerToNc.containsKey(containerName)) {
                containerToNc.put(containerName,
                    Collections.newSetFromMap(new ConcurrentHashMap<NodeConnector,Boolean>()));
            }
            containerToNc.get(containerName).add(p);
            if (!containerToNode.containsKey(containerName)) {
                containerToNode.put(containerName, new HashSet<Node>());
            }
            containerToNode.get(containerName).add(p.getNode());
            break;
        case REMOVED:
            Set<NodeConnector> ncSet = containerToNc.get(containerName);
            if (ncSet != null) {
                //remove this nc from container map
                ncSet.remove(p);

                //check if there are still ports of this node in this container
                //and if not, remove its mapping
                boolean nodeInContainer = false;
                Node node = p.getNode();
                for (NodeConnector nodeConnector : ncSet) {
                    if (nodeConnector.getNode().equals(node)){
                        nodeInContainer = true;
                        break;
                    }
                }
                if (! nodeInContainer) {
                    Set<Node> nodeSet = containerToNode.get(containerName);
                    if (nodeSet != null) {
                        nodeSet.remove(node);
                    }
                }
            }
            break;
        case CHANGED:
        default:
        }
    }

    @Override
    public void tagUpdated(String containerName, Node n, short oldTag, short newTag, UpdateType t) {
        // Not interested in this event
    }

    @Override
    public void containerModeUpdated(UpdateType t) {
        // Not interested in this event
    }

    @Override
    public NodeConnectorStatistics readNodeConnector(String containerName, NodeConnector connector, boolean cached) {
        if (!containerOwnsNodeConnector(containerName, connector)) {
            return null;
        }
        Node node = connector.getNode();
        long sid = (Long) node.getID();
        short portId = (Short) connector.getID();
        List<OFStatistics> ofList = (cached == true) ? statsMgr
                .getOFPortStatistics(sid, portId) : statsMgr.queryStatistics(
                        sid, OFStatisticsType.PORT, portId);

        List<NodeConnectorStatistics> ncStatistics = new PortStatisticsConverter(sid, ofList)
                .getNodeConnectorStatsList();
        return (ncStatistics.isEmpty()) ? new NodeConnectorStatistics() : ncStatistics.get(0);
    }

    @Override
    public List<NodeConnectorStatistics> readAllNodeConnector(String containerName, Node node, boolean cached) {

        long sid = (Long) node.getID();
        List<OFStatistics> ofList = (cached == true) ? statsMgr
                .getOFPortStatistics(sid) : statsMgr.queryStatistics(sid,
                        OFStatisticsType.FLOW, null);

        List<OFStatistics> filteredList = filterPortListPerContainer(containerName, sid, ofList);

        return new PortStatisticsConverter(sid, filteredList).getNodeConnectorStatsList();
    }

    @Override
    public long getTransmitRate(String containerName, NodeConnector connector) {
        if (!containerOwnsNodeConnector(containerName, connector)) {
            return 0;
        }

        long switchId = (Long) connector.getNode().getID();
        short port = (Short) connector.getID();

        return statsMgr.getTransmitRate(switchId, port);
    }

    @Override
    public NodeTableStatistics readNodeTable(String containerName,
            NodeTable table, boolean cached) {
        if (!containerOwnsNodeTable(containerName, table)) {
            return null;
        }
        Node node = table.getNode();
        long sid = (Long) node.getID();
        Byte tableId = (Byte) table.getID();
        List<OFStatistics> ofList = (cached == true) ? statsMgr.getOFTableStatistics(sid, tableId) :
            statsMgr.queryStatistics(sid, OFStatisticsType.TABLE, tableId);

        List<NodeTableStatistics> ntStatistics = new TableStatisticsConverter(sid, ofList).getNodeTableStatsList();

        return (ntStatistics.isEmpty()) ? new NodeTableStatistics() : ntStatistics.get(0);
    }

    @Override
    public List<NodeTableStatistics> readAllNodeTable(String containerName, Node node, boolean cached) {
        long sid = (Long) node.getID();
        List<OFStatistics> ofList = (cached == true) ?
                statsMgr.getOFTableStatistics(sid) : statsMgr.queryStatistics(sid, OFStatisticsType.FLOW, null);

        List<OFStatistics> filteredList = filterTableListPerContainer(containerName, sid, ofList);

        return new TableStatisticsConverter(sid, filteredList).getNodeTableStatsList();
    }

    @Override
    public void descriptionStatisticsRefreshed(Long switchId, List<OFStatistics> description) {
        String container;
        IReadFilterInternalListener listener;
        Node node = NodeCreator.createOFNode(switchId);
        NodeDescription nodeDescription = new DescStatisticsConverter(description).getHwDescription();
        for (Map.Entry<String, IReadFilterInternalListener> l : readFilterInternalListeners.entrySet()) {
            container = l.getKey();
            listener = l.getValue();
            if (container == GlobalConstants.DEFAULT.toString()
                    || (containerToNode.containsKey(container) && containerToNode.get(container).contains(node))) {
                listener.nodeDescriptionStatisticsUpdated(node, nodeDescription);
            }
        }
    }

    @Override
    public void flowStatisticsRefreshed(Long switchId, List<OFStatistics> flows) {
        String container;
        IReadFilterInternalListener listener;
        Node node = NodeCreator.createOFNode(switchId);
        for (Map.Entry<String, IReadFilterInternalListener> l : readFilterInternalListeners.entrySet()) {
            container = l.getKey();
            listener = l.getValue();

            // Convert and filter the statistics per container
            List<FlowOnNode> flowOnNodeList = new FlowStatisticsConverter(flows).getFlowOnNodeList(node);
            flowOnNodeList = filterFlowListPerContainer(container, node, flowOnNodeList);

            // notify listeners
            listener.nodeFlowStatisticsUpdated(node, flowOnNodeList);
        }
    }

    @Override
    public void portStatisticsRefreshed(Long switchId, List<OFStatistics> ports) {
        String container;
        IReadFilterInternalListener listener;
        Node node = NodeCreator.createOFNode(switchId);
        for (Map.Entry<String, IReadFilterInternalListener> l : readFilterInternalListeners.entrySet()) {
            container = l.getKey();
            listener = l.getValue();

            // Convert and filter the statistics per container
            List<OFStatistics> filteredPorts = filterPortListPerContainer(container, switchId, ports);
            List<NodeConnectorStatistics> ncStatsList = new PortStatisticsConverter(switchId, filteredPorts)
                    .getNodeConnectorStatsList();

            // notify listeners
            listener.nodeConnectorStatisticsUpdated(node, ncStatsList);
        }
    }

    @Override
    public void tableStatisticsRefreshed(Long switchId, List<OFStatistics> tables) {
        String container;
        Node node = NodeCreator.createOFNode(switchId);
        for (Map.Entry<String, IReadFilterInternalListener> l : readFilterInternalListeners.entrySet()) {
            container = l.getKey();

            // Convert and filter the statistics per container
            List<OFStatistics> filteredList = filterTableListPerContainer(container, switchId, tables);
            List<NodeTableStatistics> tableStatsList = new TableStatisticsConverter(switchId, filteredList)
                    .getNodeTableStatsList();

            // notify listeners
            l.getValue().nodeTableStatisticsUpdated(node, tableStatsList);
        }
    }
}
