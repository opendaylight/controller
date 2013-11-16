/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.nodes.node.GroupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.desc.stats.reply.GroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.reply.GroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStats;

public class NodeStatistics {

    private NodeRef targetNode;
    
    private List<GroupStats> groupStatistics;
    
    private List<MeterStats> meterStatistics;
    
    private List<GroupDescStats> groupDescStats;
    
    private List<MeterConfigStats> meterConfigStats;
    
    private GroupFeatures groupFeatures;
    
    private MeterFeatures meterFeatures;
    
    public NodeStatistics(){
        
    }

    public NodeRef getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(NodeRef targetNode) {
        this.targetNode = targetNode;
    }

    public List<GroupStats> getGroupStatistics() {
        return groupStatistics;
    }

    public void setGroupStatistics(List<GroupStats> groupStatistics) {
        this.groupStatistics = groupStatistics;
    }

    public List<MeterStats> getMeterStatistics() {
        return meterStatistics;
    }

    public void setMeterStatistics(List<MeterStats> meterStatistics) {
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

    public GroupFeatures getGroupFeatures() {
        return groupFeatures;
    }

    public void setGroupFeatures(GroupFeatures groupFeatures) {
        this.groupFeatures = groupFeatures;
    }

    public MeterFeatures getMeterFeatures() {
        return meterFeatures;
    }

    public void setMeterFeatures(MeterFeatures meterFeatures) {
        this.meterFeatures = meterFeatures;
    }
    
}
