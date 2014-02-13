/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.aggregate.flow.statistics.AggregateFlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.features.GroupFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.desc.stats.reply.GroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.reply.GroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.AggregateFlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.queue.id.and.statistics.map.QueueIdAndStatisticsMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * This class handles the lifecycle of per-node statistics. It receives data
 * from StatisticsListener, stores it in the data store and keeps track of
 * when the data should be removed.
 *
 * @author avishnoi@in.ibm.com
 */
public final class NodeStatisticsHandler implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(NodeStatisticsHandler.class);
    private static final int NUMBER_OF_WAIT_CYCLES = 2;

    private final InstanceIdentifier<Node> targetNodeIdentifier;
    private final FlowStatsTracker flowStats;
    private final FlowTableStatsTracker flowTableStats;
    private final GroupDescStatsTracker groupDescStats;
    private final GroupStatsTracker groupStats;
    private final MeterConfigStatsTracker meterConfigStats;
    private final MeterStatsTracker meterStats;
    private final NodeConnectorStatsTracker nodeConnectorStats;
    private final QueueStatsTracker queueStats;
    private final DataProviderService dps;
    private final NodeRef targetNodeRef;
    private final NodeKey targetNodeKey;

    public NodeStatisticsHandler(final DataProviderService dps, final NodeKey nodeKey) {
        this.dps = Preconditions.checkNotNull(dps);
        this.targetNodeKey = Preconditions.checkNotNull(nodeKey);
        this.targetNodeIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey).build();
        this.targetNodeRef = new NodeRef(targetNodeIdentifier);

        final long lifetimeNanos = TimeUnit.MILLISECONDS.toNanos(StatisticsProvider.STATS_COLLECTION_MILLIS * NUMBER_OF_WAIT_CYCLES);
        flowStats = new FlowStatsTracker(targetNodeIdentifier, dps, lifetimeNanos);
        flowTableStats = new FlowTableStatsTracker(targetNodeIdentifier, dps, lifetimeNanos);
        groupDescStats = new GroupDescStatsTracker(targetNodeIdentifier, dps, lifetimeNanos);
        groupStats = new GroupStatsTracker(targetNodeIdentifier, dps, lifetimeNanos);
        meterConfigStats = new MeterConfigStatsTracker(targetNodeIdentifier, dps, lifetimeNanos);
        meterStats = new MeterStatsTracker(targetNodeIdentifier, dps, lifetimeNanos);
        nodeConnectorStats = new NodeConnectorStatsTracker(targetNodeIdentifier, dps, lifetimeNanos);
        queueStats = new QueueStatsTracker(targetNodeIdentifier, dps, lifetimeNanos);
    }

    public NodeKey getTargetNodeKey() {
        return targetNodeKey;
    }

    public Collection<TableKey> getKnownTables() {
        return flowTableStats.getTables();
    }

    public InstanceIdentifier<Node> getTargetNodeIdentifier() {
        return targetNodeIdentifier;
    }

    public NodeRef getTargetNodeRef() {
        return targetNodeRef;
    }

    public synchronized void updateGroupDescStats(List<GroupDescStats> list) {
        groupDescStats.updateStats(list);
    }

    public synchronized void updateGroupStats(List<GroupStats> list) {
        groupStats.updateStats(list);
    }

    public synchronized void updateMeterConfigStats(List<MeterConfigStats> list) {
        meterConfigStats.updateStats(list);
    }

    public synchronized void updateMeterStats(List<MeterStats> list) {
        meterStats.updateStats(list);
    }

    public synchronized void updateQueueStats(List<QueueIdAndStatisticsMap> list) {
        queueStats.updateStats(list);
    }

    public synchronized void updateFlowTableStats(List<FlowTableAndStatisticsMap> list) {
        flowTableStats.updateStats(list);
    }

    public synchronized void updateNodeConnectorStats(List<NodeConnectorStatisticsAndPortNumberMap> list) {
        nodeConnectorStats.updateStats(list);
    }

    public synchronized void updateAggregateFlowStats(Short tableId, AggregateFlowStatistics flowStats) {
        if (tableId != null) {
            final DataModificationTransaction trans = dps.beginTransaction();

            InstanceIdentifier<Table> tableRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey)
                    .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(tableId)).toInstance();

            AggregateFlowStatisticsDataBuilder aggregateFlowStatisticsDataBuilder = new AggregateFlowStatisticsDataBuilder();
            AggregateFlowStatisticsBuilder aggregateFlowStatisticsBuilder = new AggregateFlowStatisticsBuilder(flowStats);

            aggregateFlowStatisticsDataBuilder.setAggregateFlowStatistics(aggregateFlowStatisticsBuilder.build());

            logger.debug("Augment aggregate statistics: {} for table {} on Node {}",
                    aggregateFlowStatisticsBuilder.build().toString(),tableId,targetNodeKey);

            TableBuilder tableBuilder = new TableBuilder();
            tableBuilder.setKey(new TableKey(tableId));
            tableBuilder.addAugmentation(AggregateFlowStatisticsData.class, aggregateFlowStatisticsDataBuilder.build());
            trans.putOperationalData(tableRef, tableBuilder.build());

            // FIXME: should we be tracking this data?
            trans.commit();
        }
    }

    public synchronized void updateGroupFeatures(GroupFeatures notification) {
        final DataModificationTransaction trans = dps.beginTransaction();

        final NodeBuilder nodeData = new NodeBuilder();
        nodeData.setKey(targetNodeKey);

        NodeGroupFeaturesBuilder nodeGroupFeatures = new NodeGroupFeaturesBuilder();
        GroupFeaturesBuilder groupFeatures = new GroupFeaturesBuilder(notification);
        nodeGroupFeatures.setGroupFeatures(groupFeatures.build());

        //Update augmented data
        nodeData.addAugmentation(NodeGroupFeatures.class, nodeGroupFeatures.build());
        trans.putOperationalData(targetNodeIdentifier, nodeData.build());

        // FIXME: should we be tracking this data?
        trans.commit();
    }

    public synchronized void updateMeterFeatures(MeterFeatures features) {
        final DataModificationTransaction trans = dps.beginTransaction();

        final NodeBuilder nodeData = new NodeBuilder();
        nodeData.setKey(targetNodeKey);

        NodeMeterFeaturesBuilder nodeMeterFeatures = new NodeMeterFeaturesBuilder();
        MeterFeaturesBuilder meterFeature = new MeterFeaturesBuilder(features);
        nodeMeterFeatures.setMeterFeatures(meterFeature.build());

        //Update augmented data
        nodeData.addAugmentation(NodeMeterFeatures.class, nodeMeterFeatures.build());
        trans.putOperationalData(targetNodeIdentifier, nodeData.build());

        // FIXME: should we be tracking this data?
        trans.commit();
    }

    public synchronized void updateFlowStats(List<FlowAndStatisticsMapList> list) {
        flowStats.updateStats(list);
    }

    public synchronized void cleanStaleStatistics() {
        final DataModificationTransaction trans = dps.beginTransaction();
        final long now = System.nanoTime();

        flowStats.cleanup(trans, now);
        groupDescStats.cleanup(trans, now);
        groupStats.cleanup(trans, now);
        meterConfigStats.cleanup(trans, now);
        meterStats.cleanup(trans, now);
        nodeConnectorStats.cleanup(trans, now);
        queueStats.cleanup(trans, now);

        trans.commit();
    }

    @Override
    public void close() {
        // FIXME: cleanup any resources we hold (registrations, etc.)
        logger.debug("Statistics handler for {} shut down", targetNodeKey.getId());
    }
}
