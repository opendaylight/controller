
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
 * @file   IPluginInReadService.java
 *
 * @brief  Hardware view interface to be implemented by protocol plugins
 *
 *
 *
 */
public interface IPluginInReadService {

    /**
     * Returns the hardware image for the specified flow on the specified network node
     * @param node
     * @param flow
     * @return
     */
    public FlowOnNode readFlow(Node node, Flow flow, boolean cached);

    /**
     * Returns the hardware view of all the flow installed on the specified network node
     * @param node
     * @return
     */
    public List<FlowOnNode> readAllFlow(Node node, boolean cached);

    /**
     * Returns the description of the network node as provided by the node itself
     * @param node
     * @return
     */
    public NodeDescription readDescription(Node node, boolean cached);

    /**
     * Returns the hardware view of the specified network node connector
     * @param node
     * @return
     */
    public NodeConnectorStatistics readNodeConnector(NodeConnector connector,
            boolean cached);

    /**
     * Returns the hardware info for all the node connectors on the specified network node
     * @param node
     * @return
     */
    public List<NodeConnectorStatistics> readAllNodeConnector(Node node,
            boolean cached);

    /**
     * Returns the averaged transmit rate for the specified node connector
     * @param connector
     * @return tx rate [bps]
     */
    public long getTransmitRate(NodeConnector connector);
}
