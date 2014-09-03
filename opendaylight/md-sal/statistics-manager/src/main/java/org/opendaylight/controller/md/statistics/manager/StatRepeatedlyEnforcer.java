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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager
 *
 * StatRepeatedlyEnforcer
 * Class implement Runnable and inside is running statistic collecting
 * process DataObject statistics by DataObject statistics for every FlowCapableNode.
 * Every statistics wait to finish previous statistics. Only if all statistics finish,
 * next FlowCapableNode Statistics should be collecting.
 * We are able to set minimal time for start next round cross all Network,
 * but all depends on network possibility.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 28, 2014
 */
public interface StatRepeatedlyEnforcer extends Runnable {

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

    /**
     * Method stop StatCollecting Thread
     */
     void shutdown();

    /**
     *
     * @param flowNode
     * @param statTypes
     */
    void connectedNodeRegistration(InstanceIdentifier<FlowCapableNode> flowNode,
            List<StatCapabTypes> statTypes);

    /**
     * All disconnected Nodes need be removed from stat list Nodes
     * @param flowNode
     */
    void disconnectedNodeUnregistration(InstanceIdentifier<FlowCapableNode> flowNode);

    /**
     * Method return true only and only if StatRepeatedlyEnforcer actually collecting
     * the statistics from device identified by input {@link InstanceIdentifier}.
     * Otherwise return false.
     *
     * @param InstanceIdentifier<? extends DataObject> flowNode
     * @return
     */
    boolean isProvidedIdentLocked(InstanceIdentifier<? extends DataObject> nodeIdent);

    /**
     * Method return true only and only if StatRepeatedlyEnforcer contain valid node
     * registration in its internal {@link FlowCapableNode} map.
     * Otherwise return false.
     *
     * @param InstanceIdentifier<FlowCapableNode> flowNode
     * @return
     */
    boolean isProvidedFlowNodeActive(InstanceIdentifier<FlowCapableNode> flowNode);

    /**
     * Object notification for continue statistics collecting process.
     * It is call from collecting allStatistics methods as a future result for
     * Operational/DS statistic store call (does not matter in the outcome).
     */
    void collectNextStatistics();
}

