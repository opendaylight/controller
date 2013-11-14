package org.opendaylight.controller.md.statistics.manager;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.desc.response.GroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.stats.response.GroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.meter.config.response.MeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.meter.stats.response.MeterStatistics;

public class NodeStatistics {

    private NodeRef targetNode;
    
    private List<GroupStatistics> groupStatistics;
    
    private List<MeterStatistics> meterStatistics;
    
    private List<GroupDescStats> groupDescStats;
    
    private List<MeterConfigStats> meterConfigStats;
    
    private List<org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.features.GroupFeatures> groupFeatures;
    
    private List<org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.features.MeterFeatures> meterFeatures;
    
    public NodeStatistics(){
        
    }

    public NodeRef getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(NodeRef targetNode) {
        this.targetNode = targetNode;
    }

    public List<GroupStatistics> getGroupStatistics() {
        return groupStatistics;
    }

    public void setGroupStatistics(List<GroupStatistics> groupStatistics) {
        this.groupStatistics = groupStatistics;
    }

    public List<MeterStatistics> getMeterStatistics() {
        return meterStatistics;
    }

    public void setMeterStatistics(List<MeterStatistics> meterStatistics) {
        this.meterStatistics = meterStatistics;
    }

    public List<GroupDescStats> getGroupDescStats() {
        return groupDescStats;
    }

    public void setGroupDescStats(List<GroupDescStats> groupDescStats) {
        this.groupDescStats = groupDescStats;
    }

    public List<MeterConfigStats> getMeterConfigStats() {
        return meterConfigStats;
    }

    public void setMeterConfigStats(List<MeterConfigStats> meterConfigStats) {
        this.meterConfigStats = meterConfigStats;
    }

    public List<org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.features.GroupFeatures> getGroupFeatures() {
        return groupFeatures;
    }

    public void setGroupFeatures(
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.features.GroupFeatures> groupFeatures) {
        this.groupFeatures = groupFeatures;
    }

    public List<org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.features.MeterFeatures> getMeterFeatures() {
        return meterFeatures;
    }

    public void setMeterFeatures(
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.features.MeterFeatures> meterFeatures) {
        this.meterFeatures = meterFeatures;
    }
    
}
