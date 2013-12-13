/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import org.eclipse.xtext.xbase.lib.Exceptions;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllSwitchTablesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllSwitchTablesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
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
        
        //Need to call API to receive all the nodes connected to controller.
        List<Node> targetNodes = getAllConnectedNodes();
        
        if(targetNodes == null)
            return;

        for (Node targetNode : targetNodes){
            
            InstanceIdentifier<Node> targetInstanceId = InstanceIdentifier.builder(Nodes.class).child(Node.class,targetNode.getKey()).toInstance();
            NodeRef targetNodeRef = new NodeRef(targetInstanceId);
            
            try {
            sendAllFlowsStatsFromAllTablesRequest(targetNodeRef);
            
            Thread.sleep(1000);
            }catch(Exception e){
                spLogger.error("Exception occured while sending flow statistics request : {}",e);
            }

            if(targetNode.getAugmentation(FlowCapableNode.class) != null){

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
                    spLogger.error("Exception occured while sending statistics request : {}", e);
                }
            }
        }
    }
    
    private void sendAllFlowsStatsFromAllTablesRequest(NodeRef targetNode){
        final GetAllFlowsStatisticsFromAllSwitchTablesInputBuilder input =
                new GetAllFlowsStatisticsFromAllSwitchTablesInputBuilder();
        
        input.setNode(targetNode);
        
        @SuppressWarnings("unused")
        Future<RpcResult<GetAllFlowsStatisticsFromAllSwitchTablesOutput>> response = 
                flowStatsService.getAllFlowsStatisticsFromAllSwitchTables(input.build());
        
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
