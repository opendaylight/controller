/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.queue.rev130925.QueueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.desc.stats.reply.GroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;

import com.google.common.base.Preconditions;

/**
 * Main responsibility of this class to clean up all the stale statistics data
 * associated to Flow,Meter,Group,Queue.
 * @author avishnoi@in.ibm.com
 *
 */
public class NodeStatisticsAger {
    private static final int NUMBER_OF_WAIT_CYCLES = 2;

    private final Map<GroupDescStats,Long> groupDescStatsUpdate = new HashMap<>();
    private final Map<MeterConfigStats,Long> meterConfigStatsUpdate = new HashMap<>();
    private final Map<FlowEntry,Long> flowStatsUpdate = new HashMap<>();
    private final Map<QueueEntry,Long> queuesStatsUpdate = new HashMap<>();
    private final StatisticsProvider statisticsProvider;
    private final NodeKey targetNodeKey;

    public NodeStatisticsAger(StatisticsProvider statisticsProvider, NodeKey nodeKey){
        this.statisticsProvider = Preconditions.checkNotNull(statisticsProvider);
        this.targetNodeKey = Preconditions.checkNotNull(nodeKey);
    }

    public class FlowEntry {
        private final Short tableId;
        private final Flow flow;

        public FlowEntry(Short tableId, Flow flow){
            this.tableId = tableId;
            this.flow = flow;
        }

        public Short getTableId() {
            return tableId;
        }

        public Flow getFlow() {
            return flow;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((flow == null) ? 0 : flow.hashCode());
            result = prime * result + ((tableId == null) ? 0 : tableId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FlowEntry other = (FlowEntry) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (flow == null) {
                if (other.flow != null)
                    return false;
            } else if (!flow.equals(other.flow))
                return false;
            if (tableId == null) {
                if (other.tableId != null)
                    return false;
            } else if (!tableId.equals(other.tableId))
                return false;
            return true;
        }

        private NodeStatisticsAger getOuterType() {
            return NodeStatisticsAger.this;
        }
    }

    public class QueueEntry{
        private final NodeConnectorId nodeConnectorId;
        private final QueueId queueId;
        public QueueEntry(NodeConnectorId ncId, QueueId queueId){
            this.nodeConnectorId = ncId;
            this.queueId = queueId;
        }
        public NodeConnectorId getNodeConnectorId() {
            return nodeConnectorId;
        }
        public QueueId getQueueId() {
            return queueId;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((nodeConnectorId == null) ? 0 : nodeConnectorId.hashCode());
            result = prime * result + ((queueId == null) ? 0 : queueId.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof QueueEntry)) {
                return false;
            }
            QueueEntry other = (QueueEntry) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (nodeConnectorId == null) {
                if (other.nodeConnectorId != null) {
                    return false;
                }
            } else if (!nodeConnectorId.equals(other.nodeConnectorId)) {
                return false;
            }
            if (queueId == null) {
                if (other.queueId != null) {
                    return false;
                }
            } else if (!queueId.equals(other.queueId)) {
                return false;
            }
            return true;
        }
        private NodeStatisticsAger getOuterType() {
            return NodeStatisticsAger.this;
        }
    }

    public NodeKey getTargetNodeKey() {
        return targetNodeKey;
    }

    public synchronized void updateGroupDescStats(List<GroupDescStats> list){
        Long expiryTime = getExpiryTime();
        for(GroupDescStats groupDescStats : list)
            this.groupDescStatsUpdate.put(groupDescStats, expiryTime);
    }

    public synchronized void updateMeterConfigStats(List<MeterConfigStats> list){
        Long expiryTime = getExpiryTime();
        for(MeterConfigStats meterConfigStats: list)
            this.meterConfigStatsUpdate.put(meterConfigStats, expiryTime);
    }

    public synchronized void updateFlowStats(FlowEntry flowEntry){
        this.flowStatsUpdate.put(flowEntry, getExpiryTime());
    }
    public synchronized void updateQueueStats(QueueEntry queueEntry){
        this.queuesStatsUpdate.put(queueEntry, getExpiryTime());
    }

    private static Long getExpiryTime(){
        final long now = System.nanoTime();
        return now + TimeUnit.MILLISECONDS.toNanos(StatisticsProvider.STATS_THREAD_EXECUTION_TIME * NUMBER_OF_WAIT_CYCLES);
    }

