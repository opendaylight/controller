package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupDescStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterConfigStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsListener;

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
        // TODO Auto-generated method stub
    }

    @Override
    public void onMeterStatisticsUpdated(MeterStatisticsUpdated notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGroupDescStatsUpdated(GroupDescStatsUpdated notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGroupStatisticsUpdated(GroupStatisticsUpdated notification) {
        // TODO Auto-generated method stub

    }
    @Override
    public void onMeterFeaturesUpdated(MeterFeaturesUpdated notification) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void onGroupFeaturesUpdated(GroupFeaturesUpdated notification) {
        // TODO Auto-generated method stub
        
    }

}
