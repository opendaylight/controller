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
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

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
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                final CheckedFuture<Optional<Group>, ReadFailedException> future = tx
                        .read(LogicalDatastoreType.OPERATIONAL, keyIdent);
                Futures.addCallback(future, new FutureCallback<Optional<Group>>() {
                    @Override
                    public void onSuccess(final Optional<Group> result) {
                        if (result.isPresent()) {
                            tx.delete(LogicalDatastoreType.OPERATIONAL, keyIdent);
                        }
                    }
                    @Override
                    public void onFailure(final Throwable t) {
                        //NOOP
                    }
                });
            }
        });
    }

    @Override
    public void onGroupDescStatsUpdated(final GroupDescStatsUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! isExpectedStatistics(transId)) {
            LOG.debug("STAT-MANAGER - GroupDescStatsUpdated: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification);
        }
        final NodeId nodeId = notification.getId();
        final List<GroupDescStats> groupStats = notification.getGroupDescStats() == null
                ? new ArrayList<GroupDescStats>(10) : new ArrayList<>(notification.getGroupDescStats());
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId);
        if (txContainer.isPresent()) {
            final List<? extends TransactionAware> cacheNotifs =
                    txContainer.get().getNotifications();
            for (final TransactionAware notif : cacheNotifs) {
                if (notif instanceof GroupDescStatsUpdated) {
                    groupStats.addAll(((GroupDescStatsUpdated) notif).getGroupDescStats());
                }
            }
        }
        manager.enqueue(new StatDataStoreOperation() {

            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                statGroupDescCommit(groupStats, nodeId, tx);
            }
        });
    }

    @Override
    public void onGroupFeaturesUpdated(final GroupFeaturesUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! isExpectedStatistics(transId)) {
            LOG.debug("STAT-MANAGER - MeterFeaturesUpdated: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId);
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
                final CheckedFuture<Optional<Node>, ReadFailedException> future = tx
                        .read(LogicalDatastoreType.OPERATIONAL, nodeIdent);
                Futures.addCallback(future, new FutureCallback<Optional<Node>>() {
                    @Override
                    public void onSuccess(final Optional<Node> result) {
                        if (result.isPresent()) {
                            tx.put(LogicalDatastoreType.OPERATIONAL, groupFeatureIdent, stats, true);
                        }
                    }
                    @Override
                    public void onFailure(final Throwable t) {
                        //NOOP
                    }
                });
                manager.getStatCollector().collectNextStatistics();
            }
        });
    }

    @Override
    public void onGroupStatisticsUpdated(final GroupStatisticsUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! isExpectedStatistics(transId)) {
            LOG.debug("STAT-MANAGER - GroupStatisticsUpdated: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<GroupStats> groupStats = notification.getGroupStats() == null
                ? new ArrayList<GroupStats>(10) : new ArrayList<>(notification.getGroupStats());
        Optional<Group> notifGroup = Optional.absent();
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId);
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

        manager.enqueue(new StatDataStoreOperation() {

            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                statGroupCommit(groupStats, nodeId, group, tx);
            }
        });
    }

    private void statGroupCommit(final List<GroupStats> groupStats, final NodeId nodeId,
            final Optional<Group> group, final ReadWriteTransaction trans) {
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId));

        for (final GroupStats groupStat : groupStats) {
            final GroupStatistics stats = new GroupStatisticsBuilder(groupStat).build();

            final GroupKey groupKey = new GroupKey(groupStat.getGroupId());
            final InstanceIdentifier<GroupStatistics> gsIdent = nodeIdent
                    .augmentation(FlowCapableNode.class).child(Group.class,groupKey)
                    .augmentation(NodeGroupStatistics.class).child(GroupStatistics.class);

            final CheckedFuture<Optional<FlowCapableNode>, ReadFailedException> hashIdUpd = trans
                    .read(LogicalDatastoreType.OPERATIONAL, nodeIdent.augmentation(FlowCapableNode.class));
            Futures.addCallback(hashIdUpd, new FutureCallback<Optional<FlowCapableNode>>() {
                @Override
                public void onSuccess(final Optional<FlowCapableNode> result) {
                    if (result.isPresent()) {
                        trans.put(LogicalDatastoreType.OPERATIONAL, gsIdent, stats);
                    }
                }
                @Override
                public void onFailure(final Throwable t) {
                    //NOOP
                }
            });
        }
        if ( ! group.isPresent()) {
            manager.getStatCollector().collectNextStatistics();
        }
    }

    private void statGroupDescCommit(final List<GroupDescStats> groupStats, final NodeId nodeId,
            final ReadWriteTransaction trans) {
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId));
        final InstanceIdentifier<FlowCapableNode> fNodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId)).augmentation(FlowCapableNode.class);

        final CheckedFuture<Optional<FlowCapableNode>, ReadFailedException> future =
                trans.read(LogicalDatastoreType.OPERATIONAL, fNodeIdent);
        Futures.addCallback(future, new FutureCallback<Optional<? extends DataObject>>() {

            @Override
            public void onFailure(final Throwable t) {
                LOG.debug("Read Operational/DS for FlowCapableNode {} fail!", fNodeIdent);
            }

            @Override
            public void onSuccess(final Optional<? extends DataObject> result) {
                if ( ! result.isPresent()) {
                    LOG.debug("Read Operational/DS for FlowCapableNode {} fail!", fNodeIdent);
                    return;
                }
                if (! (result.get() instanceof FlowCapableNode)) {
                    return;
                }
                final FlowCapableNode flowNode = (FlowCapableNode) result.get();
                final List<InstanceIdentifier<Group>> nodeExistedGroupPaths = new ArrayList<>();
                final List<Group> existGroups = flowNode.getGroup() != null
                        ? flowNode.getGroup() : Collections.<Group> emptyList();
                /* Add all existed groups paths - no updated paths has to be removed */
                for (final Group group : existGroups) {
                    nodeExistedGroupPaths.add(fNodeIdent.child(Group.class, group.getKey()));
                }

                for (final GroupDescStats group : groupStats) {
                    if (group.getGroupId() != null) {
                        final GroupBuilder groupBuilder = new GroupBuilder(group);
                        final GroupKey groupKey = new GroupKey(group.getGroupId());
                        final InstanceIdentifier<Group> groupRef = fNodeIdent.child(Group.class,groupKey);

                        final NodeGroupDescStatsBuilder groupDesc= new NodeGroupDescStatsBuilder();
                        groupDesc.setGroupDesc(new GroupDescBuilder(group).build());
                        //Update augmented data
                        groupBuilder.addAugmentation(NodeGroupDescStats.class, groupDesc.build());
                        if (nodeExistedGroupPaths.contains(groupRef)) {
                            nodeExistedGroupPaths.remove(groupRef);
                        }
                        final CheckedFuture<Optional<FlowCapableNode>, ReadFailedException> hashIdUpd = trans
                                .read(LogicalDatastoreType.OPERATIONAL, nodeIdent.augmentation(FlowCapableNode.class));
                        Futures.addCallback(hashIdUpd, new FutureCallback<Optional<FlowCapableNode>>() {
                            @Override
                            public void onSuccess(final Optional<FlowCapableNode> result) {
                                if (result.isPresent()) {
                                    trans.put(LogicalDatastoreType.OPERATIONAL, groupRef, groupBuilder.build());
                                }
                            }
                            @Override
                            public void onFailure(final Throwable t) {
                                //NOOP
                            }
                        });
                    }
                }
                cleaningOperationalDS(true, trans, nodeExistedGroupPaths, nodeIdent);
            }
        });
    }
}