    public synchronized void cleanStaleStatistics(){
        final DataModificationTransaction trans = this.statisticsProvider.startChange();
        final long now = System.nanoTime();

        //Clean stale statistics related to group
        for (Iterator<Entry<GroupDescStats, Long>> it = this.groupDescStatsUpdate.entrySet().iterator();it.hasNext();){
            Entry<GroupDescStats, Long> e = it.next();
            if (now > e.getValue()) {
                cleanGroupStatsFromDataStore(trans, e.getKey());
                it.remove();
            }
        }

        //Clean stale statistics related to meter
        for (Iterator<Entry<MeterConfigStats, Long>> it = this.meterConfigStatsUpdate.entrySet().iterator();it.hasNext();){
            Entry<MeterConfigStats, Long> e = it.next();
            if (now > e.getValue()) {
                cleanMeterStatsFromDataStore(trans, e.getKey());
                it.remove();
            }
        }

        //Clean stale statistics related to flow
        for (Iterator<Entry<FlowEntry, Long>> it = this.flowStatsUpdate.entrySet().iterator();it.hasNext();){
            Entry<FlowEntry, Long> e = it.next();
            if (now > e.getValue()) {
                cleanFlowStatsFromDataStore(trans, e.getKey());
                it.remove();
            }
        }

        //Clean stale statistics related to queue
        for (Iterator<Entry<QueueEntry, Long>> it = this.queuesStatsUpdate.entrySet().iterator();it.hasNext();){
            Entry<QueueEntry, Long> e = it.next();
            if (now > e.getValue()) {
                cleanQueueStatsFromDataStore(trans, e.getKey());
                it.remove();
            }
        }

        trans.commit();
    }

    private void cleanQueueStatsFromDataStore(DataModificationTransaction trans, QueueEntry queueEntry) {
        InstanceIdentifier<?> queueRef
                        = InstanceIdentifier.builder(Nodes.class)
                                            .child(Node.class, this.targetNodeKey)
                                            .child(NodeConnector.class, new NodeConnectorKey(queueEntry.getNodeConnectorId()))
                                            .augmentation(FlowCapableNodeConnector.class)
                                            .child(Queue.class, new QueueKey(queueEntry.getQueueId()))
                                            .augmentation(FlowCapableNodeConnectorQueueStatisticsData.class).toInstance();
        trans.removeOperationalData(queueRef);
    }

    private void cleanFlowStatsFromDataStore(DataModificationTransaction trans, FlowEntry flowEntry) {
        InstanceIdentifier<?> flowRef
                        = InstanceIdentifier.builder(Nodes.class).child(Node.class, this.targetNodeKey)
                                            .augmentation(FlowCapableNode.class)
                                            .child(Table.class, new TableKey(flowEntry.getTableId()))
                                            .child(Flow.class,flowEntry.getFlow().getKey())
                                            .augmentation(FlowStatisticsData.class).toInstance();
        trans.removeOperationalData(flowRef);
    }

    private void cleanMeterStatsFromDataStore(DataModificationTransaction trans, MeterConfigStats meterConfigStats) {
        InstanceIdentifierBuilder<Meter> meterRef
                        = InstanceIdentifier.builder(Nodes.class).child(Node.class,this.targetNodeKey)
                                            .augmentation(FlowCapableNode.class)
                                            .child(Meter.class,new MeterKey(meterConfigStats.getMeterId()));

        InstanceIdentifier<?> nodeMeterConfigStatsAugmentation = meterRef.augmentation(NodeMeterConfigStats.class).toInstance();
        trans.removeOperationalData(nodeMeterConfigStatsAugmentation);

        InstanceIdentifier<?> nodeMeterStatisticsAugmentation = meterRef.augmentation(NodeMeterStatistics.class).toInstance();
        trans.removeOperationalData(nodeMeterStatisticsAugmentation);
    }

    private void cleanGroupStatsFromDataStore(DataModificationTransaction trans, GroupDescStats groupDescStats) {
        InstanceIdentifierBuilder<Group> groupRef
                        = InstanceIdentifier.builder(Nodes.class).child(Node.class,this.targetNodeKey)
                                            .augmentation(FlowCapableNode.class)
                                            .child(Group.class,new GroupKey(groupDescStats.getGroupId()));

        InstanceIdentifier<?> nodeGroupDescStatsAugmentation = groupRef.augmentation(NodeGroupDescStats.class).toInstance();
        trans.removeOperationalData(nodeGroupDescStatsAugmentation);

        InstanceIdentifier<?> nodeGroupStatisticsAugmentation = groupRef.augmentation(NodeGroupStatistics.class).toInstance();
        trans.removeOperationalData(nodeGroupStatisticsAugmentation);
    }
}
