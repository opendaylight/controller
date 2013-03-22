
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.statisticsmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IReadService;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class which implements the methods for retrieving
 * the network nodes statistics.
 */
public class StatisticsManager implements IStatisticsManager {
    private static final Logger log = LoggerFactory
            .getLogger(StatisticsManager.class);
    private IReadService reader;

    public StatisticsManager() {

    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        log.debug("INIT called!");
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        log.debug("DESTROY called!");
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        log.debug("START called!");
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
        log.debug("STOP called!");
    }

    public void setReaderService(IReadService service) {
        log.debug("Got inventory service set request {}", service);
        this.reader = service;
    }

    public void unsetReaderService(IReadService service) {
        log.debug("Got a service UNset request");
        this.reader = null;
    }

    @Override
    public List<FlowOnNode> getFlows(Node node) {
        return reader.readAllFlows(node);
    }

    @Override
    public Map<Node, List<FlowOnNode>> getFlowStatisticsForFlowList(
            List<FlowEntry> flowList) {
        Map<Node, List<FlowOnNode>> map = new HashMap<Node, List<FlowOnNode>>();
        if (flowList != null) {
            for (FlowEntry entry : flowList) {
                Node node = entry.getNode();
                Flow flow = entry.getFlow();
                List<FlowOnNode> list = (map.containsKey(node)) ? map.get(node)
                        : new ArrayList<FlowOnNode>();
                list.add(reader.readFlow(node, flow));
                map.put(node, list);
            }
        }
        return map;
    }

    @Override
    public int getFlowsNumber(Node node) {
        return reader.readAllFlows(node).size();
    }

    @Override
    public NodeDescription getNodeDescription(Node node) {
        return reader.readDescription(node);
    }

    @Override
    public NodeConnectorStatistics getNodeConnectorStatistics(
            NodeConnector nodeConnector) {
        return reader.readNodeConnector(nodeConnector);
    }

    @Override
    public List<NodeConnectorStatistics> getNodeConnectorStatistics(Node node) {
        return reader.readNodeConnectors(node);
    }
}
