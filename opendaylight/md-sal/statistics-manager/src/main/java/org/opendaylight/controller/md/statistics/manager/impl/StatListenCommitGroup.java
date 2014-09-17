/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.statistics.manager.StatRpcMsgManager.TransactionCacheContainer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager.StatDataStoreOperation;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupDescStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.desc.GroupDescBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.features.GroupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.features.GroupFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatistics;
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
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
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
public class StatListenCommitGroup extends StatAbstractListenCommit<Group, OpendaylightGroupStatisticsListener>
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
            final InstanceIdentifier<Node> nodeIdent) {
        /* GroupId is a marker for a way to submit data to Operational/DS */
        manager.getRpcMsgManager()
            .getGroupStat(new NodeRef(nodeIdent), data.getGroupId());
    }

    @Override
    public void removeStat(final InstanceIdentifier<Group> keyIdent) {
        final InstanceIdentifier<FlowCapableNode> fNodeIdent = keyIdent.firstIdentifierOf(FlowCapableNode.class);
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                Optional<FlowCapableNode> fNode = Optional.absent();
                try {
                    fNode = tx.read(LogicalDatastoreType.OPERATIONAL, fNodeIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.debug("Operational/DS for Group stat fail! {}", keyIdent, e);
                }
                if (fNode.isPresent()) {
                    tx.delete(LogicalDatastoreType.OPERATIONAL, keyIdent);
                }
            }
        });
    }

    @Override
    public void onGroupDescStatsUpdated(final GroupDescStatsUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        final NodeId nodeId = notification.getId();
        if ( ! isExpectedStatistics(transId, nodeId)) {
            LOG.debug("STAT-MANAGER - GroupDescStatsUpdated: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification);
            return;
        }
        final List<GroupDescStats> groupStats = notification.getGroupDescStats() == null
                ? new ArrayList<GroupDescStats>(10) : new ArrayList<>(notification.getGroupDescStats());
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId, nodeId);
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
        final TransactionId transId = notification.getTransactionId();
        final NodeId nodeId = notification.getId();
        if ( ! isExpectedStatistics(transId, nodeId)) {
            LOG.debug("STAT-MANAGER - MeterFeaturesUpdated: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification);
            return;
        }
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId, nodeId);
        if ( ! txContainer.isPresent()) {
            return;
        }
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                final GroupFeatures stats = new GroupFeaturesBuilder(notification).build();
                final KeyedInstanceIdentifier<Node, NodeKey> nodeIdent = InstanceIdentifier
                        .create(Nodes.class).child(Node.class, new NodeKey(nodeId));
                final InstanceIdentifier<GroupFeatures> groupFeatureIdent = nodeIdent
                        .augmentation(NodeGroupFeatures.class).child(GroupFeatures.class);
                Optional<Node> node = Optional.absent();
                try {
                    node = tx.read(LogicalDatastoreType.OPERATIONAL, nodeIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.debug("Read Operational/DS for Node fail! {}", nodeIdent, e);
                }
                if (node.isPresent()) {
                    tx.put(LogicalDatastoreType.OPERATIONAL, groupFeatureIdent, stats, true);
                }
                manager.getStatCollector().collectNextStatistics();
            }
        });
    }

    @Override
    public void onGroupStatisticsUpdated(final GroupStatisticsUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        final NodeId nodeId = notification.getId();
        if ( ! isExpectedStatistics(transId, nodeId)) {
            LOG.debug("STAT-MANAGER - GroupStatisticsUpdated: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification);
            return;
        }
        final List<GroupStats> groupStats = notification.getGroupStats() == null
                ? new ArrayList<GroupStats>(10) : new ArrayList<>(notification.getGroupStats());
        Optional<Group> notifGroup = Optional.absent();
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId, nodeId);
        if (txContainer.isPresent()) {
            final Optional<? extends DataObject> inputObj = txContainer.get().getConfInput();
            if (inputObj.isPresent() && inputObj.get() instanceof Group) {
                notifGroup = Optional.<Group> of((Group)inputObj.get());
            }
            final List<? extends TransactionAware> cacheNotifs =
                    txContainer.get().getNotifications();
            for (final TransactionAware notif : cacheNotifs) {
                if (notif instanceof GroupStatisticsUpdated) {
                    groupStats.addAll(((GroupStatisticsUpdated) notif).getGroupStats());
                }
            }
        }
        final Optional<Group> group = notifGroup;
        statGroupCommit(groupStats, nodeId, group);
    }

    private void statGroupCommit(final List<GroupStats> groupStats, final NodeId nodeId,
            final Optional<Group> group) {
        final InstanceIdentifier<FlowCapableNode> fNodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId)).augmentation(FlowCapableNode.class);

        for (final GroupStats groupStat : groupStats) {
            final GroupStatistics stats = new GroupStatisticsBuilder(groupStat).build();

            final GroupKey groupKey = new GroupKey(groupStat.getGroupId());
            final InstanceIdentifier<GroupStatistics> gsIdent = fNodeIdent
                    .child(Group.class,groupKey).augmentation(NodeGroupStatistics.class)
                    .child(GroupStatistics.class);
            /* Statistics Writing */
            manager.enqueue(new StatDataStoreOperation() {
                @Override
                public void applyOperation(final ReadWriteTransaction trans) {
                    Optional<FlowCapableNode> fNode = Optional.absent();
                    try {
                        fNode = trans.read(LogicalDatastoreType.OPERATIONAL, fNodeIdent).checkedGet();
                    }
                    catch (final ReadFailedException e) {
                        LOG.debug("Read Operational/DS for FlowCapableNode fail! {}", fNodeIdent, e);
                    }
                    if (fNode.isPresent()) {
                        trans.put(LogicalDatastoreType.OPERATIONAL, gsIdent, stats, true);
                    }
                }
            });
            if ( ! group.isPresent()) {
                /* Notification for next Statistics */
                manager.enqueue(new StatDataStoreOperation() {
                    @Override
                    public void applyOperation(final ReadWriteTransaction tx) {
                        manager.getStatCollector().collectNextStatistics();
                    }
                });
            }
        }
    }

    private void statGroupDescCommit(final List<GroupDescStats> groupStats, final NodeId nodeId) {
        final InstanceIdentifier<FlowCapableNode> fNodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId)).augmentation(FlowCapableNode.class);

        final List<GroupKey> deviceGroupKeys = new ArrayList<>();

        for (final GroupDescStats group : groupStats) {
            if (group.getGroupId() != null) {
                final GroupBuilder groupBuilder = new GroupBuilder(group);
                final GroupKey groupKey = new GroupKey(group.getGroupId());
                final InstanceIdentifier<Group> groupRef = fNodeIdent.child(Group.class,groupKey);

                final NodeGroupDescStatsBuilder groupDesc= new NodeGroupDescStatsBuilder();
                groupDesc.setGroupDesc(new GroupDescBuilder(group).build());
                //Update augmented data
                groupBuilder.addAugmentation(NodeGroupDescStats.class, groupDesc.build());
                deviceGroupKeys.add(groupKey);
                manager.enqueue(new StatDataStoreOperation() {
                    @Override
                    public void applyOperation(final ReadWriteTransaction trans) {
                        Optional<FlowCapableNode> hashIdUpd = Optional.absent();
                        try {
                            hashIdUpd = trans.read(LogicalDatastoreType.OPERATIONAL,fNodeIdent).checkedGet();
                        }
                        catch (final ReadFailedException e) {
                            LOG.debug("Read Operational/DS for FlowCapableNode fail! {}", fNodeIdent, e);
                        }
                        if (hashIdUpd.isPresent()) {
                            trans.put(LogicalDatastoreType.OPERATIONAL, groupRef, groupBuilder.build());
                        }
                    }
                });
            }
        }
        /* Delete all not presented + send notification */
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction trans) {
                /* Notification for continue collecting statistics */
                manager.getStatCollector().collectNextStatistics();

                Optional<FlowCapableNode> fNode;
                try {
                    fNode = trans.read(LogicalDatastoreType.OPERATIONAL, fNodeIdent).checkedGet();
                }
                catch (final ReadFailedException e1) {
                    LOG.debug("Read Operational/DS for FlowCapableNode fail! {}", fNodeIdent);
                    return;
                }
                if ( ! fNode.isPresent()) {
                    LOG.trace("Read Operational/DS for FlowCapableNode fail! Node {} doesn't exist.", fNodeIdent);
                    return;
                }
                final List<Group> existGroups = fNode.get().getGroup().isEmpty()
                        ? Collections.<Group> emptyList() : fNode.get().getGroup();
                /* Add all existed groups paths - no updated paths has to be removed */
                for (final Group group : existGroups) {
                    if ( ! deviceGroupKeys.contains(group.getKey())) {
                        final InstanceIdentifier<Group> delGroupIdent = fNodeIdent.child(Group.class, group.getKey());
                        Optional<Group> delGroup = Optional.absent();
                        try {
                            delGroup = trans.read(LogicalDatastoreType.OPERATIONAL, delGroupIdent).checkedGet();
                        }
                        catch (final ReadFailedException e) {
                            // NOOP - probably another transaction delete that node
                        }
                        if (delGroup.isPresent()) {
                            trans.delete(LogicalDatastoreType.OPERATIONAL, delGroupIdent);
                        }
                    }
                }
            }
        });
    }
}

