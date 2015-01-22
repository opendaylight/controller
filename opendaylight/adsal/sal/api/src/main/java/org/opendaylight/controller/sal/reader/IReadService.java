
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.reader;

import java.util.List;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.flowprogrammer.Flow;

/**
 * This interface defines methods for retrieving the network node's
 * flow/port/queue hardware view
 */
@Deprecated
public interface IReadService {
    /**
     * Get the hardware view for the specified flow on the specified network
     * node
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @param flow
     *            the given flow
     *            {@link org.opendaylight.controller.sal.flowprogrammer.Flow}
     * @return the flow
     *         {@link org.opendaylight.controller.sal.reader.FlowOnNode}
     *         installed on the node
     */
    FlowOnNode readFlow(Node node, Flow flow);

    /**
     * Get the hardware view for the specified flow on the specified network node
     * This call results in a direct polling of the information from the node
     * Caller will be blocked until node replies or request times out
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @param flow
     *            the given flow
     *            {@link org.opendaylight.controller.sal.flowprogrammer.Flow}
     * @return the flow
     *         {@link org.opendaylight.controller.sal.reader.FlowOnNode}
     *         installed on the node
     */
    FlowOnNode nonCachedReadFlow(Node node, Flow flow);

    /**
     * Get the hardware view for all the flows installed on the network node
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @return all the flows
     *         {@link org.opendaylight.controller.sal.reader.FlowOnNode}
     *         installed on the node
     */
    List<FlowOnNode> readAllFlows(Node node);

    /**
     * Get the hardware view for all the flows installed on the network node
     * This call results in a direct polling of the information from the node
     * Caller will be blocked until node replies or request times out
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @return the hardware view of all the flows
     *         {@link org.opendaylight.controller.sal.reader.FlowOnNode}
     *         installed on the node
     */
    List<FlowOnNode> nonCachedReadAllFlows(Node node);

    /**
     * Get the description information for the network node
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @return the node description
     *         {@link org.opendaylight.controller.sal.reader.NodeDescription}
     */
    NodeDescription readDescription(Node node);

    /**
     * Get the description information for the network node
     * This call results in a direct polling of the information from the node
     * Caller will be blocked until node replies or request times out
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @return the node description
     *         {@link org.opendaylight.controller.sal.reader.NodeDescription}
     */
    NodeDescription nonCachedReadDescription(Node node);

    /**
     * Get the hardware view for the specified node connector
     *
     * @param connector
     *            the given node connector
     *            {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @return the node connector statistics
     *         {@link org.opendaylight.controller.sal.reader.NodeConnectorStatistics}
     */
    NodeConnectorStatistics readNodeConnector(NodeConnector connector);

    /**
     * Get the hardware view for all the node connectors
     * present on the specified network node
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @return the statistics for all the node connectors
     *         {@link org.opendaylight.controller.sal.reader.NodeConnectorStatistics}
     */
    List<NodeConnectorStatistics> readNodeConnectors(Node node);

    /**
     * Read the Table statistics for the given node table
     *
     * @param table
     *            the table
     *            {@link org.opendaylight.controller.sal.core.NodeTable}
     * @return the table statistics
     *         {@link org.opendaylight.controller.sal.reader.NodeTableStatistics}
     */
    NodeTableStatistics readNodeTable(NodeTable table);

    /**
     * Read the Table statistics for the given node This is not used. Querying
     * all tables on a node is not currently a feature.
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @return the table statistics
     *         {@link org.opendaylight.controller.sal.reader.NodeTableStatistics}
     *         for all tables in a given node
     */
    List<NodeTableStatistics> readNodeTable(Node node);

    /**
     * Get the table statistics for the given node table
     * This call results in a direct polling of the information from the node
     * Caller will be blocked until the node replies or request times out
     *
     * @param table
     *            the table
     *            {@link org.opendaylight.controller.sal.core.NodeTable}
     * @return the table statistics
     *         {@link org.opendaylight.controller.sal.reader.NodeTableStatistics}
     */
    NodeTableStatistics nonCachedReadNodeTable(NodeTable table);

    /**
     * Get the node connectors statistics information for the network node
     * This call results in a direct polling of the information from the node
     * Caller will be blocked until node replies or request times out
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @return the statistics
     *         {@link org.opendaylight.controller.sal.reader.NodeConnectorStatistics}
     *         for all node connectors in a given node
     */
    List<NodeConnectorStatistics> nonCachedReadNodeConnectors(Node node);

    /**
     * Get the node connectors statistics information for the network node
     *
     * @param connector
     *            the given node connector
     *            {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @return the node connector statistics
     *         {@link org.opendaylight.controller.sal.reader.NodeConnectorStatistics}
     */
    NodeConnectorStatistics nonCachedReadNodeConnector(NodeConnector connector);

    /**
     * Get the transmit rate for the specified node connector
     *
     * @param connector
     *            the given node connector
     *            {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @return tx rate [bps]
     */
    long getTransmitRate(NodeConnector connector);

}
