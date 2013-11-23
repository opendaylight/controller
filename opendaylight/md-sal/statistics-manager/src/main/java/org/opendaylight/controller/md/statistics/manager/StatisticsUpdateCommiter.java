/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.nodes.node.GroupDescBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.nodes.node.GroupFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.nodes.node.GroupStatisticsBuilder;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;

public class StatisticsUpdateCommiter implements OpendaylightGroupStatisticsListener,
        OpendaylightMeterStatisticsListener {
    
    private final StatisticsProvider statisticsManager;

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
        it.putRuntimeData(refValue, nodeData.build());
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
        it.putRuntimeData(refValue, nodeData.build());
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
        it.putRuntimeData(refValue, nodeData.build());
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
        it.putRuntimeData(refValue, nodeData.build());
        it.commit();
    }
    
    @Override
    public void onMeterFeaturesUpdated(MeterFeaturesUpdated notification) {

        //Add statistics to local cache
        ConcurrentMap<NodeId, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getId())){
            cache.put(notification.getId(), new NodeStatistics());
        }
        MeterFeaturesBuilder meterFeature = new MeterFeaturesBuilder();
        meterFeature.setBandTypes(notification.getBandTypes());
        meterFeature.setCapabilities(notification.getCapabilities());
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
        it.putRuntimeData(refValue, nodeData.build());
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
        groupFeatures.setCapabilities(notification.getCapabilities());
        groupFeatures.setTypes(notification.getTypes());
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
        it.putRuntimeData(refValue, nodeData.build());
        it.commit();
    }

    private NodeRef getNodeRef(NodeKey nodeKey){
        InstanceIdentifierBuilder<?> builder = InstanceIdentifier.builder().node(Nodes.class);
        return new NodeRef(builder.node(Node.class,nodeKey).toInstance());
    }

}
