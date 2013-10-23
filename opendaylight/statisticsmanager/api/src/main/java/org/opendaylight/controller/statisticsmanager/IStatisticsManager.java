
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.statisticsmanager;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;

/**
 * Interface which defines the available methods for retrieving
 * the network nodes statistics.
 */
public interface IStatisticsManager {
    /**
     * Return all the statistics for all the flows present on the specified node
     * in the current container context. If the context is the default
     * container, the returned statistics are for all the flows installed on the
     * node, regardless of the container they belong to
     *
     * @param node
     *            The network node
     * @return List of flows installed on the network node. Null if specified
     *         node is null. Empty list if node/stat is not present.
     */
    List<FlowOnNode> getFlows(Node node);

    /**
     * Same as the getFlows method.
     * The only difference is that this method does not return cached flows.
     * It will always make a request to the node to get all the flows for that node.
     * If the request times out or gets an error, we revert to getting the cached flows.
     * @see IStatisticsManager#getFlows
     * @param node
     * @return List of flows installed on the network node.
     */
    List<FlowOnNode> getFlowsNoCache(Node node);

    /**
     * Returns the statistics for the flows specified in the list
     *
     * @param flows
     * @return A map of flows per node installed on that node, empty map if
     *         flows is null/empty.
     */
    Map<Node, List<FlowOnNode>> getFlowStatisticsForFlowList(List<FlowEntry> flows);

    /**
     * Returns the number of flows installed on the switch in the current
     * container context If the context is the default container, the returned
     * value is the number of all the flows installed on the switch regardless
     * of the container they belong to
     *
     * @param node
     * @return number of flows on specified node or (-1) if node was not found
     */
    int getFlowsNumber(Node node);

    /**
     * Returns the node description for the specified node retrieved by the
     * protocol plugin component and cached by statistics manager.
     * Null if node not found.
     *
     * @param node
     * @return node description
     */
    NodeDescription getNodeDescription(Node node);

    /**
     * Returns the statistics for the specified node connector as it was
     * retrieved by the protocol plugin component and cached by statistics
     * manager.
     *
     * @param node
     * @return Node connector statistics or null if requested stats was not
     *         found.
     */
    NodeConnectorStatistics getNodeConnectorStatistics(NodeConnector nodeConnector);

    /**
     * Returns the statistics for all the node connector present on the
     * specified network node
     *
     * @param node
     * @return List of node connector statistics. Null if node is null. Empty
     *         list if node/stats is not present.
     */
    List<NodeConnectorStatistics> getNodeConnectorStatistics(Node node);

    /**
     * Returns the statistics for the specified table of the node
     *
     * @param nodeTable
     * @return Table statistics. Null if node table is null or stats not found.
     */
    NodeTableStatistics getNodeTableStatistics(NodeTable nodeTable);

    /**
     * Returns the statistics for all the tables of the node
     *
     * @param nodeTable
     * @return List of table stats. Null if node is null. Empty list if
     *         node/stats not found.
     */
    List <NodeTableStatistics> getNodeTableStatistics(Node node);
}
