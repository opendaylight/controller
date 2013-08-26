/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow;

import java.util.List;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;

/**
 * Interface to serve the hardware information requests coming from SAL It is
 * implemented by the respective OF1.0 plugin component
 *
 */
public interface IReadServiceFilter {
    /**
     * Returns the hardware image for the specified flow on the specified
     * network node for the passed container
     *
     * @param container
     *            the container for which the request is originated
     * @param node
     *            the network node
     * @param flow
     *            the target flow
     * @param cached
     *            specify if entry has to be queried from the cached hardware
     *            information maintained in the protocol plugin or directly from
     *            the network node.
     * @return The FlowOnNode object containing the information present in
     *         hardware for the passed flow on the specified network node
     */
    public FlowOnNode readFlow(String container, Node node, Flow flow, boolean cached);

    /**
     * Returns the hardware view of all the flow installed on the specified
     * network node for the passed container
     *
     * @param container
     *            the container for which the request is originated
     * @param node
     *            the network node
     * @param cached
     *            specify if entries have to be queried from the cached hardware
     *            information maintained in the protocol plugin or directly from
     *            the network node.
     * @return The list of FlowOnNode objects containing the information present
     *         in hardware on the specified network node for all its flows
     */
    public List<FlowOnNode> readAllFlow(String container, Node node, boolean cached);

    /**
     * Returns the description of the network node as provided by the node
     * itself
     *
     * @param node
     *            the network node
     * @param cached
     *            specify if entry has to be queried from the cached hardware
     *            information maintained in the protocol plugin or directly from
     *            the network node.
     * @return The NodeDescription object containing the description information
     *         for the specified network node
     */
    public NodeDescription readDescription(Node node, boolean cached);

    /**
     * Returns the hardware view of the specified network node connector for the
     * given container
     *
     * @param container
     *            the container for which the request is originated
     * @param nodeConnector
     *            the target nodeConnector
     * @param cached
     *            specify if entry has to be queried from the cached hardware
     *            information maintained in the protocol plugin or directly from
     *            the corresponding network node.
     * @return The NodeConnectorStatistics object containing the statistics
     *         present in hardware for the corresponding network node port
     */
    public NodeConnectorStatistics readNodeConnector(String container, NodeConnector nodeConnector, boolean cached);

    /**
     * Returns the hardware info for all the node connectors on the specified
     * network node for the given container
     *
     * @param container
     *            the container for which the request is originated
     * @param node
     *            the target node
     * @param cached
     *            specify if entries have to be queried from the cached hardware
     *            information maintained in the protocol plugin or directly from
     *            the corresponding network node.
     * @return The list of NodeConnectorStatistics objects containing the
     *         statistics present in hardware for all the network node ports
     */
    public List<NodeConnectorStatistics> readAllNodeConnector(String container, Node node, boolean cached);

    /**
     * Returns the table statistics of the node as specified by the given
     * container
     *
     * @param container
     *            the container for which the request is originated
     * @param nodeTable
     *            the target network node table
     * @param cached
     *            specify if entry has to be queried from the cached hardware
     *            information maintained in the protocol plugin or directly from
     *            the corresponding network node.
     * @return The NodeTableStatistics object containing the statistics present
     *         in hardware for the corresponding network node table
     */
    public NodeTableStatistics readNodeTable(String container, NodeTable nodeTable, boolean cached);

    /**
     * Returns the table statistics of all the tables for the specified node
     *
     * @param container
     *            the container for which the request is originated
     * @param node
     *            the target node
     * @param cached
     *            specify if entries have to be queried from the cached hardware
     *            information maintained in the protocol plugin or directly from
     *            the corresponding network node.
     * @return The list of NodeTableStatistics objects containing the statistics
     *         present in hardware for all the network node tables
     */
    public List<NodeTableStatistics> readAllNodeTable(String containerName, Node node, boolean cached);

    /**
     * Returns the average transmit rate for the specified node connector on the
     * given container. If the node connector does not belong to the passed
     * container a zero value is returned
     *
     * @param container
     *            the container for which the request is originated
     * @param nodeConnector
     *            the target node connector
     * @return The average tx rate in bps
     */
    public long getTransmitRate(String container, NodeConnector nodeConnector);
}
