package org.opendaylight.controller.md.statistics.manager;

import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupDescRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupDescStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupFeaturesRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupStatsRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.all.statistics.GroupDescBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.all.statistics.GroupFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.all.statistics.GroupStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterConfigRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterConfigStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterFeaturesRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterStatsRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.meter.all.stats.MeterConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.meter.all.stats.MeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.meter.all.stats.MeterStatsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

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
        ConcurrentMap<NodeRef, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getNode())){
            cache.put(notification.getNode(), new NodeStatistics());
        }
        cache.get(notification.getNode()).setMeterConfigStats(notification.getMeterConfigStats());
        
        //Publish data to configuration data store
        DataModificationTransaction it = this.statisticsManager.startChange();
        MeterConfigRef ref = notification.getMeterConfigId();

        MeterConfigBuilder meterConfig = new MeterConfigBuilder();
        meterConfig.setNode(notification.getNode());
        meterConfig.setMeterConfigStats(notification.getMeterConfigStats());
        
        InstanceIdentifier<? extends Object> refValue = ref.getValue();
        it.putRuntimeData(refValue, meterConfig.build());
        it.commit();
    }

    @Override
    public void onMeterStatisticsUpdated(MeterStatisticsUpdated notification) {
        //Add statistics to local cache
        ConcurrentMap<NodeRef, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getNode())){
            cache.put(notification.getNode(), new NodeStatistics());
        }
        cache.get(notification.getNode()).setMeterStatistics(notification.getMeterStatistics());
        
        //Publish data to configuration data store
        DataModificationTransaction it = this.statisticsManager.startChange();
        MeterStatsRef ref = notification.getMeterStatsId();

        MeterStatsBuilder meterStats = new MeterStatsBuilder();
        meterStats.setNode(notification.getNode());
        meterStats.setMeterStatistics(notification.getMeterStatistics());
        
        InstanceIdentifier<? extends Object> refValue = ref.getValue();
        it.putRuntimeData(refValue, meterStats.build());
        it.commit();
    }

    @Override
    public void onGroupDescStatsUpdated(GroupDescStatsUpdated notification) {
        //Add statistics to local cache
        ConcurrentMap<NodeRef, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getNode())){
            cache.put(notification.getNode(), new NodeStatistics());
        }
        cache.get(notification.getNode()).setGroupDescStats(notification.getGroupDescStats());
        
        //Publish data to configuration data store
        DataModificationTransaction it = this.statisticsManager.startChange();
        GroupDescRef ref = notification.getGroupDescId();

        GroupDescBuilder descStats = new GroupDescBuilder();
        descStats.setNode(notification.getNode());
        descStats.setGroupDescStats(notification.getGroupDescStats());
        
        InstanceIdentifier<? extends Object> refValue = ref.getValue();
        it.putRuntimeData(refValue, descStats.build());
        it.commit();
    }

    @Override
    public void onGroupStatisticsUpdated(GroupStatisticsUpdated notification) {
        
        //Add statistics to local cache
        ConcurrentMap<NodeRef, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getNode())){
            cache.put(notification.getNode(), new NodeStatistics());
        }
        cache.get(notification.getNode()).setGroupStatistics(notification.getGroupStatistics());
        
        //Publish data to configuration data store
        DataModificationTransaction it = this.statisticsManager.startChange();
        GroupStatsRef ref = notification.getGroupStatsId();

        GroupStatsBuilder groupStats = new GroupStatsBuilder();
        groupStats.setNode(notification.getNode());
        groupStats.setGroupStatistics(notification.getGroupStatistics());
        
        InstanceIdentifier<? extends Object> refValue = ref.getValue();
        it.putRuntimeData(refValue, groupStats.build());
        it.commit();
    }
    @Override
    public void onMeterFeaturesUpdated(MeterFeaturesUpdated notification) {

        //Add statistics to local cache
        ConcurrentMap<NodeRef, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getNode())){
            cache.put(notification.getNode(), new NodeStatistics());
        }
        cache.get(notification.getNode()).setMeterFeatures(notification.getMeterFeatures());
        
        //Publish data to configuration data store
        DataModificationTransaction it = this.statisticsManager.startChange();
        MeterFeaturesRef ref = notification.getMeterFeaturesId();

        MeterFeaturesBuilder meterFeatures = new MeterFeaturesBuilder();
        meterFeatures.setNode(notification.getNode());
        meterFeatures.setMeterFeatures(notification.getMeterFeatures());
        
        InstanceIdentifier<? extends Object> refValue = ref.getValue();
        it.putRuntimeData(refValue, meterFeatures.build());
        it.commit();
    }
    
    @Override
    public void onGroupFeaturesUpdated(GroupFeaturesUpdated notification) {
        
        //Add statistics to local cache
        ConcurrentMap<NodeRef, NodeStatistics> cache = this.statisticsManager.getStatisticsCache();
        if(!cache.containsKey(notification.getNode())){
            cache.put(notification.getNode(), new NodeStatistics());
        }
        cache.get(notification.getNode()).setGroupFeatures(notification.getGroupFeatures());
        
        //Publish data to configuration data store
        DataModificationTransaction it = this.statisticsManager.startChange();
        GroupFeaturesRef ref = notification.getGroupFeaturesId();

        GroupFeaturesBuilder featuresStats = new GroupFeaturesBuilder();
        featuresStats.setNode(notification.getNode());
        featuresStats.setGroupFeatures(notification.getGroupFeatures());
        
        InstanceIdentifier<? extends Object> refValue = ref.getValue();
        it.putRuntimeData(refValue, featuresStats.build());
        it.commit();
    }
}
