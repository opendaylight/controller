
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

/**
 * The interface defines hardware statistics updates service to be offered by
 * protocol plugins
 */
@Deprecated
public interface IPluginOutReadService {

    /**
     * Notifies the hardware view of all the flow installed on the specified
     * network node
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @param flowStatsList
     *            the hardware view of all the flow
     *            {@link org.opendaylight.controller.sal.reader.FlowOnNode}
     *            installed on the specified network node
     */
    public void nodeFlowStatisticsUpdated(Node node, List<FlowOnNode> flowStatsList);

    /**
     * Notifies the hardware view of the specified network node
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @param ncStatsList
     *            the statistics
     *            {@link org.opendaylight.controller.sal.reader.NodeConnectorStatistics}
     *            for all node connectors in a given node
     */
    public void nodeConnectorStatisticsUpdated(Node node, List<NodeConnectorStatistics> ncStatsList);

    /**
     * Notifies all the table statistics for a node
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @param tableStatsList
     *            the statistics
     *            {@link org.opendaylight.controller.sal.reader.NodeTableStatistics}
     *            for all the tables in a given node
     */
    public void nodeTableStatisticsUpdated(Node node, List<NodeTableStatistics> tableStatsList);

    /**
     * Notifies the hardware view of node description changes
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @param nodeDescription
     *            the node description
     *            {@link org.opendaylight.controller.sal.reader.NodeDescription}
     */
    public void descriptionStatisticsUpdated(Node node, NodeDescription nodeDescription );

}
