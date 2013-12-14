/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.aggregate.flow.statistics.AggregateFlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.features.GroupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.desc.stats.reply.GroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.reply.GroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.GenericStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.GenericTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.NodeConnectorStatistics;

public class NodeStatistics {

    private NodeRef targetNode;
    
    private List<GroupStats> groupStatistics;
    
    private List<MeterStats> meterStatistics;
    
    private List<GroupDescStats> groupDescStats;
    
    private List<MeterConfigStats> meterConfigStats;
    
    private GroupFeatures groupFeatures;
    
    private MeterFeatures meterFeatures;
    
    private final Map<Short,Map<Flow,GenericStatistics>> flowAndStatsMap= 
            new HashMap<Short,Map<Flow,GenericStatistics>>();
    
    private final Map<Short,AggregateFlowStatistics> tableAndAggregateFlowStatsMap = 
            new HashMap<Short,AggregateFlowStatistics>();
    
    private final Map<NodeConnectorId,NodeConnectorStatistics> nodeConnectorStats = 
            new ConcurrentHashMap<NodeConnectorId,NodeConnectorStatistics>();
    
    private final Map<Short,GenericTableStatistics> flowTableAndStatisticsMap = 
            new HashMap<Short,GenericTableStatistics>();
    
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

    public Map<Short,Map<Flow,GenericStatistics>> getFlowAndStatsMap() {
        return flowAndStatsMap;
    }

    public Map<Short, GenericTableStatistics> getFlowTableAndStatisticsMap() {
        return flowTableAndStatisticsMap;
    }

    public Map<Short, AggregateFlowStatistics> getTableAndAggregateFlowStatsMap() {
        return tableAndAggregateFlowStatsMap;
    }
    public Map<NodeConnectorId, NodeConnectorStatistics> getNodeConnectorStats() {
        return nodeConnectorStats;
    }
}
