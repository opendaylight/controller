
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
 * The interface defines hardware view read methods to be implemented by protocol plugins
 */
@Deprecated
public interface IPluginInReadService {

    /**
     * Returns the hardware image for the specified flow on the specified
     * network node
     *
     * @param node
     *            the network node
     * @param flow
     *            the target flow
     * @param cached
     *            specify if entry has to be queried from the cached hardware
     *            information maintained locally or directly from the network
     *            node.
     * @return The FlowOnNode object containing the information present in
     *         hardware for the passed flow on the specified network node
     */
    public FlowOnNode readFlow(Node node, Flow flow, boolean cached);

    /**
     * Returns the hardware view of all the flow installed on the specified
     * network node
     *
     * @param node
     *            the network node
     * @param cached
     *            specify if entries have to be queried from the cached hardware
     *            information maintained locally or directly from the network
     *            node.
     * @return The list of FlowOnNode objects containing the information present
     *         in hardware on the specified network node for all its flows
     */
    public List<FlowOnNode> readAllFlow(Node node, boolean cached);

    /**
     * Returns the description of the network node as provided by the node
     * itself
     *
     * @param node
     *            the network node
     * @param cached
     *            specify if entry has to be queried from the cached hardware
     *            information maintained locally or directly from the network
     *            node.
     * @return The NodeDescription object containing the description information
     *         for the specified network node
     */
    public NodeDescription readDescription(Node node, boolean cached);

    /**
     * Returns the hardware view of the specified network node connector
     *
     * @param connector
     *            the target nodeConnector
     * @param cached
     *            specify if entry has to be queried from the cached hardware
     *            information maintained locally or directly from the
     *            corresponding network node.
     * @return The NodeConnectorStatistics object containing the statistics
     *         present in hardware for the corresponding network node port
     */
    public NodeConnectorStatistics readNodeConnector(NodeConnector connector,
            boolean cached);

    /**
     * Returns the hardware info for all the node connectors on the specified
     * network node
     *
     * @param node
     *            the target node
     * @param cached
     *            specify if entries have to be queried from the cached hardware
     *            information maintained locally or directly from the
     *            corresponding network node.
     * @return The list of NodeConnectorStatistics objects containing the
     *         statistics present in hardware for all the network node ports
     */
    public List<NodeConnectorStatistics> readAllNodeConnector(Node node,
            boolean cached);

    /**
     * Returns the table statistics for the node
     * @param table
     *            the target network node table
     * @param cached
     *            specify if entry has to be queried from the cached hardware
     *            information maintained locally or directly from
     *            the corresponding network node.
     * @return The NodeTableStatistics object containing the statistics present
     *         in hardware for the corresponding network node table
     */
    public NodeTableStatistics readNodeTable(NodeTable table, boolean cached);

    /**
     * Returns all the table statistics for the node
     *
     * @param node
     *            the target node
     * @param cached
     *            specify if entries have to be queried from the cached hardware
     *            information maintained locally or directly from the
     *            corresponding network node.
     * @return The list of NodeTableStatistics objects containing the statistics
     *         present in hardware for all the network node tables
     */
    public List<NodeTableStatistics> readAllNodeTable(Node node, boolean cached);

    /**
     * Returns the averaged transmit rate for the specified node connector
     * @param connector
     *            the target nodeConnector
     * @return tx rate [bps]
     */
    public long getTransmitRate(NodeConnector connector);

}
