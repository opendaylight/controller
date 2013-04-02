
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.protocol_plugin.openflow.IOFStatisticsManager;
import org.opendaylight.controller.protocol_plugin.openflow.IPluginReadServiceFilter;
import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.ActionType;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;

/**
 * Read Service shim layer which is in charge of filtering the flow statistics
 * based on container. It is a Global instance.
 *
 *
 *
 */
public class ReadServiceFilter implements IPluginReadServiceFilter,
        IContainerListener {
    private static final Logger logger = LoggerFactory
            .getLogger(ReadServiceFilter.class);
    private IController controller = null;
    private IOFStatisticsManager statsMgr = null;
    private Map<String, Set<NodeConnector>> containerToNc;

    public void setController(IController core) {
        this.controller = core;
    }

    public void unsetController(IController core) {
        if (this.controller == core) {
            this.controller = null;
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        containerToNc = new HashMap<String, Set<NodeConnector>>();
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

    @Override
    public FlowOnNode readFlow(String container, Node node, Flow flow,
            boolean cached) {

        if (controller == null) {
            // Avoid to provide cached statistics if controller went down.
            // They are not valid anymore anyway
            logger.error("Internal plugin error");
            return null;
        }

        long sid = (Long) node.getID();
        OFMatch ofMatch = new FlowConverter(flow).getOFMatch();
        List<OFStatistics> ofList = (cached == true) ? statsMgr
                .getOFFlowStatistics(sid, ofMatch) : statsMgr.queryStatistics(
                sid, OFStatisticsType.FLOW, ofMatch);

        /*
         * Convert and filter the statistics per container
         */
        List<FlowOnNode> flowOnNodeList = new FlowStatisticsConverter(ofList)
                .getFlowOnNodeList(node);
        List<FlowOnNode> filteredList = filterFlowListPerContainer(container,
                node, flowOnNodeList);

        return (filteredList == null || filteredList.isEmpty()) ? null
                : filteredList.get(0);
    }

    @Override
    public List<FlowOnNode> readAllFlow(String container, Node node,
            boolean cached) {

        long sid = (Long) node.getID();
        List<OFStatistics> ofList = (cached == true) ? statsMgr
                .getOFFlowStatistics(sid) : statsMgr.queryStatistics(sid,
                OFStatisticsType.FLOW, null);

        /*
         * Convert and filter the statistics per container
         */
        List<FlowOnNode> flowOnNodeList = new FlowStatisticsConverter(ofList)
                .getFlowOnNodeList(node);
        List<FlowOnNode> filteredList = filterFlowListPerContainer(container,
                node, flowOnNodeList);

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
     * Filters a list of FlowOnNode elements based on the container
     *
     * @param container
     * @param nodeId
     * @param list
     * @return
     */
    public List<OFStatistics> filterPortListPerContainer(String container,
            long switchId, List<OFStatistics> list) {
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
        return (flowPortsBelongToContainer(container, node, flow)
                && flowVlanBelongsToContainer(container, node, flow) && flowSpecAllowsFlow(
                container, flow.getMatch()));
    }

    /**
     * Returns whether the passed NodeConnector belongs to the container
     *
     * @param container	container name
     * @param p		node connector to test
     * @return 		true if belongs false otherwise
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
     * Returns whether the container flowspec allows the passed flow
     *
     * @param container
     * @param match
     * @return
     */
    private boolean flowSpecAllowsFlow(String container, Match match) {
        return true; // Always true for now
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
    private boolean flowVlanBelongsToContainer(String container, Node node,
            Flow flow) {
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
            NodeConnector inPort = (NodeConnector) m
                    .getField(MatchType.IN_PORT).getValue();

            // If the incoming port is specified, check if it belongs to
            if (!containerOwnsNodeConnector(container, inPort)) {
                return false;
            }
        }

        // If an outgoing port is specified, it must belong to this container
        for (Action action : flow.getActions()) {
            if (action.getType() == ActionType.OUTPUT) {
                NodeConnector outPort = (NodeConnector) ((Output) action)
                        .getPort();
                if (!containerOwnsNodeConnector(container, outPort)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void containerFlowUpdated(String containerName,
            ContainerFlow previousFlow, ContainerFlow currentFlow, UpdateType t) {

    }

    @Override
    public void nodeConnectorUpdated(String containerName, NodeConnector p,
            UpdateType type) {
        Set<NodeConnector> target = null;

        switch (type) {
        case ADDED:
            if (!containerToNc.containsKey(containerName)) {
                containerToNc.put(containerName, new HashSet<NodeConnector>());
            }
            containerToNc.get(containerName).add(p);
            break;
        case CHANGED:
            break;
        case REMOVED:
            target = containerToNc.get(containerName);
            if (target != null) {
                target.remove(p);
            }
            break;
        default:
        }
    }

    @Override
    public void tagUpdated(String containerName, Node n, short oldTag,
            short newTag, UpdateType t) {
        // Not interested in this event
    }

    @Override
    public void containerModeUpdated(UpdateType t) {
        // do nothing
    }

    @Override
    public NodeConnectorStatistics readNodeConnector(String containerName,
            NodeConnector connector, boolean cached) {
        if (!containerOwnsNodeConnector(containerName, connector)) {
            return null;
        }
        Node node = connector.getNode();
        long sid = (Long) node.getID();
        short portId = (Short) connector.getID();
        List<OFStatistics> ofList = (cached == true) ? statsMgr
                .getOFPortStatistics(sid, portId) : statsMgr.queryStatistics(
                sid, OFStatisticsType.PORT, portId);

        List<NodeConnectorStatistics> ncStatistics = new PortStatisticsConverter(
                sid, ofList).getNodeConnectorStatsList();
        return (ncStatistics.isEmpty()) ? new NodeConnectorStatistics()
                : ncStatistics.get(0);
    }

    @Override
    public List<NodeConnectorStatistics> readAllNodeConnector(
            String containerName, Node node, boolean cached) {

        long sid = (Long) node.getID();
        List<OFStatistics> ofList = (cached == true) ? statsMgr
                .getOFPortStatistics(sid) : statsMgr.queryStatistics(sid,
                OFStatisticsType.FLOW, null);

        List<OFStatistics> filteredList = filterPortListPerContainer(
                containerName, sid, ofList);

        return new PortStatisticsConverter(sid, filteredList)
                .getNodeConnectorStatsList();
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

}
