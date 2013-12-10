/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowTableStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowsStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.NodeConnectorStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.aggregate.flow.statistics.AggregateFlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterConfigStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.GenericStatistics;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsUpdateCommiter implements OpendaylightGroupStatisticsListener,
        OpendaylightMeterStatisticsListener, 
        OpendaylightFlowStatisticsListener{
    
    public final static Logger sucLogger = LoggerFactory.getLogger(StatisticsUpdateCommiter.class);

    private final StatisticsProvider statisticsManager;
    
    private final int unaccountedFlowsCounter = 1;

    public StatisticsUpdateCommiter(final StatisticsProvider manager){

        this.statisticsManager = manager;
    }
    
    public StatisticsProvider getStatisticsManager(){
        return statisticsManager;
    }
   
    @Override
    public void onMeterConfigStatsUpdated(MeterConfigStatsUpdated notification) {

        //Add statistics to local cache
        ConcurrentMap<NodeId, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getId())){
            cache.put(notification.getId(), new NodeStatistics());
        }
        cache.get(notification.getId()).setMeterConfigStats(notification.getMeterConfigStats());
        
        //Publish data to configuration data store
        DataModificationTransaction it = this.statisticsManager.startChange();
        NodeKey key = new NodeKey(notification.getId());
        NodeRef ref = getNodeRef(key);
        
        final NodeBuilder nodeData = new NodeBuilder(); 
        nodeData.setKey(key);
        
        NodeMeterConfigStatsBuilder meterConfig= new NodeMeterConfigStatsBuilder();
        MeterConfigStatsBuilder stats = new MeterConfigStatsBuilder();
        stats.setMeterConfigStats(notification.getMeterConfigStats());
        meterConfig.setMeterConfigStats(stats.build());
        
        //Update augmented data
        nodeData.addAugmentation(NodeMeterConfigStats.class, meterConfig.build());
        
        InstanceIdentifier<? extends Object> refValue = ref.getValue();
        it.putOperationalData(refValue, nodeData.build());
        it.commit();

    }

    @Override
    public void onMeterStatisticsUpdated(MeterStatisticsUpdated notification) {
        //Add statistics to local cache
        ConcurrentMap<NodeId, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getId())){
            cache.put(notification.getId(), new NodeStatistics());
        }
        cache.get(notification.getId()).setMeterStatistics(notification.getMeterStats());
        
        //Publish data to configuration data store
        DataModificationTransaction it = this.statisticsManager.startChange();
        NodeKey key = new NodeKey(notification.getId());
        NodeRef ref = getNodeRef(key);
        
        final NodeBuilder nodeData = new NodeBuilder(); 
        nodeData.setKey(key);
        
        NodeMeterStatisticsBuilder meterStats= new NodeMeterStatisticsBuilder();
        MeterStatisticsBuilder stats = new MeterStatisticsBuilder();
        stats.setMeterStats(notification.getMeterStats());
        meterStats.setMeterStatistics(stats.build());
        
        //Update augmented data
        nodeData.addAugmentation(NodeMeterStatistics.class, meterStats.build());
        
        InstanceIdentifier<? extends Object> refValue = ref.getValue();
        it.putOperationalData(refValue, nodeData.build());
        it.commit();

    }

    @Override
    public void onGroupDescStatsUpdated(GroupDescStatsUpdated notification) {
        //Add statistics to local cache
        ConcurrentMap<NodeId, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getId())){
            cache.put(notification.getId(), new NodeStatistics());
        }
        cache.get(notification.getId()).setGroupDescStats(notification.getGroupDescStats());
        
        //Publish data to configuration data store
        DataModificationTransaction it = this.statisticsManager.startChange();
        NodeKey key = new NodeKey(notification.getId());
        NodeRef ref = getNodeRef(key);
        
        final NodeBuilder nodeData = new NodeBuilder(); 
        nodeData.setKey(key);
        
        NodeGroupDescStatsBuilder groupDesc= new NodeGroupDescStatsBuilder();
        GroupDescBuilder stats = new GroupDescBuilder();
        stats.setGroupDescStats(notification.getGroupDescStats());
        groupDesc.setGroupDesc(stats.build());
        
        //Update augmented data
        nodeData.addAugmentation(NodeGroupDescStats.class, groupDesc.build());

        InstanceIdentifier<? extends Object> refValue = ref.getValue();
        it.putOperationalData(refValue, nodeData.build());
        it.commit();

    }

    @Override
    public void onGroupStatisticsUpdated(GroupStatisticsUpdated notification) {
        
        //Add statistics to local cache
        ConcurrentMap<NodeId, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getId())){
            cache.put(notification.getId(), new NodeStatistics());
        }
        cache.get(notification.getId()).setGroupStatistics(notification.getGroupStats());
        
        //Publish data to configuration data store
        
        DataModificationTransaction it = this.statisticsManager.startChange();
        NodeKey key = new NodeKey(notification.getId());
        NodeRef ref = getNodeRef(key);

        final NodeBuilder nodeData = new NodeBuilder(); 
        nodeData.setKey(key);
        
        NodeGroupStatisticsBuilder groupStats = new NodeGroupStatisticsBuilder();
        GroupStatisticsBuilder stats = new GroupStatisticsBuilder();
        stats.setGroupStats(notification.getGroupStats());
        groupStats.setGroupStatistics(stats.build());
                
        //Update augmented data
        nodeData.addAugmentation(NodeGroupStatistics.class, groupStats.build());

        InstanceIdentifier<? extends Object> refValue = ref.getValue();
        it.putOperationalData(refValue, nodeData.build());
        it.commit();

