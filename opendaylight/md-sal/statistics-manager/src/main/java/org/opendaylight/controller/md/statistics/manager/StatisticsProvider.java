/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.xtext.xbase.lib.Exceptions;
import org.opendaylight.controller.md.statistics.manager.MultipartMessageManager.StatsRequestType;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
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
    public static final int STATS_THREAD_EXECUTION_TIME= 15000;

    private static final Logger spLogger = LoggerFactory.getLogger(StatisticsProvider.class);

    private final MultipartMessageManager multipartMessageManager = new MultipartMessageManager();
    private final InstanceIdentifier<Nodes> nodesIdentifier = InstanceIdentifier.builder(Nodes.class).toInstance();
    private final DataProviderService dps;

    //Local caching of stats
    private final ConcurrentMap<NodeId,NodeStatisticsHandler> statisticsCache = new ConcurrentHashMap<>();

    private OpendaylightGroupStatisticsService groupStatsService;

    private OpendaylightMeterStatisticsService meterStatsService;

    private OpendaylightFlowStatisticsService flowStatsService;

    private OpendaylightPortStatisticsService portStatsService;

    private OpendaylightFlowTableStatisticsService flowTableStatsService;

    private OpendaylightQueueStatisticsService queueStatsService;

    private StatisticsUpdateHandler statsUpdateHandler;

    private Thread statisticsRequesterThread;

    private Thread statisticsAgerThread;


    public StatisticsProvider(final DataProviderService dataService) {
        this.dps = Preconditions.checkNotNull(dataService);
    }

    public MultipartMessageManager getMultipartMessageManager() {
        return multipartMessageManager;
    }

    private final StatisticsListener updateCommiter = new StatisticsListener(StatisticsProvider.this);

    private Registration<NotificationListener> listenerRegistration;

    public void start(final DataBrokerService dbs, final NotificationProviderService nps, final RpcConsumerRegistry rpcRegistry) {

        this.listenerRegistration = nps.registerNotificationListener(this.updateCommiter);

        statsUpdateHandler = new StatisticsUpdateHandler(StatisticsProvider.this);
        registerDataStoreUpdateListener(dbs);

        // Get Group/Meter statistics service instance
        groupStatsService = rpcRegistry.getRpcService(OpendaylightGroupStatisticsService.class);
        meterStatsService = rpcRegistry.getRpcService(OpendaylightMeterStatisticsService.class);
        flowStatsService = rpcRegistry.getRpcService(OpendaylightFlowStatisticsService.class);
        portStatsService = rpcRegistry.getRpcService(OpendaylightPortStatisticsService.class);
        flowTableStatsService = rpcRegistry.getRpcService(OpendaylightFlowTableStatisticsService.class);
        queueStatsService = rpcRegistry.getRpcService(OpendaylightQueueStatisticsService.class);

        statisticsRequesterThread = new Thread( new Runnable(){

            @Override
            public void run() {
                while(true){
                    try {
                        statsRequestSender();

                        Thread.sleep(STATS_THREAD_EXECUTION_TIME);
                    }catch (Exception e){
                        spLogger.error("Exception occurred while sending stats request : {}",e);
                    }
                }
            }
        });

        spLogger.debug("Statistics requester thread started with timer interval : {}",STATS_THREAD_EXECUTION_TIME);

        statisticsRequesterThread.start();

        statisticsAgerThread = new Thread( new Runnable(){

            @Override
            public void run() {
                while(true){
                    try {
                        for(NodeStatisticsHandler nodeStatisticsAger : statisticsCache.values()){
                            nodeStatisticsAger.cleanStaleStatistics();
                        }
                        multipartMessageManager.cleanStaleTransactionIds();

                        Thread.sleep(STATS_THREAD_EXECUTION_TIME);
                    }catch (Exception e){
                        spLogger.error("Exception occurred while sending stats request : {}",e);
                    }
                }
            }
        });

        spLogger.debug("Statistics ager thread started with timer interval : {}",STATS_THREAD_EXECUTION_TIME);

        statisticsAgerThread.start();

        spLogger.info("Statistics Provider started.");
    }

    private void registerDataStoreUpdateListener(DataBrokerService dbs) {
        //Register for Node updates
        InstanceIdentifier<? extends DataObject> pathNode = InstanceIdentifier.builder(Nodes.class)
                                                                        .child(Node.class).toInstance();
        dbs.registerDataChangeListener(pathNode, statsUpdateHandler);

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

    private void statsRequestSender(){

        List<Node> targetNodes = getAllConnectedNodes();

        if(targetNodes == null)
            return;


        for (Node targetNode : targetNodes){

            if(targetNode.getAugmentation(FlowCapableNode.class) != null){
                sendStatisticsRequestsToNode(targetNode);
            }
        }
    }

    public void sendStatisticsRequestsToNode(Node targetNode){

        spLogger.debug("Send requests for statistics collection to node : {})",targetNode.getId());

        InstanceIdentifier<Node> targetInstanceId = InstanceIdentifier.builder(Nodes.class).child(Node.class,targetNode.getKey()).toInstance();

        NodeRef targetNodeRef = new NodeRef(targetInstanceId);

        try{
            if(flowStatsService != null){
                sendAggregateFlowsStatsFromAllTablesRequest(targetNode.getKey());
                sendAllFlowsStatsFromAllTablesRequest(targetNodeRef);
            }
            if(flowTableStatsService != null){
                sendAllFlowTablesStatisticsRequest(targetNodeRef);
            }
            if(portStatsService != null){
                sendAllNodeConnectorsStatisticsRequest(targetNodeRef);
            }
            if(groupStatsService != null){
                sendAllGroupStatisticsRequest(targetNodeRef);
                sendGroupDescriptionRequest(targetNodeRef);
            }
            if(meterStatsService != null){
                sendAllMeterStatisticsRequest(targetNodeRef);
                sendMeterConfigStatisticsRequest(targetNodeRef);
            }
            if(queueStatsService != null){
                sendAllQueueStatsFromAllNodeConnector (targetNodeRef);
            }
        }catch(Exception e){
            spLogger.error("Exception occured while sending statistics requests : {}", e);
        }
    }


    public void sendAllFlowTablesStatisticsRequest(NodeRef targetNodeRef) throws InterruptedException, ExecutionException {
        final GetFlowTablesStatisticsInputBuilder input =
                new GetFlowTablesStatisticsInputBuilder();

        input.setNode(targetNodeRef);

        Future<RpcResult<GetFlowTablesStatisticsOutput>> response =
                flowTableStatsService.getFlowTablesStatistics(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(getNodeId(targetNodeRef),response.get().getResult().getTransactionId()
                , StatsRequestType.ALL_FLOW_TABLE);

    }

    public void sendAllFlowsStatsFromAllTablesRequest(NodeRef targetNode) throws InterruptedException, ExecutionException{
        final GetAllFlowsStatisticsFromAllFlowTablesInputBuilder input =
                new GetAllFlowsStatisticsFromAllFlowTablesInputBuilder();

        input.setNode(targetNode);

        Future<RpcResult<GetAllFlowsStatisticsFromAllFlowTablesOutput>> response =
                flowStatsService.getAllFlowsStatisticsFromAllFlowTables(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(getNodeId(targetNode), response.get().getResult().getTransactionId()
                , StatsRequestType.ALL_FLOW);

    }

    public void sendFlowStatsFromTableRequest(NodeRef targetNode,Flow flow) throws InterruptedException, ExecutionException{
        final GetFlowStatisticsFromFlowTableInputBuilder input =
                new GetFlowStatisticsFromFlowTableInputBuilder();

        input.setNode(targetNode);
        input.fieldsFrom(flow);

        Future<RpcResult<GetFlowStatisticsFromFlowTableOutput>> response =
                flowStatsService.getFlowStatisticsFromFlowTable(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(getNodeId(targetNode), response.get().getResult().getTransactionId()
                , StatsRequestType.ALL_FLOW);

    }

    public void sendAggregateFlowsStatsFromAllTablesRequest(NodeKey targetNodeKey) throws InterruptedException, ExecutionException{

        List<Short> tablesId = getTablesFromNode(targetNodeKey);

        if(tablesId.size() != 0){
            for(Short id : tablesId){

                sendAggregateFlowsStatsFromTableRequest(targetNodeKey,id);
            }
        }else{
            spLogger.debug("No details found in data store for flow tables associated with Node {}",targetNodeKey);
        }
    }

    public void sendAggregateFlowsStatsFromTableRequest(NodeKey targetNodeKey,Short tableId) throws InterruptedException, ExecutionException{

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

    public void sendAllNodeConnectorsStatisticsRequest(NodeRef targetNode) throws InterruptedException, ExecutionException{

        final GetAllNodeConnectorsStatisticsInputBuilder input = new GetAllNodeConnectorsStatisticsInputBuilder();

        input.setNode(targetNode);

        Future<RpcResult<GetAllNodeConnectorsStatisticsOutput>> response =
                portStatsService.getAllNodeConnectorsStatistics(input.build());
        this.multipartMessageManager.addTxIdToRequestTypeEntry(getNodeId(targetNode), response.get().getResult().getTransactionId()
                , StatsRequestType.ALL_PORT);

    }

    public void sendAllGroupStatisticsRequest(NodeRef targetNode) throws InterruptedException, ExecutionException{

        final GetAllGroupStatisticsInputBuilder input = new GetAllGroupStatisticsInputBuilder();

        input.setNode(targetNode);

        Future<RpcResult<GetAllGroupStatisticsOutput>> response =
                groupStatsService.getAllGroupStatistics(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(getNodeId(targetNode), response.get().getResult().getTransactionId()
                , StatsRequestType.ALL_GROUP);

    }

    public void sendGroupDescriptionRequest(NodeRef targetNode) throws InterruptedException, ExecutionException{
        final GetGroupDescriptionInputBuilder input = new GetGroupDescriptionInputBuilder();

        input.setNode(targetNode);

        Future<RpcResult<GetGroupDescriptionOutput>> response =
                groupStatsService.getGroupDescription(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(getNodeId(targetNode), response.get().getResult().getTransactionId()
                , StatsRequestType.GROUP_DESC);

    }

    public void sendAllMeterStatisticsRequest(NodeRef targetNode) throws InterruptedException, ExecutionException{

        GetAllMeterStatisticsInputBuilder input = new GetAllMeterStatisticsInputBuilder();

        input.setNode(targetNode);

        Future<RpcResult<GetAllMeterStatisticsOutput>> response =
                meterStatsService.getAllMeterStatistics(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(getNodeId(targetNode), response.get().getResult().getTransactionId()
                , StatsRequestType.ALL_METER);;

    }

    public void sendMeterConfigStatisticsRequest(NodeRef targetNode) throws InterruptedException, ExecutionException{

        GetAllMeterConfigStatisticsInputBuilder input = new GetAllMeterConfigStatisticsInputBuilder();

        input.setNode(targetNode);

        Future<RpcResult<GetAllMeterConfigStatisticsOutput>> response =
                meterStatsService.getAllMeterConfigStatistics(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(getNodeId(targetNode), response.get().getResult().getTransactionId()
                , StatsRequestType.METER_CONFIG);;

    }

    public void sendAllQueueStatsFromAllNodeConnector(NodeRef targetNode) throws InterruptedException, ExecutionException {
        GetAllQueuesStatisticsFromAllPortsInputBuilder input = new GetAllQueuesStatisticsFromAllPortsInputBuilder();

        input.setNode(targetNode);

        Future<RpcResult<GetAllQueuesStatisticsFromAllPortsOutput>> response =
                queueStatsService.getAllQueuesStatisticsFromAllPorts(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(getNodeId(targetNode), response.get().getResult().getTransactionId()
                , StatsRequestType.ALL_QUEUE_STATS);;

    }

    public void sendQueueStatsFromGivenNodeConnector(NodeRef targetNode,NodeConnectorId nodeConnectorId, QueueId queueId) throws InterruptedException, ExecutionException {
        GetQueueStatisticsFromGivenPortInputBuilder input = new GetQueueStatisticsFromGivenPortInputBuilder();

        input.setNode(targetNode);
        input.setNodeConnectorId(nodeConnectorId);
        input.setQueueId(queueId);
        Future<RpcResult<GetQueueStatisticsFromGivenPortOutput>> response =
                queueStatsService.getQueueStatisticsFromGivenPort(input.build());

        this.multipartMessageManager.addTxIdToRequestTypeEntry(getNodeId(targetNode), response.get().getResult().getTransactionId()
                , StatsRequestType.ALL_QUEUE_STATS);;

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
        NodeStatisticsHandler ager = statisticsCache.get(nodeId);
        if (ager == null) {
            ager = new NodeStatisticsHandler(this, new NodeKey(nodeId));
            statisticsCache.put(nodeId, ager);
        }

        return ager;
    }

    private List<Node> getAllConnectedNodes(){
        Nodes nodes = (Nodes) dps.readOperationalData(nodesIdentifier);
        if(nodes == null)
            return null;

        spLogger.debug("Number of connected nodes : {}",nodes.getNode().size());
        return nodes.getNode();
    }

    private List<Short> getTablesFromNode(NodeKey nodeKey){
        InstanceIdentifier<FlowCapableNode> nodesIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class,nodeKey).augmentation(FlowCapableNode.class).toInstance();

        FlowCapableNode node = (FlowCapableNode)dps.readOperationalData(nodesIdentifier);
        List<Short> tablesId = new ArrayList<Short>();
        if(node != null && node.getTable()!=null){
            spLogger.debug("Number of tables {} supported by node {}",node.getTable().size(),nodeKey);
            for(Table table: node.getTable()){
                tablesId.add(table.getId());
            }
        }
        return tablesId;
    }

    @SuppressWarnings("unchecked")
    private NodeId getNodeId(NodeRef nodeRef){
        InstanceIdentifier<Node> nodeII = (InstanceIdentifier<Node>) nodeRef.getValue();
        NodeKey nodeKey = InstanceIdentifier.keyOf(nodeII);
        return nodeKey.getId();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void close(){

        try {
            spLogger.info("Statistics Provider stopped.");
            if (this.listenerRegistration != null) {

                this.listenerRegistration.close();

                this.statisticsRequesterThread.destroy();

                this.statisticsAgerThread.destroy();

            }
          } catch (Throwable e) {
            throw Exceptions.sneakyThrow(e);
          }
    }

}
