package org.opendaylight.controller.md.statistics.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import org.eclipse.xtext.xbase.lib.Exceptions;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupFeaturesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupFeaturesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterFeaturesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterFeaturesOutput;
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
    
    private Thread statisticsRequesterThread;
    
    private final  InstanceIdentifier<Nodes> nodesIdentifier = InstanceIdentifier.builder().node(Nodes.class).toInstance();
    
    //Local caching of stats
    
    private final ConcurrentMap<NodeRef,NodeStatistics> statisticsCache = 
            new ConcurrentHashMap<NodeRef,NodeStatistics>();
    
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

        statisticsRequesterThread = new Thread( new Runnable(){

            @Override
            public void run() {
                while(true){
                    try {
                        statsRequestSender();
                        
                        Thread.sleep(5000);
                    }catch (Exception e){
                        spLogger.error("Exception occurred while sending stats request : {}",e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        });
        spLogger.info("Statistics Provider started.");
    }
    
    protected DataModificationTransaction startChange() {
        
        DataProviderService dps = this.getDataService();
        return dps.beginTransaction();
    }
    
    private void statsRequestSender(){
        
        //Need to call API to receive all the nodes connected to controller.
        
        List<NodeRef> targetNodes = new ArrayList<NodeRef>();
        
        for (NodeRef targetNode : targetNodes){
            
            sendAllGroupStatisticsRequest(targetNode);
            
            sendAllMeterStatisticsRequest(targetNode);

            //We need to add check, so see if groups/meters are supported
            //by the target node.
            sendGroupDescriptionRequest(targetNode);
            
            sendGroupFeaturesRequest(targetNode);
            
            sendMeterConfigStatisticsRequest(targetNode);
            
            sendMeterFeaturesRequest(targetNode);
        }
    }
    
    private void sendAllGroupStatisticsRequest(NodeRef targetNode){
        
        GetAllGroupStatisticsInputBuilder input = new GetAllGroupStatisticsInputBuilder();
        
        input.setNode(targetNode);

        Future<RpcResult<GetAllGroupStatisticsOutput>> response = 
                groupStatsService.getAllGroupStatistics(input.build());
    }
    
    private void sendGroupDescriptionRequest(NodeRef targetNode){
        GetGroupDescriptionInputBuilder input = new GetGroupDescriptionInputBuilder();
        
        input.setNode(targetNode);
        
        Future<RpcResult<GetGroupDescriptionOutput>> response = 
                groupStatsService.getGroupDescription(input.build());
    }
    
    private void sendGroupFeaturesRequest(NodeRef targetNode){
        
        GetGroupFeaturesInputBuilder input = new GetGroupFeaturesInputBuilder();
        
        input.setNode(targetNode);
        
        Future<RpcResult<GetGroupFeaturesOutput>> response = 
                groupStatsService.getGroupFeatures(input.build());
    }
    
    private void sendAllMeterStatisticsRequest(NodeRef targenetNode){
        
        GetAllMeterStatisticsInputBuilder input = new GetAllMeterStatisticsInputBuilder();
        
        input.setNode(targenetNode);
        
        Future<RpcResult<GetAllMeterStatisticsOutput>> response = 
                meterStatsService.getAllMeterStatistics(input.build());
    }
    
    private void sendMeterConfigStatisticsRequest(NodeRef targetNode){
        
        GetAllMeterConfigStatisticsInputBuilder input = new GetAllMeterConfigStatisticsInputBuilder();
        
        input.setNode(targetNode);
        
        Future<RpcResult<GetAllMeterConfigStatisticsOutput>> response = 
                meterStatsService.getAllMeterConfigStatistics(input.build());
        
    }
    private void sendMeterFeaturesRequest(NodeRef targetNode){
     
        GetMeterFeaturesInputBuilder input = new GetMeterFeaturesInputBuilder();
        
        input.setNode(targetNode);
        
        Future<RpcResult<GetMeterFeaturesOutput>> response = 
                meterStatsService.getMeterFeatures(input.build());
    }
    
    public ConcurrentMap<NodeRef, NodeStatistics> getStatisticsCache() {
        return statisticsCache;
    }
    
    private List<Node> getAllConnectedNodes(){
        
        Nodes nodes = (Nodes) dps.readOperationalData(nodesIdentifier);
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
