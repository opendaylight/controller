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

import org.opendaylight.controller.md.statistics.manager.MultipartMessageManager.StatsRequestType;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.aggregate.flow.statistics.AggregateFlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.queue.rev130925.QueueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.features.GroupFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.desc.stats.reply.GroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.reply.GroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.AggregateFlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.queue.id.and.statistics.map.QueueIdAndStatisticsMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * This class handles the lifecycle of per-node statistics. It receives data
 * from StatisticsListener, stores it in the data store and keeps track of
 * when the data should be removed.
 *
 * @author avishnoi@in.ibm.com
 */
public final class NodeStatisticsHandler implements AutoCloseable, FlowCapableContext {
    private static final Logger logger = LoggerFactory.getLogger(NodeStatisticsHandler.class);
    private static final int NUMBER_OF_WAIT_CYCLES = 2;

    private final MultipartMessageManager msgManager = new MultipartMessageManager();
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

    public NodeStatisticsHandler(final DataProviderService dps, final NodeKey nodeKey,
            final OpendaylightFlowStatisticsService flowStatsService,
            final OpendaylightFlowTableStatisticsService flowTableStatsService,
            final OpendaylightGroupStatisticsService groupStatsService,
            final OpendaylightMeterStatisticsService meterStatsService,
            final OpendaylightPortStatisticsService portStatsService,
            final OpendaylightQueueStatisticsService queueStatsService) {
        this.dps = Preconditions.checkNotNull(dps);
        this.targetNodeKey = Preconditions.checkNotNull(nodeKey);
        this.targetNodeIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey).build();
        this.targetNodeRef = new NodeRef(targetNodeIdentifier);

        final long lifetimeNanos = TimeUnit.MILLISECONDS.toNanos(StatisticsProvider.STATS_COLLECTION_MILLIS * NUMBER_OF_WAIT_CYCLES);

        if (flowStatsService != null) {
            flowStats = new FlowStatsTracker(flowStatsService, this, lifetimeNanos);
        } else {
            flowStats = null;
        }
        if (flowTableStatsService != null) {
            flowTableStats = new FlowTableStatsTracker(flowTableStatsService, this, lifetimeNanos);
        } else {
            flowTableStats = null;
        }

