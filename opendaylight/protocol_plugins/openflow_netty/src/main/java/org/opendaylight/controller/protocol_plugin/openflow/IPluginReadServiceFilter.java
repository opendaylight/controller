
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
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;

/**
 * Interface to serve the hardware information requests coming from SAL
 * It is implemented by the respective OF1.0 plugin component
 *
 */
public interface IPluginReadServiceFilter {
    /**
     * Returns the hardware image for the specified flow
     * on the specified network node for the passed container
     *
     * @param container
     * @param node
     * @param flow
     * @param cached
     * @return
     */
    public FlowOnNode readFlow(String container, Node node, Flow flow,
            boolean cached);

    /**
     * Returns the hardware view of all the flow installed
     * on the specified network node for the passed container
     *
     * @param container
     * @param node
     * @param cached
     * @return
     */
    public List<FlowOnNode> readAllFlow(String container, Node node,
            boolean cached);

    /**
     * Returns the description of the network node as provided by the node itself
     *
     * @param node
     * @param cached
     * @return
     */
    public NodeDescription readDescription(Node node, boolean cached);

    /**
     * Returns the hardware view of the specified network node connector
     * for the given container
     * @param node
     * @return
     */
    public NodeConnectorStatistics readNodeConnector(String container,
            NodeConnector nodeConnector, boolean cached);

    /**
     * Returns the hardware info for all the node connectors on the
     * specified network node for the given container
     *
     * @param node
     * @return
     */
    public List<NodeConnectorStatistics> readAllNodeConnector(String container,
            Node node, boolean cached);

    /**
     * Returns the average transmit rate for the specified node conenctor on
     * the given container. If the node connector does not belong to the passed
     * container a zero value is returned
     *
     * @param container
     * @param nodeConnector
     * @return tx rate [bps]
     */
    public long getTransmitRate(String container, NodeConnector nodeConnector);
}
