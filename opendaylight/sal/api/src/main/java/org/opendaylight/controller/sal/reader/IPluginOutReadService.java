
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
 * @file   IPluginOutReadService.java
 *
 * @brief  Hardware statistics updates service to be offered by protocol plugins
 */
public interface IPluginOutReadService {

    /**
     * Notifies the hardware view of all the flow installed on the specified network node
     * @param node
     * @return
     */
    public void nodeFlowStatisticsUpdated(Node node, List<FlowOnNode> flowStatsList);

    /**
     * Notifies the hardware view of the specified network node connector
     * @param node
     * @return
     */
    public void nodeConnectorStatisticsUpdated(Node node, List<NodeConnectorStatistics> ncStatsList);

    /**
     * Notifies all the table statistics for a node
     * @param node
     * @return
     */
    public void nodeTableStatisticsUpdated(Node node, List<NodeTableStatistics> tableStatsList);
    /**
     * Notifies the hardware view of node description changes
     * @param node
     * @return
     */
    public void descriptionStatisticsUpdated(Node node, NodeDescription nodeDescription );

}
