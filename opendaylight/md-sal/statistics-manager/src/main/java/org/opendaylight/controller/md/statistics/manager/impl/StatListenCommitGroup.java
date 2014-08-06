/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.statistics.manager.StatDeviceMsgManager.TransactionCacheContainer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupDescStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.desc.GroupDescBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.desc.stats.reply.GroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.reply.GroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * StatListenCommitGroup
 * Class is a NotifyListener for GroupStatistics and DataChangeListener for Config/DataStore for Group node.
 * All expected (registered) GroupStatistics will be builded and commit to Operational/DataStore.
 * DataChangeEven should call create/delete Group in Operational/DS
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
class StatListenCommitGroup extends StatAbstractListenCommit<Group, OpendaylightGroupStatisticsListener>
                                                    implements OpendaylightGroupStatisticsListener {

    private static final Logger LOG = LoggerFactory.getLogger(StatListenCommitMeter.class);

    public StatListenCommitGroup(final StatisticsManager manager,  final DataBroker db,
            final NotificationProviderService nps) {
        super(manager, db, nps, Group.class);
    }

    @Override
    protected OpendaylightGroupStatisticsListener getStatNotificationListener() {
        return this;
    }

    @Override
    protected InstanceIdentifier<Group> getWildCardedRegistrationPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class)
                .augmentation(FlowCapableNode.class).child(Group.class);
    }

    @Override
    public void createStat(final InstanceIdentifier<Group> keyIdent, final Group data,
            final InstanceIdentifier<FlowCapableNode> nodeIdent) {
        /* GroupId is a marker for a way to submit data to Operational/DS */
        manager.getDeviceMsgManager()
            .getGroupStat(new NodeRef(nodeIdent), data.getGroupId());
    }

    @Override
    public void removeStat(final InstanceIdentifier<Group> keyIdent) {
        final WriteTransaction trans = manager.getWriteTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, keyIdent);
        trans.submit();
    }

    @Override
    public void onGroupDescStatsUpdated(final GroupDescStatsUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! manager.getDeviceMsgManager().isExpectedStatistics(transId)) {
            LOG.warn("STAT-MANAGER - GroupDescStatsUpdated: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getDeviceMsgManager().addNotification(notification);
        }
        final NodeId nodeId = notification.getId();
        final List<GroupDescStats> groupStats = notification.getGroupDescStats() == null
                ? new ArrayList<GroupDescStats>(10) : new ArrayList<>(notification.getGroupDescStats());
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getDeviceMsgManager().getTransactionCacheContainer(transId);
        if (txContainer.isPresent()) {
            final List<? extends TransactionAware> cacheNotifs =
                    txContainer.get().getNotifications();
            for (final TransactionAware notif : cacheNotifs) {
                if (notif instanceof GroupDescStatsUpdated) {
                    groupStats.addAll(((GroupDescStatsUpdated) notif).getGroupDescStats());
                }
            }
        }
        statGroupDescCommit(groupStats, nodeId);
    }

    @Override
    public void onGroupFeaturesUpdated(final GroupFeaturesUpdated notification) {
        // NOOP - Group Fueaturs - JOB for Inventory Manager
        final TransactionId transId = notification.getTransactionId();
        if ( ! manager.getDeviceMsgManager().isExpectedStatistics(transId)) {
            LOG.trace("STAT-MANAGER - GroupFeaturesUpdated: unregistred notification detect TransactionId {}", transId);
        } else {
            LOG.error("STAT-MANAGER - GroupFeaturesUpdated: unimplement registred notification detect TransactionId {}", transId);
        }
    }

    @Override
    public void onGroupStatisticsUpdated(final GroupStatisticsUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! manager.getDeviceMsgManager().isExpectedStatistics(transId)) {
            LOG.warn("STAT-MANAGER - GroupStatisticsUpdated: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getDeviceMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<GroupStats> groupStats = notification.getGroupStats() == null
                ? new ArrayList<GroupStats>(10) : new ArrayList<>(notification.getGroupStats());
        Optional<Group> group = Optional.absent();
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getDeviceMsgManager().getTransactionCacheContainer(transId);
        if (txContainer.isPresent()) {
            final Optional<DataObject> inputObj = txContainer.get().getConfInput();
            if (inputObj.isPresent() && inputObj.get() instanceof Group) {
                group = Optional.<Group> of((Group)inputObj.get());
            }
            final List<? extends TransactionAware> cacheNotifs =
                    txContainer.get().getNotifications();
            for (final TransactionAware notif : cacheNotifs) {
                if (notif instanceof GroupStatisticsUpdated) {
                    groupStats.addAll(((GroupStatisticsUpdated) notif).getGroupStats());
                }
            }
        }
        statGroupCommit(groupStats, nodeId, group);
    }

    private void statGroupCommit(final List<GroupStats> groupStats, final NodeId nodeId,
            final Optional<Group> group) {
        final WriteTransaction trans = manager.getWriteTransaction();
        final InstanceIdentifier<FlowCapableNode> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId)).augmentation(FlowCapableNode.class);

        for (final GroupStats groupStat : groupStats) {
            final GroupBuilder groupBuilder = new GroupBuilder();
            final GroupKey groupKey = new GroupKey(groupStat.getGroupId());
            groupBuilder.setKey(groupKey);
            final InstanceIdentifier<Group> groupRef = nodeIdent.child(Group.class,groupKey);

            final NodeGroupStatisticsBuilder groupStatisticsBuilder = new NodeGroupStatisticsBuilder();
            groupStatisticsBuilder.setGroupStatistics(new GroupStatisticsBuilder(groupStat).build());
            //Update augmented data
            groupBuilder.addAugmentation(NodeGroupStatistics.class, groupStatisticsBuilder.build());
            trans.merge(LogicalDatastoreType.OPERATIONAL, groupRef, groupBuilder.build(), true);
        }
        if (group.isPresent()) {
            trans.submit();
        } else {
            continueStatCollecting(trans.submit());
        }
    }

    private void statGroupDescCommit(final List<GroupDescStats> groupStats, final NodeId nodeId) {
        final ReadWriteTransaction trans = manager.getReadWriteTransaction();
        final InstanceIdentifier<FlowCapableNode> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId)).augmentation(FlowCapableNode.class);

        final Optional<FlowCapableNode> flowNode = getFlowCapableNodeFromOperational(trans, nodeIdent, true);
        if ( ! flowNode.isPresent()) {
            LOG.warn("FlowCapableNode {} is not presented in Operational/DS. Statistics can not be updated.", nodeIdent);
        }

        final List<InstanceIdentifier<Group>> nodeExistedGroupPaths = new ArrayList<>();
        /* Add all existed groups paths - no updated paths has to be removed */
        for (final Group group : flowNode.get().getGroup()) {
            nodeExistedGroupPaths.add(nodeIdent.child(Group.class, group.getKey()));
        }


        for (final GroupDescStats group : groupStats) {
            final GroupBuilder groupBuilder = new GroupBuilder();
            final GroupKey groupKey = new GroupKey(group.getGroupId());
            groupBuilder.setKey(groupKey);

            final InstanceIdentifier<Group> groupRef = nodeIdent.child(Group.class,groupKey);

            final NodeGroupDescStatsBuilder groupDesc= new NodeGroupDescStatsBuilder();
            groupDesc.setGroupDesc(new GroupDescBuilder(group).build());
            //Update augmented data
            groupBuilder.addAugmentation(NodeGroupDescStats.class, groupDesc.build());
            if (nodeExistedGroupPaths.contains(groupRef)) {
                nodeExistedGroupPaths.remove(groupRef);
            }
            trans.merge(LogicalDatastoreType.OPERATIONAL, groupRef, groupBuilder.build(), true);
        }
        cleaningOperationalDS(trans, true, nodeExistedGroupPaths, nodeIdent);
    }
}

