/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.statistics.manager.MultipartMessageManager.StatsRequestType;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.queue.rev130925.QueueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetQueueStatisticsFromGivenPortInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetQueueStatisticsFromGivenPortOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Following are main responsibilities of the class:
 * 1) Invoke statistics request thread to send periodic statistics request to all the
 * flow capable switch connected to the controller. It sends statistics request for
 * Group,Meter,Table,Flow,Queue,Aggregate stats.
 *
 * 2) Invoke statistics ager thread, to clean up all the stale statistics data from
 * operational data store.
 *
 * @author avishnoi@in.ibm.com
 *
 */
public class StatisticsProvider implements AutoCloseable {
    public static final long STATS_COLLECTION_MILLIS = TimeUnit.SECONDS.toMillis(15);

    private static final Logger spLogger = LoggerFactory.getLogger(StatisticsProvider.class);

    private final ConcurrentMap<NodeId, NodeStatisticsHandler> handlers = new ConcurrentHashMap<>();
    private final MultipartMessageManager multipartMessageManager = new MultipartMessageManager();
    private final Timer timer = new Timer("statistics-manager", true);
    private final DataProviderService dps;

    private OpendaylightGroupStatisticsService groupStatsService;

    private OpendaylightMeterStatisticsService meterStatsService;

    private OpendaylightFlowStatisticsService flowStatsService;

    private OpendaylightPortStatisticsService portStatsService;

    private OpendaylightFlowTableStatisticsService flowTableStatsService;

    private OpendaylightQueueStatisticsService queueStatsService;

    private StatisticsUpdateHandler statsUpdateHandler;

    public StatisticsProvider(final DataProviderService dataService) {
        this.dps = Preconditions.checkNotNull(dataService);
    }

    public MultipartMessageManager getMultipartMessageManager() {
        return multipartMessageManager;
    }

    private final StatisticsListener updateCommiter = new StatisticsListener(StatisticsProvider.this);

    private Registration<NotificationListener> listenerRegistration;

    private ListenerRegistration<DataChangeListener> flowCapableTrackerRegistration;

    public void start(final DataBrokerService dbs, final NotificationProviderService nps, final RpcConsumerRegistry rpcRegistry) {

        // Get Group/Meter statistics service instances
        groupStatsService = rpcRegistry.getRpcService(OpendaylightGroupStatisticsService.class);
        meterStatsService = rpcRegistry.getRpcService(OpendaylightMeterStatisticsService.class);
        flowStatsService = rpcRegistry.getRpcService(OpendaylightFlowStatisticsService.class);
        portStatsService = rpcRegistry.getRpcService(OpendaylightPortStatisticsService.class);
        flowTableStatsService = rpcRegistry.getRpcService(OpendaylightFlowTableStatisticsService.class);
        queueStatsService = rpcRegistry.getRpcService(OpendaylightQueueStatisticsService.class);

        // Start receiving notifications
        this.listenerRegistration = nps.registerNotificationListener(this.updateCommiter);

        // Register for switch connect/disconnect notifications
        final InstanceIdentifier<FlowCapableNode> fcnId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class).augmentation(FlowCapableNode.class).build();
        spLogger.debug("Registering FlowCapable tracker to {}", fcnId);
        this.flowCapableTrackerRegistration = dbs.registerDataChangeListener(fcnId,
                new FlowCapableTracker(this, fcnId));

        statsUpdateHandler = new StatisticsUpdateHandler(StatisticsProvider.this);
        registerDataStoreUpdateListener(dbs);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    // Send stats requests
                    statsRequestSender();

                    // Perform cleanup
                    for(NodeStatisticsHandler nodeStatisticsAger : handlers.values()){
                        nodeStatisticsAger.cleanStaleStatistics();
                    }

