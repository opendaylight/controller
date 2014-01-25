/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.md.statistics.manager.NodeStatisticsAger.FlowEntry;
import org.opendaylight.controller.md.statistics.manager.NodeStatisticsAger.QueueEntry;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowsStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.aggregate.flow.statistics.AggregateFlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupDescStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.desc.GroupDescBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.features.GroupFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.desc.stats.reply.GroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.reply.GroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterConfigStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterConfigStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.GenericStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.NodeConnectorStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.QueueStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.queue.id.and.statistics.map.QueueIdAndStatisticsMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class implement statistics manager related listener interface and augment all the 
 * received statistics data to data stores.
 * TODO: Need to add error message listener and clean-up the associated tx id 
 * if it exists in the tx-id cache.
 * @author vishnoianil
 *
 */
public class StatisticsUpdateCommiter implements OpendaylightGroupStatisticsListener,
        OpendaylightMeterStatisticsListener, 
        OpendaylightFlowStatisticsListener,
        OpendaylightPortStatisticsListener,
        OpendaylightFlowTableStatisticsListener,
        OpendaylightQueueStatisticsListener{
    
    public final static Logger sucLogger = LoggerFactory.getLogger(StatisticsUpdateCommiter.class);

    private final StatisticsProvider statisticsManager;
    private final MultipartMessageManager messageManager;
    
    private int unaccountedFlowsCounter = 1;

    public StatisticsUpdateCommiter(final StatisticsProvider manager){

        this.statisticsManager = manager;
        this.messageManager = this.statisticsManager.getMultipartMessageManager();
    }
    
    public StatisticsProvider getStatisticsManager(){
        return statisticsManager;
    }
   
    @Override
    public void onMeterConfigStatsUpdated(MeterConfigStatsUpdated notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;
        
        NodeKey key = new NodeKey(notification.getId());

        //Add statistics to local cache
        ConcurrentMap<NodeId, NodeStatisticsAger> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getId())){
            cache.put(notification.getId(), new NodeStatisticsAger(statisticsManager,key));
        }
        cache.get(notification.getId()).updateMeterConfigStats(notification.getMeterConfigStats());
        
        //Publish data to configuration data store
        List<MeterConfigStats> meterConfigStatsList = notification.getMeterConfigStats();
        
        for(MeterConfigStats meterConfigStats : meterConfigStatsList){
            DataModificationTransaction it = this.statisticsManager.startChange();
            MeterBuilder meterBuilder = new MeterBuilder();
            MeterKey meterKey = new MeterKey(meterConfigStats.getMeterId());
            meterBuilder.setKey(meterKey);
            
            InstanceIdentifier<Meter> meterRef = InstanceIdentifier.builder(Nodes.class).child(Node.class,key)
                                                                                        .augmentation(FlowCapableNode.class)
                                                                                        .child(Meter.class,meterKey).toInstance();
            
            NodeMeterConfigStatsBuilder meterConfig= new NodeMeterConfigStatsBuilder();
            MeterConfigStatsBuilder stats = new MeterConfigStatsBuilder();
            stats.fieldsFrom(meterConfigStats);
            meterConfig.setMeterConfigStats(stats.build());
            
            //Update augmented data
            meterBuilder.addAugmentation(NodeMeterConfigStats.class, meterConfig.build());
            it.putOperationalData(meterRef, meterBuilder.build());
            it.commit();

        }
    }

    @Override
    public void onMeterStatisticsUpdated(MeterStatisticsUpdated notification) {
        
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        NodeKey key = new NodeKey(notification.getId());
        
        List<MeterStats> meterStatsList = notification.getMeterStats();
        
        for(MeterStats meterStats : meterStatsList){

            //Publish data to configuration data store
            DataModificationTransaction it = this.statisticsManager.startChange();
            MeterBuilder meterBuilder = new MeterBuilder();
            MeterKey meterKey = new MeterKey(meterStats.getMeterId());
            meterBuilder.setKey(meterKey);
            
            InstanceIdentifier<Meter> meterRef = InstanceIdentifier.builder(Nodes.class).child(Node.class,key)
                                                                                        .augmentation(FlowCapableNode.class)
                                                                                        .child(Meter.class,meterKey).toInstance();
            
            NodeMeterStatisticsBuilder meterStatsBuilder= new NodeMeterStatisticsBuilder();
            MeterStatisticsBuilder stats = new MeterStatisticsBuilder();
            stats.fieldsFrom(meterStats);
            meterStatsBuilder.setMeterStatistics(stats.build());

            //Update augmented data
            meterBuilder.addAugmentation(NodeMeterStatistics.class, meterStatsBuilder.build());
            it.putOperationalData(meterRef, meterBuilder.build());
            it.commit();
        }
    }

    @Override
    public void onGroupDescStatsUpdated(GroupDescStatsUpdated notification) {
        
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        NodeKey key = new NodeKey(notification.getId());

        //Add statistics to local cache
        ConcurrentMap<NodeId, NodeStatisticsAger> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getId())){
            cache.put(notification.getId(), new NodeStatisticsAger(statisticsManager,key));
        }
        cache.get(notification.getId()).updateGroupDescStats(notification.getGroupDescStats());
        
        //Publish data to configuration data store
        List<GroupDescStats> groupDescStatsList = notification.getGroupDescStats();

        for(GroupDescStats groupDescStats : groupDescStatsList){
            DataModificationTransaction it = this.statisticsManager.startChange();
            
            GroupBuilder groupBuilder = new GroupBuilder();
            GroupKey groupKey = new GroupKey(groupDescStats.getGroupId());
            groupBuilder.setKey(groupKey);
            
            InstanceIdentifier<Group> groupRef = InstanceIdentifier.builder(Nodes.class).child(Node.class,key)
                                                                                        .augmentation(FlowCapableNode.class)
                                                                                        .child(Group.class,groupKey).toInstance();

            NodeGroupDescStatsBuilder groupDesc= new NodeGroupDescStatsBuilder();
            GroupDescBuilder stats = new GroupDescBuilder();
            stats.fieldsFrom(groupDescStats);
            groupDesc.setGroupDesc(stats.build());
            
            //Update augmented data
            groupBuilder.addAugmentation(NodeGroupDescStats.class, groupDesc.build());

            it.putOperationalData(groupRef, groupBuilder.build());
            it.commit();
        }
    }

    @Override
    public void onGroupStatisticsUpdated(GroupStatisticsUpdated notification) {
        
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        //Publish data to configuration data store
        NodeKey key = new NodeKey(notification.getId());
        List<GroupStats> groupStatsList = notification.getGroupStats();

        for(GroupStats groupStats : groupStatsList){
            DataModificationTransaction it = this.statisticsManager.startChange();
            
            GroupBuilder groupBuilder = new GroupBuilder();
            GroupKey groupKey = new GroupKey(groupStats.getGroupId());
            groupBuilder.setKey(groupKey);
            
            InstanceIdentifier<Group> groupRef = InstanceIdentifier.builder(Nodes.class).child(Node.class,key)
                                                                                        .augmentation(FlowCapableNode.class)
                                                                                        .child(Group.class,groupKey).toInstance();

            NodeGroupStatisticsBuilder groupStatisticsBuilder= new NodeGroupStatisticsBuilder();
            GroupStatisticsBuilder stats = new GroupStatisticsBuilder();
            stats.fieldsFrom(groupStats);
            groupStatisticsBuilder.setGroupStatistics(stats.build());
            
            //Update augmented data
            groupBuilder.addAugmentation(NodeGroupStatistics.class, groupStatisticsBuilder.build());
            it.putOperationalData(groupRef, groupBuilder.build());
            it.commit();
        }
    }
    
    @Override
    public void onMeterFeaturesUpdated(MeterFeaturesUpdated notification) {

        MeterFeaturesBuilder meterFeature = new MeterFeaturesBuilder();
        meterFeature.setMeterBandSupported(notification.getMeterBandSupported());
        meterFeature.setMeterCapabilitiesSupported(notification.getMeterCapabilitiesSupported());
        meterFeature.setMaxBands(notification.getMaxBands());
        meterFeature.setMaxColor(notification.getMaxColor());
        meterFeature.setMaxMeter(notification.getMaxMeter());
        
        //Publish data to configuration data store
        DataModificationTransaction it = this.statisticsManager.startChange();
        NodeKey key = new NodeKey(notification.getId());
        NodeRef ref = getNodeRef(key);
        
        final NodeBuilder nodeData = new NodeBuilder(); 
        nodeData.setKey(key);
        
        NodeMeterFeaturesBuilder nodeMeterFeatures= new NodeMeterFeaturesBuilder();
        nodeMeterFeatures.setMeterFeatures(meterFeature.build());
        
        //Update augmented data
        nodeData.addAugmentation(NodeMeterFeatures.class, nodeMeterFeatures.build());
        
        InstanceIdentifier<? extends Object> refValue = ref.getValue();
        it.putOperationalData(refValue, nodeData.build());
        it.commit();
    }
    
    @Override
    public void onGroupFeaturesUpdated(GroupFeaturesUpdated notification) {

        GroupFeaturesBuilder groupFeatures = new GroupFeaturesBuilder();
        groupFeatures.setActions(notification.getActions());
        groupFeatures.setGroupCapabilitiesSupported(notification.getGroupCapabilitiesSupported());
        groupFeatures.setGroupTypesSupported(notification.getGroupTypesSupported());
        groupFeatures.setMaxGroups(notification.getMaxGroups());
        
        //Publish data to configuration data store
        DataModificationTransaction it = this.statisticsManager.startChange();
        NodeKey key = new NodeKey(notification.getId());
        NodeRef ref = getNodeRef(key);
        
        final NodeBuilder nodeData = new NodeBuilder(); 
        nodeData.setKey(key);
        
        NodeGroupFeaturesBuilder nodeGroupFeatures= new NodeGroupFeaturesBuilder();
        nodeGroupFeatures.setGroupFeatures(groupFeatures.build());
        
        //Update augmented data
        nodeData.addAugmentation(NodeGroupFeatures.class, nodeGroupFeatures.build());
        
        InstanceIdentifier<? extends Object> refValue = ref.getValue();
        it.putOperationalData(refValue, nodeData.build());
        it.commit();
    }

    @Override
    public void onFlowsStatisticsUpdate(FlowsStatisticsUpdate notification) {
        
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        NodeKey key = new NodeKey(notification.getId());
        sucLogger.debug("Received flow stats update : {}",notification.toString());
        
        for(FlowAndStatisticsMapList map: notification.getFlowAndStatisticsMapList()){
            short tableId = map.getTableId();
            
            DataModificationTransaction it = this.statisticsManager.startChange();

            boolean foundOriginalFlow = false;

            FlowBuilder flowBuilder = new FlowBuilder();

            FlowStatisticsDataBuilder flowStatisticsData = new FlowStatisticsDataBuilder();

            FlowBuilder flow = new FlowBuilder();
            flow.setContainerName(map.getContainerName());
            flow.setBufferId(map.getBufferId());
            flow.setCookie(map.getCookie());
            flow.setCookieMask(map.getCookieMask());
            flow.setFlags(map.getFlags());
            flow.setFlowName(map.getFlowName());
            flow.setHardTimeout(map.getHardTimeout());
            if(map.getFlowId() != null)
                flow.setId(new FlowId(map.getFlowId().getValue()));
            flow.setIdleTimeout(map.getIdleTimeout());
            flow.setInstallHw(map.isInstallHw());
            flow.setInstructions(map.getInstructions());
            if(map.getFlowId()!= null)
                flow.setKey(new FlowKey(new FlowId(map.getKey().getFlowId().getValue())));
            flow.setMatch(map.getMatch());
            flow.setOutGroup(map.getOutGroup());
            flow.setOutPort(map.getOutPort());
            flow.setPriority(map.getPriority());
            flow.setStrict(map.isStrict());
            flow.setTableId(tableId);
                
            Flow flowRule = flow.build();
                
            FlowAndStatisticsMapListBuilder stats = new FlowAndStatisticsMapListBuilder();
            stats.setByteCount(map.getByteCount());
            stats.setPacketCount(map.getPacketCount());
            stats.setDuration(map.getDuration());
                
            GenericStatistics flowStats = stats.build();
                
            //Add statistics to local cache
            ConcurrentMap<NodeId, NodeStatisticsAger> cache = this.statisticsManager.getStatisticsCache();
            if(!cache.containsKey(notification.getId())){
                cache.put(notification.getId(), new NodeStatisticsAger(statisticsManager,key));
            }
            NodeStatisticsAger nsa = cache.get(notification.getId());
            FlowEntry flowStatsEntry = nsa.new FlowEntry(tableId,flowRule);
            cache.get(notification.getId()).updateFlowStats(flowStatsEntry);
                
            //Augment the data to the flow node

            FlowStatisticsBuilder flowStatistics = new FlowStatisticsBuilder();
            flowStatistics.setByteCount(flowStats.getByteCount());
            flowStatistics.setPacketCount(flowStats.getPacketCount());
            flowStatistics.setDuration(flowStats.getDuration());
            flowStatistics.setContainerName(map.getContainerName());
            flowStatistics.setBufferId(map.getBufferId());
            flowStatistics.setCookie(map.getCookie());
            flowStatistics.setCookieMask(map.getCookieMask());
            flowStatistics.setFlags(map.getFlags());
            flowStatistics.setFlowName(map.getFlowName());
            flowStatistics.setHardTimeout(map.getHardTimeout());
            flowStatistics.setIdleTimeout(map.getIdleTimeout());
            flowStatistics.setInstallHw(map.isInstallHw());
            flowStatistics.setInstructions(map.getInstructions());
            flowStatistics.setMatch(map.getMatch());
            flowStatistics.setOutGroup(map.getOutGroup());
            flowStatistics.setOutPort(map.getOutPort());
            flowStatistics.setPriority(map.getPriority());
            flowStatistics.setStrict(map.isStrict());
            flowStatistics.setTableId(tableId);

            flowStatisticsData.setFlowStatistics(flowStatistics.build());
                
            sucLogger.debug("Flow : {}",flowRule.toString());
            sucLogger.debug("Statistics to augment : {}",flowStatistics.build().toString());

            InstanceIdentifier<Table> tableRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, key)
                    .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(tableId)).toInstance();
            
            Table table= (Table)it.readConfigurationData(tableRef);

            //TODO: Not a good way to do it, need to figure out better way.
            //TODO: major issue in any alternate approach is that flow key is incrementally assigned 
            //to the flows stored in data store.
            if(table != null){

                for(Flow existingFlow : table.getFlow()){
                    sucLogger.debug("Existing flow in data store : {}",existingFlow.toString());
                    if(flowEquals(flowRule,existingFlow)){
                        InstanceIdentifier<Flow> flowRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, key)
                                .augmentation(FlowCapableNode.class)
                                .child(Table.class, new TableKey(tableId))
                                .child(Flow.class,existingFlow.getKey()).toInstance();
                        flowBuilder.setKey(existingFlow.getKey());
                        flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
                        sucLogger.debug("Found matching flow in the datastore, augmenting statistics");
                        foundOriginalFlow = true;
                        it.putOperationalData(flowRef, flowBuilder.build());
                        it.commit();
                        break;
                    }
                }
            }
            
            table= (Table)it.readOperationalData(tableRef);
            if(!foundOriginalFlow && table != null){

                for(Flow existingFlow : table.getFlow()){
                    FlowStatisticsData augmentedflowStatisticsData = existingFlow.getAugmentation(FlowStatisticsData.class);
                    if(augmentedflowStatisticsData != null){
                        FlowBuilder existingOperationalFlow = new FlowBuilder();
                        existingOperationalFlow.fieldsFrom(augmentedflowStatisticsData.getFlowStatistics());
                        sucLogger.debug("Existing unaccounted flow in operational data store : {}",existingFlow.toString());
                        if(flowEquals(flowRule,existingOperationalFlow.build())){
                            InstanceIdentifier<Flow> flowRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, key)
                                    .augmentation(FlowCapableNode.class)
                                    .child(Table.class, new TableKey(tableId))
                                    .child(Flow.class,existingFlow.getKey()).toInstance();
                            flowBuilder.setKey(existingFlow.getKey());
                            flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
                            sucLogger.debug("Found matching unaccounted flow in the operational datastore, augmenting statistics");
                            foundOriginalFlow = true;
                            it.putOperationalData(flowRef, flowBuilder.build());
                            it.commit();
                            break;
                        }
                    }
                }
            }
            if(!foundOriginalFlow){
                long flowKey = Long.parseLong(new String("1"+Short.toString(tableId)+"0"+Integer.toString(this.unaccountedFlowsCounter)));
                this.unaccountedFlowsCounter++;
                FlowKey newFlowKey = new FlowKey(new FlowId(Long.toString(flowKey)));
                InstanceIdentifier<Flow> flowRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, key)
                        .augmentation(FlowCapableNode.class)
                        .child(Table.class, new TableKey(tableId))
                        .child(Flow.class,newFlowKey).toInstance();
                flowBuilder.setKey(newFlowKey);
                flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
                sucLogger.info("Flow {} is not present in config data store, augmenting statistics as an unaccounted flow",flowBuilder.build());
                it.putOperationalData(flowRef, flowBuilder.build());
                it.commit();
            }
        }
    }

    @Override
    public void onAggregateFlowStatisticsUpdate(AggregateFlowStatisticsUpdate notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        NodeKey key = new NodeKey(notification.getId());
        
        Short tableId = messageManager.getTableIdForTxId(notification.getId(),notification.getTransactionId());
        if(tableId != null){
            
            DataModificationTransaction it = this.statisticsManager.startChange();

            InstanceIdentifier<Table> tableRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, key)
                    .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(tableId)).toInstance();

            AggregateFlowStatisticsDataBuilder aggregateFlowStatisticsDataBuilder = new AggregateFlowStatisticsDataBuilder();
            AggregateFlowStatisticsBuilder aggregateFlowStatisticsBuilder = new AggregateFlowStatisticsBuilder();
            aggregateFlowStatisticsBuilder.setByteCount(notification.getByteCount());
            aggregateFlowStatisticsBuilder.setFlowCount(notification.getFlowCount());
            aggregateFlowStatisticsBuilder.setPacketCount(notification.getPacketCount());
            aggregateFlowStatisticsDataBuilder.setAggregateFlowStatistics(aggregateFlowStatisticsBuilder.build());
            
            sucLogger.debug("Augment aggregate statistics: {} for table {} on Node {}",aggregateFlowStatisticsBuilder.build().toString(),tableId,key);

            TableBuilder tableBuilder = new TableBuilder();
            tableBuilder.setKey(new TableKey(tableId));
            tableBuilder.addAugmentation(AggregateFlowStatisticsData.class, aggregateFlowStatisticsDataBuilder.build());
            it.putOperationalData(tableRef, tableBuilder.build());
            it.commit();

        }
    }

    @Override
    public void onNodeConnectorStatisticsUpdate(NodeConnectorStatisticsUpdate notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        NodeKey key = new NodeKey(notification.getId());
        
        List<NodeConnectorStatisticsAndPortNumberMap> portsStats = notification.getNodeConnectorStatisticsAndPortNumberMap();
        for(NodeConnectorStatisticsAndPortNumberMap portStats : portsStats){
            
            DataModificationTransaction it = this.statisticsManager.startChange();

            FlowCapableNodeConnectorStatisticsBuilder statisticsBuilder 
                                            = new FlowCapableNodeConnectorStatisticsBuilder();
            statisticsBuilder.setBytes(portStats.getBytes());
            statisticsBuilder.setCollisionCount(portStats.getCollisionCount());
            statisticsBuilder.setDuration(portStats.getDuration());
            statisticsBuilder.setPackets(portStats.getPackets());
            statisticsBuilder.setReceiveCrcError(portStats.getReceiveCrcError());
            statisticsBuilder.setReceiveDrops(portStats.getReceiveDrops());
            statisticsBuilder.setReceiveErrors(portStats.getReceiveErrors());
            statisticsBuilder.setReceiveFrameError(portStats.getReceiveFrameError());
            statisticsBuilder.setReceiveOverRunError(portStats.getReceiveOverRunError());
            statisticsBuilder.setTransmitDrops(portStats.getTransmitDrops());
            statisticsBuilder.setTransmitErrors(portStats.getTransmitErrors());
            
            //Augment data to the node-connector
            FlowCapableNodeConnectorStatisticsDataBuilder statisticsDataBuilder = 
                    new FlowCapableNodeConnectorStatisticsDataBuilder();
            
            statisticsDataBuilder.setFlowCapableNodeConnectorStatistics(statisticsBuilder.build());
            
            InstanceIdentifier<NodeConnector> nodeConnectorRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, key).child(NodeConnector.class, new NodeConnectorKey(portStats.getNodeConnectorId())).toInstance();
            
            NodeConnector nodeConnector = (NodeConnector)it.readOperationalData(nodeConnectorRef);
            
            if(nodeConnector != null){
                sucLogger.debug("Augmenting port statistics {} to port {}",statisticsDataBuilder.build().toString(),nodeConnectorRef.toString());
                NodeConnectorBuilder nodeConnectorBuilder = new NodeConnectorBuilder();
                nodeConnectorBuilder.addAugmentation(FlowCapableNodeConnectorStatisticsData.class, statisticsDataBuilder.build());
                it.putOperationalData(nodeConnectorRef, nodeConnectorBuilder.build());
                it.commit();
            }
        }
    }

    @Override
    public void onFlowTableStatisticsUpdate(FlowTableStatisticsUpdate notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        NodeKey key = new NodeKey(notification.getId());
        
        List<FlowTableAndStatisticsMap> flowTablesStatsList = notification.getFlowTableAndStatisticsMap();
        for (FlowTableAndStatisticsMap ftStats : flowTablesStatsList){
            
            DataModificationTransaction it = this.statisticsManager.startChange();

            InstanceIdentifier<Table> tableRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, key)
                    .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(ftStats.getTableId().getValue())).toInstance();
            
            FlowTableStatisticsDataBuilder statisticsDataBuilder = new FlowTableStatisticsDataBuilder();
            
            FlowTableStatisticsBuilder statisticsBuilder = new FlowTableStatisticsBuilder();
            statisticsBuilder.setActiveFlows(ftStats.getActiveFlows());
            statisticsBuilder.setPacketsLookedUp(ftStats.getPacketsLookedUp());
            statisticsBuilder.setPacketsMatched(ftStats.getPacketsMatched());
            
            statisticsDataBuilder.setFlowTableStatistics(statisticsBuilder.build());
            
            sucLogger.debug("Augment flow table statistics: {} for table {} on Node {}",statisticsBuilder.build().toString(),ftStats.getTableId(),key);
            
            TableBuilder tableBuilder = new TableBuilder();
            tableBuilder.setKey(new TableKey(ftStats.getTableId().getValue()));
            tableBuilder.addAugmentation(FlowTableStatisticsData.class, statisticsDataBuilder.build());
            it.putOperationalData(tableRef, tableBuilder.build());
            it.commit();
        }
    }

    @Override
    public void onQueueStatisticsUpdate(QueueStatisticsUpdate notification) {
        
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        NodeKey key = new NodeKey(notification.getId());
        
        //Add statistics to local cache
        ConcurrentMap<NodeId, NodeStatisticsAger> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getId())){
            cache.put(notification.getId(), new NodeStatisticsAger(statisticsManager,key));
        }
        
        NodeStatisticsAger nsa = cache.get(notification.getId());
        
        List<QueueIdAndStatisticsMap> queuesStats = notification.getQueueIdAndStatisticsMap();
        for(QueueIdAndStatisticsMap swQueueStats : queuesStats){
            
            QueueEntry queueEntry = nsa.new QueueEntry(swQueueStats.getNodeConnectorId(),swQueueStats.getQueueId());
            nsa.updateQueueStats(queueEntry);
            
            FlowCapableNodeConnectorQueueStatisticsDataBuilder queueStatisticsDataBuilder = new FlowCapableNodeConnectorQueueStatisticsDataBuilder();
            
            FlowCapableNodeConnectorQueueStatisticsBuilder queueStatisticsBuilder = new FlowCapableNodeConnectorQueueStatisticsBuilder();
            
            queueStatisticsBuilder.fieldsFrom(swQueueStats);
            
            queueStatisticsDataBuilder.setFlowCapableNodeConnectorQueueStatistics(queueStatisticsBuilder.build());
            
            DataModificationTransaction it = this.statisticsManager.startChange();

            InstanceIdentifier<Queue> queueRef 
                    = InstanceIdentifier.builder(Nodes.class)
                                        .child(Node.class, key)
                                        .child(NodeConnector.class, new NodeConnectorKey(swQueueStats.getNodeConnectorId()))
                                        .augmentation(FlowCapableNodeConnector.class)
                                        .child(Queue.class, new QueueKey(swQueueStats.getQueueId())).toInstance();
            
            QueueBuilder queueBuilder = new QueueBuilder();
            queueBuilder.addAugmentation(FlowCapableNodeConnectorQueueStatisticsData.class, queueStatisticsDataBuilder.build());
            queueBuilder.setKey(new QueueKey(swQueueStats.getQueueId()));

            sucLogger.debug("Augmenting queue statistics {} of queue {} to port {}"
                                        ,queueStatisticsDataBuilder.build().toString(),
                                        swQueueStats.getQueueId(),
                                        swQueueStats.getNodeConnectorId());
            
            it.putOperationalData(queueRef, queueBuilder.build());
            it.commit();
            
        }
        
    }

    private NodeRef getNodeRef(NodeKey nodeKey){
        InstanceIdentifierBuilder<?> builder = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey);
        return new NodeRef(builder.toInstance());
    }
   
    public boolean flowEquals(Flow statsFlow, Flow storedFlow) {
        if (statsFlow.getClass() != storedFlow.getClass()) {
            return false;
        }
        if (statsFlow.getContainerName()== null) {
            if (storedFlow.getContainerName()!= null) {
                return false;
            }
        } else if(!statsFlow.getContainerName().equals(storedFlow.getContainerName())) {
            return false;
        }
        if (statsFlow.getMatch()== null) {
            if (storedFlow.getMatch() != null) {
                return false;
            }
        } //else if(!statsFlow.getMatch().equals(storedFlow.getMatch())) {
        else if(!matchEquals(statsFlow.getMatch(), storedFlow.getMatch())) {
            return false;
        }
        if (statsFlow.getPriority() == null) {
            if (storedFlow.getPriority() != null) {
                return false;
            }
        } else if(!statsFlow.getPriority().equals(storedFlow.getPriority())) {
            return false;
        }
        if (statsFlow.getTableId() == null) {
            if (storedFlow.getTableId() != null) {
                return false;
            }
        } else if(!statsFlow.getTableId().equals(storedFlow.getTableId())) {
            return false;
        }
        return true;
    }
    
    /**
     * Explicit equals method to compare the 'match' for flows stored in the data-stores and flow fetched from the switch.
     * Usecase: e.g If user don't set any ethernet source and destination address for match,data store will store null for 
     * these address.
     * e.g [_ethernetMatch=EthernetMatch [_ethernetDestination=null, _ethernetSource=null, _ethernetType=
     * EthernetType [_type=EtherType [_value=2048], _mask=null, augmentation=[]]
     * 
     * But when you fetch the flows from switch, openflow driver library converts all zero bytes of mac address in the 
     * message stream to 00:00:00:00:00:00. Following string shows how library interpret the zero mac address bytes and 
     * eventually when translator convert it to MD-SAL match, this is how it looks 
     * [_ethernetDestination=EthernetDestination [_address=MacAddress [_value=00:00:00:00:00:00], _mask=null, augmentation=[]], 
     * _ethernetSource=EthernetSource [_address=MacAddress [_value=00:00:00:00:00:00], _mask=null, augmentation=[]], 
     * _ethernetType=EthernetType [_type=EtherType [_value=2048], _mask=null, augmentation=[]]
     * 
     * Similarly for inPort, if user/application don't set any value for it, FRM will store null value for it in data store. 
     * When we fetch the same flow (with its statistics) from switch, plugin converts its value to openflow:X:0.
     *  e.g _inPort=Uri [_value=openflow:1:0]  
     * 
     * So this custom equals method add additional check to take care of these scenario, in case any match element is null in data-store-flow, but not
     * in the flow fetched from switch.
     * 
     * @param statsFlow
     * @param storedFlow
     * @return
     */
    
    public boolean matchEquals(Match statsFlow, Match storedFlow) {
        if (statsFlow == storedFlow) {
            return true;
        }
        if (storedFlow.getClass() != statsFlow.getClass()) {
            return false;
        }
        if (storedFlow.getEthernetMatch() == null) {
            if (statsFlow.getEthernetMatch() != null) {
                return false;
            }
        } else if(!storedFlow.getEthernetMatch().equals(statsFlow.getEthernetMatch())) {
            return false;
        }
        if (storedFlow.getIcmpv4Match()== null) {
            if (statsFlow.getIcmpv4Match() != null) {
                return false;
            }
        } else if(!storedFlow.getIcmpv4Match().equals(statsFlow.getIcmpv4Match())) {
            return false;
        }
        if (storedFlow.getIcmpv6Match() == null) {
            if (statsFlow.getIcmpv6Match() != null) {
                return false;
            }
        } else if(!storedFlow.getIcmpv6Match().equals(statsFlow.getIcmpv6Match())) {
            return false;
        }
        if (storedFlow.getInPhyPort() == null) {
            if (statsFlow.getInPhyPort() != null) {
                return false;
            }
        } else if(!storedFlow.getInPhyPort().equals(statsFlow.getInPhyPort())) {
            return false;
        }
        if (storedFlow.getInPort()== null) {
            if (statsFlow.getInPort() != null) {
                return false;
            }
        } else if(!storedFlow.getInPort().equals(statsFlow.getInPort())) {
            return false;
        }
        if (storedFlow.getIpMatch()== null) {
            if (statsFlow.getIpMatch() != null) {
                return false;
            }
        } else if(!storedFlow.getIpMatch().equals(statsFlow.getIpMatch())) {
            return false;
        }
        if (storedFlow.getLayer3Match()== null) {
            if (statsFlow.getLayer3Match() != null) {
                    return false;
            }
        } else if(!storedFlow.getLayer3Match().equals(statsFlow.getLayer3Match())) {
            return false;
        }
        if (storedFlow.getLayer4Match()== null) {
            if (statsFlow.getLayer4Match() != null) {
                return false;
            }
        } else if(!storedFlow.getLayer4Match().equals(statsFlow.getLayer4Match())) {
            return false;
        }
        if (storedFlow.getMetadata() == null) {
            if (statsFlow.getMetadata() != null) {
                return false;
            }
        } else if(!storedFlow.getMetadata().equals(statsFlow.getMetadata())) {
            return false;
        }
        if (storedFlow.getProtocolMatchFields() == null) {
            if (statsFlow.getProtocolMatchFields() != null) {
                return false;
            }
        } else if(!storedFlow.getProtocolMatchFields().equals(statsFlow.getProtocolMatchFields())) {
            return false;
        }
        if (storedFlow.getTunnel()== null) {
            if (statsFlow.getTunnel() != null) {
                return false;
            }
        } else if(!storedFlow.getTunnel().equals(statsFlow.getTunnel())) {
            return false;
        }
        if (storedFlow.getVlanMatch()== null) {
            if (statsFlow.getVlanMatch() != null) {
                return false;
            }
        } else if(!storedFlow.getVlanMatch().equals(statsFlow.getVlanMatch())) {
            return false;
        }
        return true;
    }
}