        if (groupStatsService != null) {
            groupDescStats = new GroupDescStatsTracker(groupStatsService, this, lifetimeNanos);
            groupStats = new GroupStatsTracker(groupStatsService, this, lifetimeNanos);
        } else {
            groupDescStats = null;
            groupStats = null;
        }
        if (meterStatsService != null) {
            meterConfigStats = new MeterConfigStatsTracker(meterStatsService, this, lifetimeNanos);
            meterStats = new MeterStatsTracker(meterStatsService, this, lifetimeNanos);
        } else {
            meterConfigStats = null;
            meterStats = null;
        }
        if (portStatsService != null) {
            nodeConnectorStats = new NodeConnectorStatsTracker(portStatsService, this, lifetimeNanos);
        } else {
            nodeConnectorStats = null;
        }
        if (queueStatsService != null) {
            queueStats = new QueueStatsTracker(queueStatsService, this, lifetimeNanos);
        } else {
            queueStats = null;
        }
    }

    public NodeKey getTargetNodeKey() {
        return targetNodeKey;
    }

    @Override
    public InstanceIdentifier<Node> getNodeIdentifier() {
        return targetNodeIdentifier;
    }

    @Override
    public NodeRef getNodeRef() {
        return targetNodeRef;
    }

    @Override
    public DataModificationTransaction startDataModification() {
        return dps.beginTransaction();
    }

    public synchronized void updateGroupDescStats(TransactionAware transaction, Boolean more, List<GroupDescStats> list) {
        if (msgManager.isExpectedTransaction(transaction, more)) {
            groupDescStats.updateStats(list);
        }
    }

    public synchronized void updateGroupStats(TransactionAware transaction, Boolean more, List<GroupStats> list) {
        if (msgManager.isExpectedTransaction(transaction, more)) {
            groupStats.updateStats(list);
        }
    }

    public synchronized void updateMeterConfigStats(TransactionAware transaction, Boolean more, List<MeterConfigStats> list) {
        if (msgManager.isExpectedTransaction(transaction, more)) {
            meterConfigStats.updateStats(list);
        }
    }

    public synchronized void updateMeterStats(TransactionAware transaction, Boolean more, List<MeterStats> list) {
        if (msgManager.isExpectedTransaction(transaction, more)) {
            meterStats.updateStats(list);
        }
    }

    public synchronized void updateQueueStats(TransactionAware transaction, Boolean more, List<QueueIdAndStatisticsMap> list) {
        if (msgManager.isExpectedTransaction(transaction, more)) {
            queueStats.updateStats(list);
        }
    }

    public synchronized void updateFlowTableStats(TransactionAware transaction, Boolean more, List<FlowTableAndStatisticsMap> list) {
        if (msgManager.isExpectedTransaction(transaction, more)) {
            flowTableStats.updateStats(list);
        }
    }

    public synchronized void updateNodeConnectorStats(TransactionAware transaction, Boolean more, List<NodeConnectorStatisticsAndPortNumberMap> list) {
        if (msgManager.isExpectedTransaction(transaction, more)) {
            nodeConnectorStats.updateStats(list);
        }
    }

    public synchronized void updateAggregateFlowStats(TransactionAware transaction, Boolean more, AggregateFlowStatistics flowStats) {
        final Short tableId = msgManager.isExpectedTableTransaction(transaction, more);
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

            trans.commit();
        }
    }

    public synchronized void updateFlowStats(TransactionAware transaction, Boolean more, List<FlowAndStatisticsMapList> list) {
        if (msgManager.isExpectedTransaction(transaction, more)) {
            flowStats.updateStats(list);
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
        msgManager.cleanStaleTransactionIds();

        trans.commit();
    }

    public synchronized void requestPeriodicStatistics() {
        logger.debug("Send requests for statistics collection to node : {}", targetNodeKey);

        if (flowTableStats != null){
            registerTransaction(flowTableStats.request(), StatsRequestType.ALL_FLOW);
        }
        if (flowStats != null){
            // FIXME: it does not make sense to trigger this before sendAllFlowTablesStatisticsRequest()
            //        comes back -- we do not have any tables anyway.
            final Collection<TableKey> tables = flowTableStats.getTables();
            logger.debug("Node {} supports {} table(s)", targetNodeKey, tables.size());
            for (final TableKey key : tables) {
                logger.debug("Send aggregate stats request for flow table {} to node {}", key.getId(), targetNodeKey);
                registerTableTransaction(flowStats.requestAggregateFlows(key),  key.getId());
            }

            registerTransaction(flowStats.requestAllFlowsAllTables(), StatsRequestType.ALL_FLOW);
        }

        if (nodeConnectorStats != null) {
            registerTransaction(nodeConnectorStats.request(), StatsRequestType.ALL_PORT);
        }

        if (groupStats != null) {
            registerTransaction(groupStats.request(), StatsRequestType.ALL_GROUP);
        }
        sendGroupDescriptionRequest();

        if (meterStats != null) {
            registerTransaction(meterStats.request(), StatsRequestType.ALL_METER);
        }
        sendMeterConfigStatisticsRequest();

        if(queueStats != null) {
            registerTransaction(queueStats.request(), StatsRequestType.ALL_QUEUE_STATS);
        }
    }

    public synchronized void start() {
        requestPeriodicStatistics();
    }

    @Override
    public synchronized void close() {
        // FIXME: cleanup any resources we hold (registrations, etc.)
        logger.debug("Statistics handler for {} shut down", targetNodeKey.getId());
    }

    synchronized void sendFlowStatsFromTableRequest(Flow flow) {
        if (flowStats == null) {
            logger.debug("No Flow statistics service, not sending a request");
            return;
        }

        registerTransaction(flowStats.requestFlow(flow), StatsRequestType.ALL_FLOW);
    }

    synchronized void sendGroupDescriptionRequest() {
        if (groupStats == null) {
            logger.debug("No Group Descriptor statistics service, not sending a request");
            return;
        }

        registerTransaction(groupDescStats.request(), StatsRequestType.GROUP_DESC);
    }

    synchronized void sendMeterConfigStatisticsRequest() {
        if (meterConfigStats == null) {
            logger.debug("No Meter Config statistics service, not sending a request");
            return;
        }

        registerTransaction(meterConfigStats.request(), StatsRequestType.METER_CONFIG);
    }

    synchronized void sendQueueStatsFromGivenNodeConnector(NodeConnectorId nodeConnectorId, QueueId queueId) {
        if (queueStats == null) {
            logger.debug("No Queue statistics service, not sending a request");
            return;
        }

        registerTransaction(queueStats.request(nodeConnectorId, queueId), StatsRequestType.ALL_QUEUE_STATS);
    }

    private void registerTransaction(final ListenableFuture<TransactionId> future, final StatsRequestType type) {
        Futures.addCallback(future, new FutureCallback<TransactionId>() {
            @Override
            public void onSuccess(TransactionId result) {
                msgManager.recordExpectedTransaction(result, type);
                logger.debug("Transaction {} for node {} sent successfully", result, targetNodeKey);
            }

            @Override
            public void onFailure(Throwable t) {
                logger.warn("Failed to send statistics request for node {}", targetNodeKey, t);
            }
        });
    }

    private void registerTableTransaction(final ListenableFuture<TransactionId> future, final Short id) {
        Futures.addCallback(future, new FutureCallback<TransactionId>() {
            @Override
            public void onSuccess(TransactionId result) {
                msgManager.recordExpectedTableTransaction(result, StatsRequestType.AGGR_FLOW, id);
                logger.debug("Transaction {} for node {} table {} sent successfully", result, targetNodeKey, id);
            }

            @Override
            public void onFailure(Throwable t) {
                logger.warn("Failed to send table statistics request for node {} table {}", targetNodeKey, id, t);
            }
        });
    }
}