                    multipartMessageManager.cleanStaleTransactionIds();
                } catch (RuntimeException e) {
                    spLogger.warn("Failed to request statistics", e);
                }
            }
        }, 0, STATS_COLLECTION_MILLIS);

        spLogger.debug("Statistics timer task with timer interval : {}ms", STATS_COLLECTION_MILLIS);
        spLogger.info("Statistics Provider started.");
    }

    private void registerDataStoreUpdateListener(DataBrokerService dbs) {
        // FIXME: the below should be broken out into StatisticsUpdateHandler

        //Register for flow updates
        InstanceIdentifier<? extends DataObject> pathFlow = InstanceIdentifier.builder(Nodes.class).child(Node.class)
                                                                    .augmentation(FlowCapableNode.class)
                                                                    .child(Table.class)
                                                                    .child(Flow.class).toInstance();
        dbs.registerDataChangeListener(pathFlow, statsUpdateHandler);

        //Register for meter updates
        InstanceIdentifier<? extends DataObject> pathMeter = InstanceIdentifier.builder(Nodes.class).child(Node.class)
                                                    .augmentation(FlowCapableNode.class)
                                                    .child(Meter.class).toInstance();

        dbs.registerDataChangeListener(pathMeter, statsUpdateHandler);

        //Register for group updates
        InstanceIdentifier<? extends DataObject> pathGroup = InstanceIdentifier.builder(Nodes.class).child(Node.class)
                                                    .augmentation(FlowCapableNode.class)
                                                    .child(Group.class).toInstance();
        dbs.registerDataChangeListener(pathGroup, statsUpdateHandler);

        //Register for queue updates
        InstanceIdentifier<? extends DataObject> pathQueue = InstanceIdentifier.builder(Nodes.class).child(Node.class)
                                                                    .child(NodeConnector.class)
                                                                    .augmentation(FlowCapableNodeConnector.class)
                                                                    .child(Queue.class).toInstance();
        dbs.registerDataChangeListener(pathQueue, statsUpdateHandler);
    }

    protected DataModificationTransaction startChange() {
        return dps.beginTransaction();
    }

    private void statsRequestSender() {
        for (NodeStatisticsHandler h : handlers.values()) {
            sendStatisticsRequestsToNode(h);
        }
    }

    private void sendStatisticsRequestsToNode(final NodeStatisticsHandler h) {
        NodeKey targetNode = h.getTargetNodeKey();
        spLogger.debug("Send requests for statistics collection to node : {}", targetNode.getId());

        try{
            if(flowTableStatsService != null){
                sendAllFlowTablesStatisticsRequest(h);
            }
            if(flowStatsService != null){
                // FIXME: it does not make sense to trigger this before sendAllFlowTablesStatisticsRequest()
                //        comes back -- we do not have any tables anyway.
                sendAggregateFlowsStatsFromAllTablesRequest(h);

                sendAllFlowsStatsFromAllTablesRequest(h);
            }
            if(portStatsService != null){
                sendAllNodeConnectorsStatisticsRequest(h);
            }
            if(groupStatsService != null){
                sendAllGroupStatisticsRequest(h);
                sendGroupDescriptionRequest(h);
            }
            if(meterStatsService != null){
                sendAllMeterStatisticsRequest(h);
                sendMeterConfigStatisticsRequest(h);
            }
            if(queueStatsService != null){
                sendAllQueueStatsFromAllNodeConnector(h);
            }
        }catch(Exception e){
            spLogger.error("Exception occured while sending statistics requests : {}", e);
        }
    }


    private void sendAllFlowTablesStatisticsRequest(NodeStatisticsHandler h) throws InterruptedException, ExecutionException {
        final GetFlowTablesStatisticsInputBuilder input =
                new GetFlowTablesStatisticsInputBuilder();

        input.setNode(h.getTargetNodeRef());

        Future<RpcResult<GetFlowTablesStatisticsOutput>> response =
                flowTableStatsService.getFlowTablesStatistics(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(h.getTargetNodeKey().getId(),response.get().getResult().getTransactionId()
                , StatsRequestType.ALL_FLOW_TABLE);

    }

    private void sendAllFlowsStatsFromAllTablesRequest(NodeStatisticsHandler h) throws InterruptedException, ExecutionException{
        final GetAllFlowsStatisticsFromAllFlowTablesInputBuilder input =
                new GetAllFlowsStatisticsFromAllFlowTablesInputBuilder();

        input.setNode(h.getTargetNodeRef());

        Future<RpcResult<GetAllFlowsStatisticsFromAllFlowTablesOutput>> response =
                flowStatsService.getAllFlowsStatisticsFromAllFlowTables(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(h.getTargetNodeKey().getId(), response.get().getResult().getTransactionId()
                , StatsRequestType.ALL_FLOW);

    }

    public void sendFlowStatsFromTableRequest(NodeKey node, Flow flow) throws InterruptedException, ExecutionException {
        final NodeStatisticsHandler h = getStatisticsHandler(node.getId());
        if (h != null) {
            sendFlowStatsFromTableRequest(h, flow);
        }
    }

    private void sendFlowStatsFromTableRequest(NodeStatisticsHandler h, Flow flow) throws InterruptedException, ExecutionException{
        final GetFlowStatisticsFromFlowTableInputBuilder input =
                new GetFlowStatisticsFromFlowTableInputBuilder();

        input.setNode(h.getTargetNodeRef());
        input.fieldsFrom(flow);

        Future<RpcResult<GetFlowStatisticsFromFlowTableOutput>> response =
                flowStatsService.getFlowStatisticsFromFlowTable(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(h.getTargetNodeKey().getId(),
                response.get().getResult().getTransactionId(), StatsRequestType.ALL_FLOW);
    }

    private void sendAggregateFlowsStatsFromAllTablesRequest(final NodeStatisticsHandler h) throws InterruptedException, ExecutionException{
        final Collection<TableKey> tables = h.getKnownTables();
        spLogger.debug("Node {} supports {} table(s)", h, tables.size());

        for (TableKey key : h.getKnownTables()) {
            sendAggregateFlowsStatsFromTableRequest(h.getTargetNodeKey(), key.getId().shortValue());
        }
    }

    private void sendAggregateFlowsStatsFromTableRequest(NodeKey targetNodeKey,Short tableId) throws InterruptedException, ExecutionException{

        spLogger.debug("Send aggregate stats request for flow table {} to node {}",tableId,targetNodeKey);
        GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder input =
                new GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder();

        input.setNode(new NodeRef(InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey).toInstance()));
        input.setTableId(new org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId(tableId));
        Future<RpcResult<GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput>> response =
                flowStatsService.getAggregateFlowStatisticsFromFlowTableForAllFlows(input.build());

        multipartMessageManager.setTxIdAndTableIdMapEntry(targetNodeKey.getId(), response.get().getResult().getTransactionId(), tableId);
        this.multipartMessageManager.addTxIdToRequestTypeEntry(targetNodeKey.getId(), response.get().getResult().getTransactionId()
                , StatsRequestType.AGGR_FLOW);
    }

    private void sendAllNodeConnectorsStatisticsRequest(NodeStatisticsHandler h) throws InterruptedException, ExecutionException{

        final GetAllNodeConnectorsStatisticsInputBuilder input = new GetAllNodeConnectorsStatisticsInputBuilder();

        input.setNode(h.getTargetNodeRef());

        Future<RpcResult<GetAllNodeConnectorsStatisticsOutput>> response =
                portStatsService.getAllNodeConnectorsStatistics(input.build());
        this.multipartMessageManager.addTxIdToRequestTypeEntry(h.getTargetNodeKey().getId(), response.get().getResult().getTransactionId()
                , StatsRequestType.ALL_PORT);

    }

    private void sendAllGroupStatisticsRequest(NodeStatisticsHandler h) throws InterruptedException, ExecutionException{

        final GetAllGroupStatisticsInputBuilder input = new GetAllGroupStatisticsInputBuilder();

        input.setNode(h.getTargetNodeRef());

        Future<RpcResult<GetAllGroupStatisticsOutput>> response =
                groupStatsService.getAllGroupStatistics(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(h.getTargetNodeKey().getId(), response.get().getResult().getTransactionId()
                , StatsRequestType.ALL_GROUP);

    }

    public void sendGroupDescriptionRequest(NodeKey node) throws InterruptedException, ExecutionException{
        final NodeStatisticsHandler h = getStatisticsHandler(node.getId());
        if (h != null) {
            sendGroupDescriptionRequest(h);
        }
    }

    private void sendGroupDescriptionRequest(NodeStatisticsHandler h) throws InterruptedException, ExecutionException{
        final GetGroupDescriptionInputBuilder input = new GetGroupDescriptionInputBuilder();

        input.setNode(h.getTargetNodeRef());

        Future<RpcResult<GetGroupDescriptionOutput>> response =
                groupStatsService.getGroupDescription(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(h.getTargetNodeKey().getId(),
                response.get().getResult().getTransactionId(), StatsRequestType.GROUP_DESC);
    }

    private void sendAllMeterStatisticsRequest(NodeStatisticsHandler h) throws InterruptedException, ExecutionException{

        GetAllMeterStatisticsInputBuilder input = new GetAllMeterStatisticsInputBuilder();

        input.setNode(h.getTargetNodeRef());

        Future<RpcResult<GetAllMeterStatisticsOutput>> response =
                meterStatsService.getAllMeterStatistics(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(h.getTargetNodeKey().getId(), response.get().getResult().getTransactionId()
                , StatsRequestType.ALL_METER);;

    }

    public void sendMeterConfigStatisticsRequest(NodeKey node) throws InterruptedException, ExecutionException {
        final NodeStatisticsHandler h = getStatisticsHandler(node.getId());
        if (h != null) {
            sendMeterConfigStatisticsRequest(h);
        }
    }

    private void sendMeterConfigStatisticsRequest(NodeStatisticsHandler h) throws InterruptedException, ExecutionException{

        GetAllMeterConfigStatisticsInputBuilder input = new GetAllMeterConfigStatisticsInputBuilder();

        input.setNode(h.getTargetNodeRef());

        Future<RpcResult<GetAllMeterConfigStatisticsOutput>> response =
                meterStatsService.getAllMeterConfigStatistics(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(h.getTargetNodeKey().getId(),
                response.get().getResult().getTransactionId(), StatsRequestType.METER_CONFIG);;
    }

    private void sendAllQueueStatsFromAllNodeConnector(NodeStatisticsHandler h) throws InterruptedException, ExecutionException {
        GetAllQueuesStatisticsFromAllPortsInputBuilder input = new GetAllQueuesStatisticsFromAllPortsInputBuilder();

        input.setNode(h.getTargetNodeRef());

        Future<RpcResult<GetAllQueuesStatisticsFromAllPortsOutput>> response =
                queueStatsService.getAllQueuesStatisticsFromAllPorts(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(h.getTargetNodeKey().getId(),
                response.get().getResult().getTransactionId(), StatsRequestType.ALL_QUEUE_STATS);;
    }

    public void sendQueueStatsFromGivenNodeConnector(NodeKey node,NodeConnectorId nodeConnectorId, QueueId queueId) throws InterruptedException, ExecutionException {
        final NodeStatisticsHandler h = getStatisticsHandler(node.getId());
        if (h != null) {
            sendQueueStatsFromGivenNodeConnector(h, nodeConnectorId, queueId);
        }
    }

    private void sendQueueStatsFromGivenNodeConnector(NodeStatisticsHandler h, NodeConnectorId nodeConnectorId, QueueId queueId) throws InterruptedException, ExecutionException {
        GetQueueStatisticsFromGivenPortInputBuilder input = new GetQueueStatisticsFromGivenPortInputBuilder();

        input.setNode(h.getTargetNodeRef());
        input.setNodeConnectorId(nodeConnectorId);
        input.setQueueId(queueId);
        Future<RpcResult<GetQueueStatisticsFromGivenPortOutput>> response =
                queueStatsService.getQueueStatisticsFromGivenPort(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(h.getTargetNodeKey().getId(),
                response.get().getResult().getTransactionId(), StatsRequestType.ALL_QUEUE_STATS);;
    }

    /**
     * Get the handler for a particular node.
     *
     * @param nodeId source node
     * @return Node statistics handler for that node. Null if the statistics should
     *         not handled.
     */
    public final NodeStatisticsHandler getStatisticsHandler(final NodeId nodeId) {
        Preconditions.checkNotNull(nodeId);
        NodeStatisticsHandler handler = handlers.get(nodeId);
        if (handler == null) {
            spLogger.info("Attempted to get non-existing handler for {}", nodeId);
        }
        return handler;
    }

    @Override
    public void close() {
        try {
            if (this.listenerRegistration != null) {
                this.listenerRegistration.close();
                this.listenerRegistration = null;
            }
            if (this.flowCapableTrackerRegistration != null) {
                this.flowCapableTrackerRegistration.close();
                this.flowCapableTrackerRegistration = null;
            }
            timer.cancel();
        } catch (Exception e) {
            spLogger.warn("Failed to stop Statistics Provider completely", e);
        } finally {
            spLogger.info("Statistics Provider stopped.");
        }
    }

    void startNodeHandlers(final Collection<NodeKey> addedNodes) {
        for (NodeKey key : addedNodes) {
            if (handlers.containsKey(key.getId())) {
                spLogger.warn("Attempted to start already-existing handler for {}, very strange", key.getId());
                continue;
            }

            final NodeStatisticsHandler h = new NodeStatisticsHandler(dps, key);
            final NodeStatisticsHandler old = handlers.putIfAbsent(key.getId(), h);
            if (old == null) {
                spLogger.debug("Started node handler for {}", key.getId());

                // FIXME: this should be in the NodeStatisticsHandler itself
                sendStatisticsRequestsToNode(h);
            } else {
                spLogger.debug("Prevented race on handler for {}", key.getId());
            }
        }
    }

    void stopNodeHandlers(final Collection<NodeKey> removedNodes) {
        for (NodeKey key : removedNodes) {
            final NodeStatisticsHandler s = handlers.remove(key.getId());
            if (s != null) {
                spLogger.debug("Stopping node handler for {}", key.getId());
                s.close();
            } else {
                spLogger.warn("Attempted to remove non-existing handler for {}, very strange", key.getId());
            }
        }
    }
}
