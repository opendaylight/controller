/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMapBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NodeConnectorStatsTracker extends AbstractStatsTracker<NodeConnectorStatisticsAndPortNumberMap, NodeConnectorStatisticsAndPortNumberMap> {
    private static final Logger logger = LoggerFactory.getLogger(NodeConnectorStatsTracker.class);
    private final OpendaylightPortStatisticsService portStatsService;

    NodeConnectorStatsTracker(final OpendaylightPortStatisticsService portStatsService, final FlowCapableContext context) {
        super(context);
        this.portStatsService = portStatsService;
    }

    @Override
    protected void cleanupSingleStat(final DataModificationTransaction trans, final NodeConnectorStatisticsAndPortNumberMap item) {
        // TODO Auto-generated method stub
    }

    @Override
    protected NodeConnectorStatisticsAndPortNumberMap updateSingleStat(final DataModificationTransaction trans, final NodeConnectorStatisticsAndPortNumberMap item) {
        FlowCapableNodeConnectorStatisticsBuilder statisticsBuilder
                                        = new FlowCapableNodeConnectorStatisticsBuilder();
        statisticsBuilder.setBytes(item.getBytes());
        statisticsBuilder.setCollisionCount(item.getCollisionCount());
        statisticsBuilder.setDuration(item.getDuration());
        statisticsBuilder.setPackets(item.getPackets());
        statisticsBuilder.setReceiveCrcError(item.getReceiveCrcError());
        statisticsBuilder.setReceiveDrops(item.getReceiveDrops());
        statisticsBuilder.setReceiveErrors(item.getReceiveErrors());
        statisticsBuilder.setReceiveFrameError(item.getReceiveFrameError());
        statisticsBuilder.setReceiveOverRunError(item.getReceiveOverRunError());
        statisticsBuilder.setTransmitDrops(item.getTransmitDrops());
        statisticsBuilder.setTransmitErrors(item.getTransmitErrors());

        //Augment data to the node-connector
        FlowCapableNodeConnectorStatisticsDataBuilder statisticsDataBuilder =
                new FlowCapableNodeConnectorStatisticsDataBuilder();

        statisticsDataBuilder.setFlowCapableNodeConnectorStatistics(statisticsBuilder.build());

        final NodeConnectorKey key = new NodeConnectorKey(item.getNodeConnectorId());
        final InstanceIdentifier<NodeConnector> nodeConnectorRef = getNodeIdentifier().child(NodeConnector.class, key);

        // FIXME: can we bypass this read?
        NodeConnector nodeConnector = (NodeConnector)trans.readOperationalData(nodeConnectorRef);
        if(nodeConnector != null){
            final FlowCapableNodeConnectorStatisticsData stats = statisticsDataBuilder.build();
            logger.debug("Augmenting port statistics {} to port {}",stats,nodeConnectorRef.toString());
            NodeConnectorBuilder nodeConnectorBuilder = new NodeConnectorBuilder()
                .setKey(key).setId(item.getNodeConnectorId())
                .addAugmentation(FlowCapableNodeConnectorStatisticsData.class, stats);
            trans.putOperationalData(nodeConnectorRef, nodeConnectorBuilder.build());
        }

        return item;
    }

    @Override
    public void request() {
        if (portStatsService != null) {
            final GetAllNodeConnectorsStatisticsInputBuilder input = new GetAllNodeConnectorsStatisticsInputBuilder();
            input.setNode(getNodeRef());

            requestHelper(portStatsService.getAllNodeConnectorsStatistics(input.build()));
        }
    }

    @Override
    protected NodeConnectorStatisticsAndPortNumberMap createInvariantKey(NodeConnectorStatisticsAndPortNumberMap item) {
        NodeConnectorStatisticsAndPortNumberMapBuilder ncStatsBuilder = new NodeConnectorStatisticsAndPortNumberMapBuilder();
        ncStatsBuilder.setNodeConnectorId(item.getNodeConnectorId());
        ncStatsBuilder.setKey(item.getKey());
        return ncStatsBuilder.build();
    }
}
