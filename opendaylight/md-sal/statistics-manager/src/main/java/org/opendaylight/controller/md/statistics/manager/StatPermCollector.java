/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager
 *
 * StatPermCollector
 * Class implement {@link Runnable} and inside is running statistic collecting
 * process DataObject statistics by DataObject statistics for every {@link org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode}.
 * Every statistics wait to finish previous statistics. Only if all statistics finish,
 * next {@link org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode}
 * Statistics should be collecting. We are able to set minimal time for start next round cross all Network,
 * but all depends on network possibility.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 28, 2014
 */
public interface StatPermCollector extends Runnable, AutoCloseable {

    /**
     * StatCapType
     * Enum class refers ofp_statistics capabilities fields from OF Switch
     * capabilities specification which have to come as a post HandShake
     * information from OF Switch and Inventory Manager adds all to the
     * Operational/DS.
     * If the capabilities are not add (for any reason) NodeRegistrator
     * adds all StatCapTypes for the {@link Node}.
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
     * Add new connected node for permanent statistics collecting process
     *
     * @param flowNode
     * @param statTypes
     * @param nrOfSwitchTables
     * @return true/false if the {@link Node} added successful
     */
    boolean connectedNodeRegistration(InstanceIdentifier<Node> nodeIdent,
            List<StatCapabTypes> statTypes, Short nrOfSwitchTables);

    /**
     * All disconnected Nodes need be removed from stat list Nodes
     *
     * @param flowNode
     * @return true/false if the {@link Node} removed successful
     */
    boolean disconnectedNodeUnregistration(InstanceIdentifier<Node> nodeIdent);

    /**
     * Method add new feature {@link StatCapabTypes} to Node identified by
     * nodeIdent -> InstanceIdentifier<Node>
     *
     * @param flowNode
     * @return true/false if the {@link StatCapabTypes} add successful
     */
    boolean registerAdditionalNodeFeature(InstanceIdentifier<Node> nodeIdent, StatCapabTypes statCapab);

    /**
     * Method return true only and only if {@link StatPermCollector} contain
     * valid node registration in its internal {@link Node} map.
     * Otherwise return false.
     *
     * @param flowNode
     * @return
     */
    boolean isProvidedFlowNodeActive(InstanceIdentifier<Node> nodeIdent);

    /**
     * Object notification for continue statistics collecting process.
     * It is call from collecting allStatistics methods as a future result for
     * Operational/DS statistic store call (does not matter in the outcome).
     */
    void collectNextStatistics();

    /**
     * Method returns true if collector has registered some active nodes
     * otherwise return false.
     *
     * @return
     */
    boolean hasActiveNodes();
}

