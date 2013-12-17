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
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllPortsStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllPortsStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsProvider implements AutoCloseable {

    public final static Logger spLogger = LoggerFactory.getLogger(StatisticsProvider.class);
    
    private DataProviderService dps;

    private NotificationProviderService nps;
    
    private OpendaylightGroupStatisticsService groupStatsService;
    
    private OpendaylightMeterStatisticsService meterStatsService;
    
    private OpendaylightFlowStatisticsService flowStatsService;
    
    private OpendaylightPortStatisticsService portStatsService;

    private OpendaylightFlowTableStatisticsService flowTableStatsService;

    private OpendaylightQueueStatisticsService queueStatsService;

    private final MultipartMessageManager multipartMessageManager = new MultipartMessageManager();
    
    private Thread statisticsRequesterThread;
    
    private final  InstanceIdentifier<Nodes> nodesIdentifier = InstanceIdentifier.builder(Nodes.class).toInstance();
    
    private final int STATS_THREAD_EXECUTION_TIME= 50000;
    //Local caching of stats
    
    private final ConcurrentMap<NodeId,NodeStatistics> statisticsCache = 
            new ConcurrentHashMap<NodeId,NodeStatistics>();
    
    public DataProviderService getDataService() {
      return this.dps;
    }
    
    public void setDataService(final DataProviderService dataService) {
      this.dps = dataService;
    }
    
    public NotificationProviderService getNotificationService() {
      return this.nps;
    }
    
    public void setNotificationService(final NotificationProviderService notificationService) {
      this.nps = notificationService;
    }

    public MultipartMessageManager getMultipartMessageManager() {
        return multipartMessageManager;
    }

    private final StatisticsUpdateCommiter updateCommiter = new StatisticsUpdateCommiter(StatisticsProvider.this);
    
    private Registration<NotificationListener> listenerRegistration;
    
    public void start() {
        
        NotificationProviderService nps = this.getNotificationService();
        Registration<NotificationListener> registerNotificationListener = nps.registerNotificationListener(this.updateCommiter);
        this.listenerRegistration = registerNotificationListener;
        
        // Get Group/Meter statistics service instance
        groupStatsService = StatisticsManagerActivator.getProviderContext().
                getRpcService(OpendaylightGroupStatisticsService.class);
        
        meterStatsService = StatisticsManagerActivator.getProviderContext().
                getRpcService(OpendaylightMeterStatisticsService.class);
        
        flowStatsService = StatisticsManagerActivator.getProviderContext().
                getRpcService(OpendaylightFlowStatisticsService.class);

        portStatsService = StatisticsManagerActivator.getProviderContext().
                getRpcService(OpendaylightPortStatisticsService.class);

        flowTableStatsService = StatisticsManagerActivator.getProviderContext().
                getRpcService(OpendaylightFlowTableStatisticsService.class);
        
        queueStatsService = StatisticsManagerActivator.getProviderContext().
                getRpcService(OpendaylightQueueStatisticsService.class);
        
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
        
        spLogger.info("Statistics Provider started.");
    }
    
    protected DataModificationTransaction startChange() {
        
        DataProviderService dps = this.getDataService();
        return dps.beginTransaction();
    }
    
    private void statsRequestSender(){
        
        List<Node> targetNodes = getAllConnectedNodes();
        
        if(targetNodes == null)
            return;
        

        for (Node targetNode : targetNodes){
            
            InstanceIdentifier<Node> targetInstanceId = InstanceIdentifier.builder(Nodes.class).child(Node.class,targetNode.getKey()).toInstance();
            NodeRef targetNodeRef = new NodeRef(targetInstanceId);
            
            System.out.println("ANIL: Target Node object ::"+targetNode.toString());
            
            System.out.println("ANIL: FlowCapableNode augmentations ::"+targetNode.getAugmentation(FlowCapableNode.class));
            
            try {
                
//                sendAggregateFlowsStatsFromAllTablesRequest(targetNode.getKey());
//
//                sendAllFlowsStatsFromAllTablesRequest(targetNodeRef);
//
//                sendAllPortStatisticsRequest(targetNodeRef);
//                
//                sendAllFlowTablesStatisticsRequest(targetNodeRef);
//                
                sendAllQueueStatsFromAllNodeConnector (targetNodeRef);

            }catch(Exception e){
                spLogger.error("Exception occured while sending statistics requests : {}",e);
            }

            if(targetNode.getAugmentation(FlowCapableNode.class) == null){

                spLogger.info("Send request for stats collection to node : {})",targetNode.getId());

                try{
                  sendAllGroupStatisticsRequest(targetNodeRef);
                  Thread.sleep(1000);
                  sendAllMeterStatisticsRequest(targetNodeRef);
                  Thread.sleep(1000);
                  sendGroupDescriptionRequest(targetNodeRef);
                  Thread.sleep(1000);
                  sendMeterConfigStatisticsRequest(targetNodeRef);
                  Thread.sleep(1000);
                }catch(Exception e){
                    spLogger.error("Exception occured while sending statistics requests : {}", e);
                }
            }
        }
    }

    private void sendAllFlowTablesStatisticsRequest(NodeRef targetNodeRef) {
        final GetFlowTablesStatisticsInputBuilder input = 
                new GetFlowTablesStatisticsInputBuilder();
        
        input.setNode(targetNodeRef);

        @SuppressWarnings("unused")
        Future<RpcResult<GetFlowTablesStatisticsOutput>> response = 
                flowTableStatsService.getFlowTablesStatistics(input.build());
    }

    private void sendAllFlowsStatsFromAllTablesRequest(NodeRef targetNode){
        final GetAllFlowsStatisticsFromAllFlowTablesInputBuilder input =
                new GetAllFlowsStatisticsFromAllFlowTablesInputBuilder();
        
        input.setNode(targetNode);
        
        @SuppressWarnings("unused")
        Future<RpcResult<GetAllFlowsStatisticsFromAllFlowTablesOutput>> response = 
                flowStatsService.getAllFlowsStatisticsFromAllFlowTables(input.build());
        
    }
    
    private void sendAggregateFlowsStatsFromAllTablesRequest(NodeKey targetNodeKey) throws InterruptedException, ExecutionException{
        
        List<Short> tablesId = getTablesFromNode(targetNodeKey);
        
        if(tablesId.size() != 0){
            for(Short id : tablesId){
                
                spLogger.info("Send aggregate stats request for flow table {} to node {}",id,targetNodeKey);
                GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder input = 
                        new GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder();
                
                input.setNode(new NodeRef(InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey).toInstance()));
                input.setTableId(new org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId(id));
                Future<RpcResult<GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput>> response = 
                        flowStatsService.getAggregateFlowStatisticsFromFlowTableForAllFlows(input.build());
                
                multipartMessageManager.setTxIdAndTableIdMapEntry(response.get().getResult().getTransactionId(), id);
            }
        }
        
        //Note: Just for testing, because i am not able to fetch table list from datastore
        // Bug-225 is raised for investigation.
        
//                spLogger.info("Send aggregate stats request for flow table {} to node {}",1,targetNodeKey);
//                GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder input = 
//                        new GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder();
//                
//                input.setNode(new NodeRef(InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey).toInstance()));
//                input.setTableId(new TableId((short)1));
//                Future<RpcResult<GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput>> response = 
//                        flowStatsService.getAggregateFlowStatisticsFromFlowTableForAllFlows(input.build());`
//                
//                multipartMessageManager.setTxIdAndTableIdMapEntry(response.get().getResult().getTransactionId(), (short)1);
    }

    private void sendAllPortStatisticsRequest(NodeRef targetNode){
        
        final GetAllPortsStatisticsInputBuilder input = new GetAllPortsStatisticsInputBuilder();
        
        input.setNode(targetNode);

        @SuppressWarnings("unused")
        Future<RpcResult<GetAllPortsStatisticsOutput>> response = 
                portStatsService.getAllPortsStatistics(input.build());
    }

    private void sendAllGroupStatisticsRequest(NodeRef targetNode){
        
        final GetAllGroupStatisticsInputBuilder input = new GetAllGroupStatisticsInputBuilder();
        
        input.setNode(targetNode);

        @SuppressWarnings("unused")
        Future<RpcResult<GetAllGroupStatisticsOutput>> response = 
                groupStatsService.getAllGroupStatistics(input.build());
    }
    
    private void sendGroupDescriptionRequest(NodeRef targetNode){
        final GetGroupDescriptionInputBuilder input = new GetGroupDescriptionInputBuilder();
        
        input.setNode(targetNode);

        @SuppressWarnings("unused")
        Future<RpcResult<GetGroupDescriptionOutput>> response = 
                groupStatsService.getGroupDescription(input.build());
    }
    
    private void sendAllMeterStatisticsRequest(NodeRef targetNode){
        
        GetAllMeterStatisticsInputBuilder input = new GetAllMeterStatisticsInputBuilder();
        
        input.setNode(targetNode);

        @SuppressWarnings("unused")
        Future<RpcResult<GetAllMeterStatisticsOutput>> response = 
                meterStatsService.getAllMeterStatistics(input.build());
    }
    
    private void sendMeterConfigStatisticsRequest(NodeRef targetNode){
        
        GetAllMeterConfigStatisticsInputBuilder input = new GetAllMeterConfigStatisticsInputBuilder();
        
        input.setNode(targetNode);

        @SuppressWarnings("unused")
        Future<RpcResult<GetAllMeterConfigStatisticsOutput>> response = 
                meterStatsService.getAllMeterConfigStatistics(input.build());
    }
    
    private void sendAllQueueStatsFromAllNodeConnector(NodeRef targetNode) {
        GetAllQueuesStatisticsFromAllPortsInputBuilder input = new GetAllQueuesStatisticsFromAllPortsInputBuilder();
        
        input.setNode(targetNode);
        
        @SuppressWarnings("unused")
        Future<RpcResult<GetAllQueuesStatisticsFromAllPortsOutput>> response = 
                queueStatsService.getAllQueuesStatisticsFromAllPorts(input.build());
    }

    public ConcurrentMap<NodeId, NodeStatistics> getStatisticsCache() {
        return statisticsCache;
    }
    
    private List<Node> getAllConnectedNodes(){
        
        Nodes nodes = (Nodes) dps.readOperationalData(nodesIdentifier);
        if(nodes == null)
            return null;
        
        spLogger.info("Number of connected nodes : {}",nodes.getNode().size());
        return nodes.getNode();
    }
    
    private List<Short> getTablesFromNode(NodeKey nodeKey){
        InstanceIdentifier<FlowCapableNode> nodesIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class,nodeKey).augmentation(FlowCapableNode.class).toInstance();
        
        FlowCapableNode node = (FlowCapableNode)dps.readConfigurationData(nodesIdentifier);
        List<Short> tablesId = new ArrayList<Short>();
        if(node != null && node.getTable()!=null){
            spLogger.info("Number of tables {} supported by node {}",node.getTable().size(),nodeKey);
            for(Table table: node.getTable()){
                tablesId.add(table.getId());
            }
        }
        return tablesId;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void close(){
        
        try {
            spLogger.info("Statistics Provider stopped.");
            if (this.listenerRegistration != null) {
              
                this.listenerRegistration.close();
                
                this.statisticsRequesterThread.destroy();
            
            }
          } catch (Throwable e) {
            throw Exceptions.sneakyThrow(e);
          }
    }

}
