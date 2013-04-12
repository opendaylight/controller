
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
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;

/**
 * Interface which defines the available methods for retrieving
 * the network nodes statistics.
 */
public interface IStatisticsManager {
    /**
     * Return all the statistics for all the flows present on the specified node in the current container context.
     * If the context is the default container, the returned statistics are for all the flows installed on the node,
     * regardless of the container they belong to
     *
     * @param node	the network node
     * @return the list of flows installed on the network node
     */
    List<FlowOnNode> getFlows(Node node);

    /**
     * Returns the statistics for the flows specified in the list
     *
     * @param flows
     * @return	the list of flows installed on the network node
     */
    Map<Node, List<FlowOnNode>> getFlowStatisticsForFlowList(
            List<FlowEntry> flows);

    /**
     * Returns the number of flows installed on the switch in the current container context
     * If the context is the default container, the returned value is the number of all the
     * flows installed on the switch regardless of the container they belong to
     *
     * @param switchId
     * @return
     */
    int getFlowsNumber(Node node);

    /**
     * Returns the node description for the specified node retrieved and cached by the
     * protocol plugin component which collects the node statistics
     *
     * @param node
     * @return
     */
    NodeDescription getNodeDescription(Node node);

    /**
     * Returns the statistics for the specified node connector as it was retrieved
     * and cached by the protocol plugin component which collects the node connector statistics
     *
     * @param node
     * @return
     */
    NodeConnectorStatistics getNodeConnectorStatistics(
            NodeConnector nodeConnector);

    /**
     * Returns the statistics for all the node connector present on the specified network node
     *
     * @param node
     * @return
     */
    List<NodeConnectorStatistics> getNodeConnectorStatistics(Node node);
}