//        for (GroupStats groupstat : notification.getGroupStats()) {
//        
//            GroupStatsKey groupKey = groupstat.getKey();
//            InstanceIdentifier<? extends Object> id = InstanceIdentifier.builder(Nodes.class).child(Node.class, key).augmentation(NodeGroupStatistics.class).child(GroupStatistics.class).child(GroupStats.class,groupKey).toInstance();
//            it.putOperationalData(id, groupstat);
//            it.commit();
//        }
    }
    
    @Override
    public void onMeterFeaturesUpdated(MeterFeaturesUpdated notification) {

        //Add statistics to local cache
        ConcurrentMap<NodeId, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getId())){
            cache.put(notification.getId(), new NodeStatistics());
        }
        MeterFeaturesBuilder meterFeature = new MeterFeaturesBuilder();
        meterFeature.setMeterBandSupported(notification.getMeterBandSupported());
        meterFeature.setMeterCapabilitiesSupported(notification.getMeterCapabilitiesSupported());
        meterFeature.setMaxBands(notification.getMaxBands());
        meterFeature.setMaxColor(notification.getMaxColor());
        meterFeature.setMaxMeter(notification.getMaxMeter());
        
        cache.get(notification.getId()).setMeterFeatures(meterFeature.build());
        
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

        //Add statistics to local cache
        ConcurrentMap<NodeId, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getId())){
            cache.put(notification.getId(), new NodeStatistics());
        }
        
        GroupFeaturesBuilder groupFeatures = new GroupFeaturesBuilder();
        groupFeatures.setActions(notification.getActions());
        groupFeatures.setGroupCapabilitiesSupported(notification.getGroupCapabilitiesSupported());
        groupFeatures.setGroupTypesSupported(notification.getGroupTypesSupported());
        groupFeatures.setMaxGroups(notification.getMaxGroups());
        cache.get(notification.getId()).setGroupFeatures(groupFeatures.build());
        
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
        NodeKey key = new NodeKey(notification.getId());
        sucLogger.info("Received flow stats update : {}",notification.toString());
        
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
            ConcurrentMap<NodeId, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
            if(!cache.containsKey(notification.getId())){
                cache.put(notification.getId(), new NodeStatistics());
            }
            if(!cache.get(notification.getId()).getFlowAndStatsMap().containsKey(tableId)){
                cache.get(notification.getId()).getFlowAndStatsMap().put(tableId, new HashMap<Flow,GenericStatistics>());
            }
            cache.get(notification.getId()).getFlowAndStatsMap().get(tableId).put(flowRule,flowStats);
                
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
                
            sucLogger.info("Flow : {}",flowRule.toString());
            sucLogger.info("Statistics to augment : {}",flowStatistics.build().toString());

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
            
            if(!foundOriginalFlow){
                sucLogger.info("Associated original flow is not found in data store. Augmenting flow in operational data st");
                //TODO: Temporary fix: format [ 0+tableid+0+unaccounted flow counter]
                long flowKey = Long.getLong(new String("0"+Short.toString(tableId)+"0"+Integer.toString(this.unaccountedFlowsCounter)));
                FlowKey newFlowKey = new FlowKey(new FlowId(flowKey));
                InstanceIdentifier<Flow> flowRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, key)
                        .augmentation(FlowCapableNode.class)
                        .child(Table.class, new TableKey(tableId))
                        .child(Flow.class,newFlowKey).toInstance();
                flowBuilder.setKey(newFlowKey);
                flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
                sucLogger.debug("Flow was no present in data store, augmenting statistics as an unaccounted flow");
                it.putOperationalData(flowRef, flowBuilder.build());
                it.commit();
            }
        }
    }

    @Override
    public void onAggregateFlowStatisticsUpdate(AggregateFlowStatisticsUpdate notification) {
        NodeKey key = new NodeKey(notification.getId());
        sucLogger.info("Received aggregate flow statistics update : {}",notification.toString());
        
        Short tableId = this.statisticsManager.getMultipartMessageManager().getTableIdForTxId(notification.getTransactionId());
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
            
            ConcurrentMap<NodeId, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
            if(!cache.containsKey(notification.getId())){
                cache.put(notification.getId(), new NodeStatistics());
            }
            cache.get(notification.getId()).getTableAndAggregateFlowStatsMap().put(tableId,aggregateFlowStatisticsBuilder.build());
            
            sucLogger.info("Augment aggregate statistics: {} for table {} on Node {}",aggregateFlowStatisticsBuilder.build().toString(),tableId,key);

            TableBuilder tableBuilder = new TableBuilder();
            tableBuilder.setKey(new TableKey(tableId));
            tableBuilder.addAugmentation(AggregateFlowStatisticsData.class, aggregateFlowStatisticsDataBuilder.build());
            it.putOperationalData(tableRef, tableBuilder.build());
            it.commit();

        }
    }

    @Override
    public void onFlowStatisticsUpdated(FlowStatisticsUpdated notification) {
        // TODO Auto-generated method stub
        //TODO: Depricated, will clean it up once sal-compatibility is fixed.
        //Sal-Compatibility code usage this notification event.
        
    }

    @Override
    public void onFlowTableStatisticsUpdated(FlowTableStatisticsUpdated notification) {
        // TODO Auto-generated method stub
        //TODO: Need to implement it yet
        
    }

    @Override
    public void onNodeConnectorStatisticsUpdated(NodeConnectorStatisticsUpdated notification) {
        // TODO Auto-generated method stub
        //TODO: Need to implement it yet
        
    }



    private NodeRef getNodeRef(NodeKey nodeKey){
        InstanceIdentifierBuilder<?> builder = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey);
        return new NodeRef(builder.toInstance());
    }
    
    public boolean flowEquals(Flow statsFlow, Flow storedFlow) {
        if (statsFlow.getClass() != storedFlow.getClass()) {
            return false;
        }
        if (statsFlow.getBufferId()== null) {
            if (storedFlow.getBufferId() != null) {
                return false;
            }
        } else if(!statsFlow.getBufferId().equals(storedFlow.getBufferId())) {
            return false;
        }
        if (statsFlow.getContainerName()== null) {
            if (storedFlow.getContainerName()!= null) {
                return false;
            }
        } else if(!statsFlow.getContainerName().equals(storedFlow.getContainerName())) {
            return false;
        }
        if (statsFlow.getCookie()== null) {
            if (storedFlow.getCookie()!= null) {
                return false;
            }
        } else if(!statsFlow.getCookie().equals(storedFlow.getCookie())) {
            return false;
        }
        if (statsFlow.getMatch()== null) {
            if (storedFlow.getMatch() != null) {
                return false;
            }
        } else if(!statsFlow.getMatch().equals(storedFlow.getMatch())) {
            return false;
        }
        if (statsFlow.getCookie()== null) {
            if (storedFlow.getCookie()!= null) {
                return false;
            }
        } else if(!statsFlow.getCookie().equals(storedFlow.getCookie())) {
            return false;
        }
        if (statsFlow.getHardTimeout() == null) {
            if (storedFlow.getHardTimeout() != null) {
                return false;
            }
        } else if(!statsFlow.getHardTimeout().equals(storedFlow.getHardTimeout() )) {
            return false;
        }
        if (statsFlow.getIdleTimeout()== null) {
            if (storedFlow.getIdleTimeout() != null) {
                return false;
            }
        } else if(!statsFlow.getIdleTimeout().equals(storedFlow.getIdleTimeout())) {
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

}
