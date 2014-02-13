/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.aggregate.flow.statistics.AggregateFlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.queue.rev130925.QueueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.desc.GroupDescBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.features.GroupFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.desc.stats.reply.GroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.reply.GroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterConfigStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.AggregateFlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.GenericStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.queue.id.and.statistics.map.QueueIdAndStatisticsMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * This class handles the lifecycle of per-node statistics. It receives data
 * from StatisticsListener, stores it in the data store and keeps track of
 * when the data should be removed.
 *
 * @author avishnoi@in.ibm.com
 */
public final class NodeStatisticsHandler implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(NodeStatisticsHandler.class);
    private static final int NUMBER_OF_WAIT_CYCLES = 2;

    private final Map<GroupDescStats,Long> groupDescStatsUpdate = new HashMap<>();
    private final Map<MeterConfigStats,Long> meterConfigStatsUpdate = new HashMap<>();
    private final Map<FlowEntry,Long> flowStatsUpdate = new HashMap<>();
    private final Map<QueueEntry,Long> queuesStatsUpdate = new HashMap<>();
    private final InstanceIdentifier<Node> targetNodeIdentifier;
    private final DataProviderService dps;
    private final NodeRef targetNodeRef;
    private final NodeKey targetNodeKey;
    private Collection<TableKey> knownTables = Collections.emptySet();
    private int unaccountedFlowsCounter = 1;

    public NodeStatisticsHandler(final DataProviderService dps, final NodeKey nodeKey) {
        this.dps = Preconditions.checkNotNull(dps);
        this.targetNodeKey = Preconditions.checkNotNull(nodeKey);
        this.targetNodeIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey).build();
        this.targetNodeRef = new NodeRef(targetNodeIdentifier);
    }

    private static class FlowEntry {
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
    }

    private static final class QueueEntry {
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
    }

    public NodeKey getTargetNodeKey() {
        return targetNodeKey;
    }

    public Collection<TableKey> getKnownTables() {
        return knownTables;
    }

    public InstanceIdentifier<Node> getTargetNodeIdentifier() {
        return targetNodeIdentifier;
    }

    public NodeRef getTargetNodeRef() {
        return targetNodeRef;
    }

    public synchronized void updateGroupDescStats(List<GroupDescStats> list){
        final Long expiryTime = getExpiryTime();
        final DataModificationTransaction trans = dps.beginTransaction();

        for (GroupDescStats groupDescStats : list) {
            GroupBuilder groupBuilder = new GroupBuilder();
            GroupKey groupKey = new GroupKey(groupDescStats.getGroupId());
            groupBuilder.setKey(groupKey);

            InstanceIdentifier<Group> groupRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey)
                                                                                        .augmentation(FlowCapableNode.class)
                                                                                        .child(Group.class,groupKey).toInstance();

            NodeGroupDescStatsBuilder groupDesc= new NodeGroupDescStatsBuilder();
            GroupDescBuilder stats = new GroupDescBuilder();
            stats.fieldsFrom(groupDescStats);
            groupDesc.setGroupDesc(stats.build());

            //Update augmented data
            groupBuilder.addAugmentation(NodeGroupDescStats.class, groupDesc.build());

            trans.putOperationalData(groupRef, groupBuilder.build());
            this.groupDescStatsUpdate.put(groupDescStats, expiryTime);
        }

        trans.commit();
    }

    public synchronized void updateGroupStats(List<GroupStats> list) {
        final DataModificationTransaction trans = dps.beginTransaction();

        for(GroupStats groupStats : list) {
            GroupBuilder groupBuilder = new GroupBuilder();
            GroupKey groupKey = new GroupKey(groupStats.getGroupId());
            groupBuilder.setKey(groupKey);

            InstanceIdentifier<Group> groupRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey)
                                                                                        .augmentation(FlowCapableNode.class)
                                                                                        .child(Group.class,groupKey).toInstance();

            NodeGroupStatisticsBuilder groupStatisticsBuilder= new NodeGroupStatisticsBuilder();
            GroupStatisticsBuilder stats = new GroupStatisticsBuilder();
            stats.fieldsFrom(groupStats);
            groupStatisticsBuilder.setGroupStatistics(stats.build());

            //Update augmented data
            groupBuilder.addAugmentation(NodeGroupStatistics.class, groupStatisticsBuilder.build());
            trans.putOperationalData(groupRef, groupBuilder.build());

            // FIXME: should we be tracking this data?
        }

        trans.commit();
    }

    public synchronized void updateMeterConfigStats(List<MeterConfigStats> list) {
        final Long expiryTime = getExpiryTime();
        final DataModificationTransaction trans = dps.beginTransaction();

        for(MeterConfigStats meterConfigStats : list) {
            MeterBuilder meterBuilder = new MeterBuilder();
            MeterKey meterKey = new MeterKey(meterConfigStats.getMeterId());
            meterBuilder.setKey(meterKey);

            InstanceIdentifier<Meter> meterRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey)
                                                                                        .augmentation(FlowCapableNode.class)
                                                                                        .child(Meter.class,meterKey).toInstance();

            NodeMeterConfigStatsBuilder meterConfig= new NodeMeterConfigStatsBuilder();
            MeterConfigStatsBuilder stats = new MeterConfigStatsBuilder();
            stats.fieldsFrom(meterConfigStats);
            meterConfig.setMeterConfigStats(stats.build());

            //Update augmented data
            meterBuilder.addAugmentation(NodeMeterConfigStats.class, meterConfig.build());

            trans.putOperationalData(meterRef, meterBuilder.build());
            this.meterConfigStatsUpdate.put(meterConfigStats, expiryTime);
        }

        trans.commit();
    }


    public synchronized void updateMeterStats(List<MeterStats> list) {
        final DataModificationTransaction trans = dps.beginTransaction();

        for(MeterStats meterStats : list) {
            MeterBuilder meterBuilder = new MeterBuilder();
            MeterKey meterKey = new MeterKey(meterStats.getMeterId());
            meterBuilder.setKey(meterKey);

            InstanceIdentifier<Meter> meterRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey)
                                                                                        .augmentation(FlowCapableNode.class)
                                                                                        .child(Meter.class,meterKey).toInstance();

            NodeMeterStatisticsBuilder meterStatsBuilder= new NodeMeterStatisticsBuilder();
            MeterStatisticsBuilder stats = new MeterStatisticsBuilder();
            stats.fieldsFrom(meterStats);
            meterStatsBuilder.setMeterStatistics(stats.build());

            //Update augmented data
            meterBuilder.addAugmentation(NodeMeterStatistics.class, meterStatsBuilder.build());
            trans.putOperationalData(meterRef, meterBuilder.build());

            // FIXME: should we be tracking this data?
        }

        trans.commit();
    }

    public synchronized void updateQueueStats(List<QueueIdAndStatisticsMap> list) {
        final Long expiryTime = getExpiryTime();
        final DataModificationTransaction trans = dps.beginTransaction();

        for (QueueIdAndStatisticsMap swQueueStats : list) {

            QueueEntry queueEntry = new QueueEntry(swQueueStats.getNodeConnectorId(),swQueueStats.getQueueId());

            FlowCapableNodeConnectorQueueStatisticsDataBuilder queueStatisticsDataBuilder = new FlowCapableNodeConnectorQueueStatisticsDataBuilder();

            FlowCapableNodeConnectorQueueStatisticsBuilder queueStatisticsBuilder = new FlowCapableNodeConnectorQueueStatisticsBuilder();

            queueStatisticsBuilder.fieldsFrom(swQueueStats);

            queueStatisticsDataBuilder.setFlowCapableNodeConnectorQueueStatistics(queueStatisticsBuilder.build());

            InstanceIdentifier<Queue> queueRef
                    = InstanceIdentifier.builder(Nodes.class)
                                        .child(Node.class, targetNodeKey)
                                        .child(NodeConnector.class, new NodeConnectorKey(swQueueStats.getNodeConnectorId()))
                                        .augmentation(FlowCapableNodeConnector.class)
                                        .child(Queue.class, new QueueKey(swQueueStats.getQueueId())).toInstance();

            QueueBuilder queueBuilder = new QueueBuilder();
            FlowCapableNodeConnectorQueueStatisticsData qsd = queueStatisticsDataBuilder.build();
            queueBuilder.addAugmentation(FlowCapableNodeConnectorQueueStatisticsData.class, qsd);
            queueBuilder.setKey(new QueueKey(swQueueStats.getQueueId()));

            logger.debug("Augmenting queue statistics {} of queue {} to port {}",
                                        qsd,
                                        swQueueStats.getQueueId(),
                                        swQueueStats.getNodeConnectorId());

            trans.putOperationalData(queueRef, queueBuilder.build());
            this.queuesStatsUpdate.put(queueEntry, expiryTime);
        }

        trans.commit();
    }

    public synchronized void updateFlowTableStats(List<FlowTableAndStatisticsMap> list) {
        final DataModificationTransaction trans = dps.beginTransaction();

        final Set<TableKey> knownTables = new HashSet<>(list.size());
        for (FlowTableAndStatisticsMap ftStats : list) {

            InstanceIdentifier<Table> tableRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey)
                    .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(ftStats.getTableId().getValue())).toInstance();

            FlowTableStatisticsDataBuilder statisticsDataBuilder = new FlowTableStatisticsDataBuilder();
            final FlowTableStatistics stats = new FlowTableStatisticsBuilder(ftStats).build();
            statisticsDataBuilder.setFlowTableStatistics(stats);

            logger.debug("Augment flow table statistics: {} for table {} on Node {}",
                    stats,ftStats.getTableId(), targetNodeKey);

            TableBuilder tableBuilder = new TableBuilder();
            tableBuilder.setKey(new TableKey(ftStats.getTableId().getValue()));
            tableBuilder.addAugmentation(FlowTableStatisticsData.class, statisticsDataBuilder.build());
            trans.putOperationalData(tableRef, tableBuilder.build());

            knownTables.add(tableBuilder.getKey());
        }

        this.knownTables = Collections.unmodifiableCollection(knownTables);
        trans.commit();
    }

    public synchronized void updateNodeConnectorStats(List<NodeConnectorStatisticsAndPortNumberMap> list) {
        final DataModificationTransaction trans = dps.beginTransaction();

        for(NodeConnectorStatisticsAndPortNumberMap portStats : list) {

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

            InstanceIdentifier<NodeConnector> nodeConnectorRef = InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, targetNodeKey)
                    .child(NodeConnector.class, new NodeConnectorKey(portStats.getNodeConnectorId())).toInstance();

            // FIXME: can we bypass this read?
            NodeConnector nodeConnector = (NodeConnector)trans.readOperationalData(nodeConnectorRef);
            if(nodeConnector != null){
                final FlowCapableNodeConnectorStatisticsData stats = statisticsDataBuilder.build();
                logger.debug("Augmenting port statistics {} to port {}",stats,nodeConnectorRef.toString());
                NodeConnectorBuilder nodeConnectorBuilder = new NodeConnectorBuilder();
                nodeConnectorBuilder.addAugmentation(FlowCapableNodeConnectorStatisticsData.class, stats);
                trans.putOperationalData(nodeConnectorRef, nodeConnectorBuilder.build());
            }

            // FIXME: should we be tracking this data?
        }

        trans.commit();
    }

    public synchronized void updateAggregateFlowStats(Short tableId, AggregateFlowStatistics flowStats) {
        if (tableId != null) {
            final DataModificationTransaction trans = dps.beginTransaction();


            InstanceIdentifier<Table> tableRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey)
                    .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(tableId)).toInstance();

            AggregateFlowStatisticsDataBuilder aggregateFlowStatisticsDataBuilder = new AggregateFlowStatisticsDataBuilder();
            AggregateFlowStatisticsBuilder aggregateFlowStatisticsBuilder = new AggregateFlowStatisticsBuilder(flowStats);

            aggregateFlowStatisticsDataBuilder.setAggregateFlowStatistics(aggregateFlowStatisticsBuilder.build());

            logger.debug("Augment aggregate statistics: {} for table {} on Node {}",
                    aggregateFlowStatisticsBuilder.build().toString(),tableId,targetNodeKey);

            TableBuilder tableBuilder = new TableBuilder();
            tableBuilder.setKey(new TableKey(tableId));
            tableBuilder.addAugmentation(AggregateFlowStatisticsData.class, aggregateFlowStatisticsDataBuilder.build());
            trans.putOperationalData(tableRef, tableBuilder.build());

            // FIXME: should we be tracking this data?
            trans.commit();
        }
    }

    public synchronized void updateGroupFeatures(GroupFeatures notification) {
        final DataModificationTransaction trans = dps.beginTransaction();

        final NodeBuilder nodeData = new NodeBuilder();
        nodeData.setKey(targetNodeKey);

        NodeGroupFeaturesBuilder nodeGroupFeatures = new NodeGroupFeaturesBuilder();
        GroupFeaturesBuilder groupFeatures = new GroupFeaturesBuilder(notification);
        nodeGroupFeatures.setGroupFeatures(groupFeatures.build());

        //Update augmented data
        nodeData.addAugmentation(NodeGroupFeatures.class, nodeGroupFeatures.build());
        trans.putOperationalData(targetNodeIdentifier, nodeData.build());

        // FIXME: should we be tracking this data?
        trans.commit();
    }

    public synchronized void updateMeterFeatures(MeterFeatures features) {
        final DataModificationTransaction trans = dps.beginTransaction();

        final NodeBuilder nodeData = new NodeBuilder();
        nodeData.setKey(targetNodeKey);

        NodeMeterFeaturesBuilder nodeMeterFeatures = new NodeMeterFeaturesBuilder();
        MeterFeaturesBuilder meterFeature = new MeterFeaturesBuilder(features);
        nodeMeterFeatures.setMeterFeatures(meterFeature.build());

        //Update augmented data
        nodeData.addAugmentation(NodeMeterFeatures.class, nodeMeterFeatures.build());
        trans.putOperationalData(targetNodeIdentifier, nodeData.build());

        // FIXME: should we be tracking this data?
        trans.commit();
    }

    public synchronized void updateFlowStats(List<FlowAndStatisticsMapList> list) {
        final Long expiryTime = getExpiryTime();
        final DataModificationTransaction trans = dps.beginTransaction();

        for(FlowAndStatisticsMapList map : list) {
            short tableId = map.getTableId();
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

            logger.debug("Flow : {}",flowRule.toString());
            logger.debug("Statistics to augment : {}",flowStatistics.build().toString());

            InstanceIdentifier<Table> tableRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey)
                    .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(tableId)).toInstance();

            Table table= (Table)trans.readConfigurationData(tableRef);

            //TODO: Not a good way to do it, need to figure out better way.
            //TODO: major issue in any alternate approach is that flow key is incrementally assigned
            //to the flows stored in data store.
            // Augment same statistics to all the matching masked flow
            if(table != null){

                for(Flow existingFlow : table.getFlow()){
                    logger.debug("Existing flow in data store : {}",existingFlow.toString());
                    if(FlowComparator.flowEquals(flowRule,existingFlow)){
                        InstanceIdentifier<Flow> flowRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey)
                                .augmentation(FlowCapableNode.class)
                                .child(Table.class, new TableKey(tableId))
                                .child(Flow.class,existingFlow.getKey()).toInstance();
                        flowBuilder.setKey(existingFlow.getKey());
                        flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
                        logger.debug("Found matching flow in the datastore, augmenting statistics");
                        foundOriginalFlow = true;
                        // Update entry with timestamp of latest response
                        flow.setKey(existingFlow.getKey());
                        FlowEntry flowStatsEntry = new FlowEntry(tableId,flow.build());
                        flowStatsUpdate.put(flowStatsEntry, expiryTime);

                        trans.putOperationalData(flowRef, flowBuilder.build());
                    }
                }
            }

            table = (Table)trans.readOperationalData(tableRef);
            if(!foundOriginalFlow && table != null){

                for(Flow existingFlow : table.getFlow()){
                    FlowStatisticsData augmentedflowStatisticsData = existingFlow.getAugmentation(FlowStatisticsData.class);
                    if(augmentedflowStatisticsData != null){
                        FlowBuilder existingOperationalFlow = new FlowBuilder();
                        existingOperationalFlow.fieldsFrom(augmentedflowStatisticsData.getFlowStatistics());
                        logger.debug("Existing unaccounted flow in operational data store : {}",existingFlow.toString());
                        if(FlowComparator.flowEquals(flowRule,existingOperationalFlow.build())){
                            InstanceIdentifier<Flow> flowRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey)
                                    .augmentation(FlowCapableNode.class)
                                    .child(Table.class, new TableKey(tableId))
                                    .child(Flow.class,existingFlow.getKey()).toInstance();
                            flowBuilder.setKey(existingFlow.getKey());
                            flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
                            logger.debug("Found matching unaccounted flow in the operational datastore, augmenting statistics");
                            foundOriginalFlow = true;

                            // Update entry with timestamp of latest response
                            flow.setKey(existingFlow.getKey());
                            FlowEntry flowStatsEntry = new FlowEntry(tableId,flow.build());
                            flowStatsUpdate.put(flowStatsEntry, expiryTime);
                            trans.putOperationalData(flowRef, flowBuilder.build());
                            break;
                        }
                    }
                }
            }
            if(!foundOriginalFlow){
                String flowKey = "#UF$TABLE*"+Short.toString(tableId)+"*"+Integer.toString(this.unaccountedFlowsCounter);
                this.unaccountedFlowsCounter++;
                FlowKey newFlowKey = new FlowKey(new FlowId(flowKey));
                InstanceIdentifier<Flow> flowRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, targetNodeKey)
                        .augmentation(FlowCapableNode.class)
                        .child(Table.class, new TableKey(tableId))
                        .child(Flow.class,newFlowKey).toInstance();
                flowBuilder.setKey(newFlowKey);
                flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
                logger.debug("Flow {} is not present in config data store, augmenting statistics as an unaccounted flow",
                        flowBuilder.build());

                // Update entry with timestamp of latest response
                flow.setKey(newFlowKey);
                FlowEntry flowStatsEntry = new FlowEntry(tableId,flow.build());
                flowStatsUpdate.put(flowStatsEntry, expiryTime);
                trans.putOperationalData(flowRef, flowBuilder.build());
            }
        }

        trans.commit();
    }

    private static Long getExpiryTime(){
        final long now = System.nanoTime();
        return now + TimeUnit.MILLISECONDS.toNanos(StatisticsProvider.STATS_COLLECTION_MILLIS * NUMBER_OF_WAIT_CYCLES);
    }

    public synchronized void cleanStaleStatistics(){
        final DataModificationTransaction trans = dps.beginTransaction();
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

    @Override
    public void close() {
        // FIXME: cleanup any resources we hold (registrations, etc.)
        logger.debug("Statistics handler for {} shut down", targetNodeKey.getId());
    }
}
