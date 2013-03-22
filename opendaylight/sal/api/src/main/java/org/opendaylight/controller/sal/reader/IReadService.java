
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
import org.opendaylight.controller.sal.flowprogrammer.Flow;

/**
 * Interface for retrieving the network node's flow/port/queue hardware view
 *
 *
 *
 */
public interface IReadService {
    /**
     * Get the hardware view for the specified flow on the specified network node
     *
     * @param node
     * @param flow
     */
    FlowOnNode readFlow(Node node, Flow flow);

    /**
     * Get the hardware view for the specified flow on the specified network node
     * This call results in a direct polling of the information from the node
     * Caller will be blocked until node replies or request times out
     *
     * @param node
     * @param flow
     */
    FlowOnNode nonCachedReadFlow(Node node, Flow flow);

    /**
     * Get the hardware view for all the flows installed on the network node
     *
     * @param node
     * @return
     */
    List<FlowOnNode> readAllFlows(Node node);

    /**
     * Get the hardware view for all the flows installed on the network node
     * This call results in a direct polling of the information from the node
     * Caller will be blocked until node replies or request times out
     *
     * @param node
     * @param flow
     */
    List<FlowOnNode> nonCachedReadAllFlows(Node node);

    /**
     * Get the description information for the network node
     * @param node
     * @return
     */
    NodeDescription readDescription(Node node);

    /**
     * Get the description information for the network node
     * This call results in a direct polling of the information from the node
     * Caller will be blocked until node replies or request times out
     *
     * @param node
     * @return
     */
    NodeDescription nonCachedReadDescription(Node node);

    /**
     * Get the hardware view for the specified node connector
     * @param connector
     */
    NodeConnectorStatistics readNodeConnector(NodeConnector connector);

    /**
     * Get the hardware view for all the node connectors
     * present on the specified network node
     * @param connector
     */
    List<NodeConnectorStatistics> readNodeConnectors(Node node);

    /**
     * Get the node connectors statistics information for the network node
     * This call results in a direct polling of the information from the node
     * Caller will be blocked until node replies or request times out
     *
     * @param node
     * @return
     */
    List<NodeConnectorStatistics> nonCachedReadNodeConnectors(Node node);

    /**
     * Get the node connectors statistics information for the network node
     *
     * @param node
     * @return
     */
    NodeConnectorStatistics nonCachedReadNodeConnector(NodeConnector connector);

    /**
     * Get the transmit rate for the specified node connector
     *
     * @param connector
     * @return tx rate [bps]
     */
    long getTransmitRate(NodeConnector connector);

}
