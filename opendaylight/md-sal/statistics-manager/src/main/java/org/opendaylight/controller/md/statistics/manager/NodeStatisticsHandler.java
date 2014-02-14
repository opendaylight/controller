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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.aggregate.flow.statistics.AggregateFlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.queue.rev130925.QueueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionOutput;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.AggregateFlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetQueueStatisticsFromGivenPortInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetQueueStatisticsFromGivenPortOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.queue.id.and.statistics.map.QueueIdAndStatisticsMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
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

    private final OpendaylightFlowStatisticsService flowStatsService;
    private final OpendaylightFlowTableStatisticsService flowTableStatsService;
    private final OpendaylightGroupStatisticsService groupStatsService;
    private final OpendaylightMeterStatisticsService meterStatsService;
    private final OpendaylightPortStatisticsService portStatsService;
    private final OpendaylightQueueStatisticsService queueStatsService;

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

        this.flowStatsService = flowStatsService;
        this.flowTableStatsService = flowTableStatsService;
        this.groupStatsService = groupStatsService;
        this.meterStatsService = meterStatsService;
        this.portStatsService = portStatsService;
        this.queueStatsService = queueStatsService;

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

        try{
            if(flowTableStatsService != null){
                final GetFlowTablesStatisticsInputBuilder input = new GetFlowTablesStatisticsInputBuilder();
                input.setNode(targetNodeRef);

                Future<RpcResult<GetFlowTablesStatisticsOutput>> response = flowTableStatsService.getFlowTablesStatistics(input.build());
                recordExpectedTransaction(response.get().getResult().getTransactionId(), StatsRequestType.ALL_FLOW_TABLE);
            }
            if(flowStatsService != null){
                // FIXME: it does not make sense to trigger this before sendAllFlowTablesStatisticsRequest()
                //        comes back -- we do not have any tables anyway.
                sendAggregateFlowsStatsFromAllTablesRequest();

                sendAllFlowsStatsFromAllTablesRequest();
            }
            if(portStatsService != null){
                sendAllNodeConnectorsStatisticsRequest();
            }
            if(groupStatsService != null){
                sendAllGroupStatisticsRequest();
                sendGroupDescriptionRequest();
            }
            if(meterStatsService != null){
                sendAllMeterStatisticsRequest();
                sendMeterConfigStatisticsRequest();
            }
            if(queueStatsService != null){
                sendAllQueueStatsFromAllNodeConnector();
            }
        } catch(Exception e) {
            logger.error("Exception occured while sending statistics requests", e);
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

    synchronized void sendFlowStatsFromTableRequest(Flow flow) throws InterruptedException, ExecutionException{
        final GetFlowStatisticsFromFlowTableInputBuilder input =
                new GetFlowStatisticsFromFlowTableInputBuilder(flow);

        input.setNode(targetNodeRef);

        Future<RpcResult<GetFlowStatisticsFromFlowTableOutput>> response =
                flowStatsService.getFlowStatisticsFromFlowTable(input.build());

        recordExpectedTransaction(response.get().getResult().getTransactionId(), StatsRequestType.ALL_FLOW);
    }

    synchronized void sendGroupDescriptionRequest() throws InterruptedException, ExecutionException{
        final GetGroupDescriptionInputBuilder input = new GetGroupDescriptionInputBuilder();

        input.setNode(targetNodeRef);

        Future<RpcResult<GetGroupDescriptionOutput>> response =
                groupStatsService.getGroupDescription(input.build());

        recordExpectedTransaction(response.get().getResult().getTransactionId(), StatsRequestType.GROUP_DESC);
    }

    synchronized void sendMeterConfigStatisticsRequest() throws InterruptedException, ExecutionException{

        GetAllMeterConfigStatisticsInputBuilder input = new GetAllMeterConfigStatisticsInputBuilder();

        input.setNode(targetNodeRef);

        Future<RpcResult<GetAllMeterConfigStatisticsOutput>> response =
                meterStatsService.getAllMeterConfigStatistics(input.build());

        recordExpectedTransaction(response.get().getResult().getTransactionId(), StatsRequestType.METER_CONFIG);
    }

    synchronized void sendQueueStatsFromGivenNodeConnector(NodeConnectorId nodeConnectorId, QueueId queueId) throws InterruptedException, ExecutionException {
        GetQueueStatisticsFromGivenPortInputBuilder input = new GetQueueStatisticsFromGivenPortInputBuilder();

        input.setNode(targetNodeRef);
        input.setNodeConnectorId(nodeConnectorId);
        input.setQueueId(queueId);
        Future<RpcResult<GetQueueStatisticsFromGivenPortOutput>> response =
                queueStatsService.getQueueStatisticsFromGivenPort(input.build());

        recordExpectedTransaction(response.get().getResult().getTransactionId(), StatsRequestType.ALL_QUEUE_STATS);;
    }

    private void sendAllMeterStatisticsRequest() throws InterruptedException, ExecutionException{

        GetAllMeterStatisticsInputBuilder input = new GetAllMeterStatisticsInputBuilder();

        input.setNode(targetNodeRef);

        Future<RpcResult<GetAllMeterStatisticsOutput>> response =
                meterStatsService.getAllMeterStatistics(input.build());

        recordExpectedTransaction(response.get().getResult().getTransactionId(), StatsRequestType.ALL_METER);
    }

    private void sendAllFlowsStatsFromAllTablesRequest() throws InterruptedException, ExecutionException{
        final GetAllFlowsStatisticsFromAllFlowTablesInputBuilder input = new GetAllFlowsStatisticsFromAllFlowTablesInputBuilder();
        input.setNode(targetNodeRef);

        Future<RpcResult<GetAllFlowsStatisticsFromAllFlowTablesOutput>> response = flowStatsService.getAllFlowsStatisticsFromAllFlowTables(input.build());

        recordExpectedTransaction(response.get().getResult().getTransactionId(), StatsRequestType.ALL_FLOW);
    }

    private void sendAggregateFlowsStatsFromAllTablesRequest() throws InterruptedException, ExecutionException{
        final Collection<TableKey> tables = getKnownTables();
        logger.debug("Node {} supports {} table(s)", targetNodeKey, tables.size());

        for (TableKey key : tables) {
            sendAggregateFlowsStatsFromTableRequest(key.getId().shortValue());
        }
    }

    private void sendAggregateFlowsStatsFromTableRequest(Short tableId) throws InterruptedException, ExecutionException{
        logger.debug("Send aggregate stats request for flow table {} to node {}",tableId, targetNodeKey);
        GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder input =
                new GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder();

        input.setNode(new NodeRef(InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey).toInstance()));
        input.setTableId(new org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId(tableId));
        Future<RpcResult<GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput>> response =
                flowStatsService.getAggregateFlowStatisticsFromFlowTableForAllFlows(input.build());

        recordExpectedTableTransaction(response.get().getResult().getTransactionId(), tableId);
    }

    private void sendAllQueueStatsFromAllNodeConnector() throws InterruptedException, ExecutionException {
        GetAllQueuesStatisticsFromAllPortsInputBuilder input = new GetAllQueuesStatisticsFromAllPortsInputBuilder();

        input.setNode(targetNodeRef);

        Future<RpcResult<GetAllQueuesStatisticsFromAllPortsOutput>> response =
                queueStatsService.getAllQueuesStatisticsFromAllPorts(input.build());

        recordExpectedTransaction(response.get().getResult().getTransactionId(), StatsRequestType.ALL_QUEUE_STATS);
    }

    private void sendAllNodeConnectorsStatisticsRequest() throws InterruptedException, ExecutionException{
        final GetAllNodeConnectorsStatisticsInputBuilder input = new GetAllNodeConnectorsStatisticsInputBuilder();

        input.setNode(targetNodeRef);

        Future<RpcResult<GetAllNodeConnectorsStatisticsOutput>> response =
                portStatsService.getAllNodeConnectorsStatistics(input.build());
        recordExpectedTransaction(response.get().getResult().getTransactionId(), StatsRequestType.ALL_PORT);
    }

    private void sendAllGroupStatisticsRequest() throws InterruptedException, ExecutionException{
        final GetAllGroupStatisticsInputBuilder input = new GetAllGroupStatisticsInputBuilder();
        input.setNode(targetNodeRef);

        Future<RpcResult<GetAllGroupStatisticsOutput>> response =
                groupStatsService.getAllGroupStatistics(input.build());

        recordExpectedTransaction(response.get().getResult().getTransactionId(), StatsRequestType.ALL_GROUP);
    }

    private void recordExpectedTransaction(TransactionId transactionId, StatsRequestType reqType) {
        msgManager.recordExpectedTransaction(transactionId, reqType);
    }

    private void recordExpectedTableTransaction(TransactionId transactionId, Short tableId) {
        msgManager.recordExpectedTableTransaction(transactionId, StatsRequestType.AGGR_FLOW, tableId);
    }
}
