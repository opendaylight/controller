/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 28, 2014
 */
public interface StatRepeatedlyEnforcer {

    /**
     * StatCapType
     * Enum class refers ofp_statistics capabilities fields from OF Switch
     * capabilities specification which have to come as a post HandShake
     * information from OF Switch and Inventory Manager adds all to the
     * Operational/DS.
     * If the capabilities are not add (for any reason) NodeRegistrator
     * adds all StatCapTypes for the FlowCapableNode.
     */
    public enum StatCapabTypes {
        /**
         * OFPC_FLOW_STATS
         */
        FLOW_STATS,
        /**
         * OFPC_TABLE_STATS
         */
        TABLE_STATS,
        /**
         * OFPC_PORT_STATS
         */
        PORT_STATS,
        /**
         * OFPC_GROUP_STATS
         */
        GROUP_STATS,
        /**
         * OFPC_QUEUE_STATS
         */
        QUEUE_STATS,
        /**
         * Meter statistics has no support from OF Switch capabilities
         * so we have to try get statistics for it and wait for response
         * Error or response package with results.
         */
        METER_STATS
    }

    void connectedNodeRegistration(InstanceIdentifier<FlowCapableNode> flowNode,
            List<StatCapabTypes> statTypes);

    void disconnectedNodeUnregistration(InstanceIdentifier<FlowCapableNode> flowNode);

    /**
     * Method return true only and only if StatRepeatedlyEnforcer actually collecting
     * the statistics from device identified by input {@link FlowCapableNode}.
     * Otherwise return false.
     *
     * @param InstanceIdentifier<FlowCapableNode> flowNode
     * @return
     */
    boolean isProvidedFlowNodeLocked(InstanceIdentifier<FlowCapableNode> flowNode);

    /**
     * Method return true only and only if StatRepeatedlyEnforcer contain valid node
     * registration in its in its internal {@link FlowCapableNode} map.
     * Otherwise return false.
     *
     * @param InstanceIdentifier<FlowCapableNode> flowNode
     * @return
     */
    boolean isProvidedFlowNodeActive(InstanceIdentifier<FlowCapableNode> flowNode);
}

