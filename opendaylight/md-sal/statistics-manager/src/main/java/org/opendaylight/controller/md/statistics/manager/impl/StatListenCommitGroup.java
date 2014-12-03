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
import org.opendaylight.controller.md.statistics.manager.StatPermCollector.StatCapabTypes;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatisticsBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

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
    public void onGroupDescStatsUpdated(final GroupDescStatsUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        final NodeId nodeId = notification.getId();
        if ( ! isExpectedStatistics(transId, nodeId)) {
            LOG.debug("Unregistred notification detect TransactionId {}", transId);
            return;
        }
        manager.getRpcMsgManager().addNotification(notification, nodeId);
        if (notification.isMoreReplies()) {
            return;
        }

        /* Don't block RPC Notification thread */
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier
                        .create(Nodes.class).child(Node.class, new NodeKey(nodeId));
                /* Validate exist FlowCapableNode */
                final InstanceIdentifier<FlowCapableNode> fNodeIdent = nodeIdent.augmentation(FlowCapableNode.class);
                Optional<FlowCapableNode> fNode = Optional.absent();
                try {
                    fNode = tx.read(LogicalDatastoreType.OPERATIONAL,fNodeIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.debug("Read Operational/DS for FlowCapableNode fail! {}", fNodeIdent, e);
                }
                if ( ! fNode.isPresent()) {
                    return;
                }
                /* Get and Validate TransactionCacheContainer */
                final Optional<TransactionCacheContainer<?>> txContainer = getTransactionCacheContainer(transId, nodeId);
                if ( ! isTransactionCacheContainerValid(txContainer)) {
                    return;
                }
                /* Prepare List actual Groups and not updated Groups will be removed */
                final List<Group> existGroups = fNode.get().getGroup() != null
                        ? fNode.get().getGroup() : Collections.<Group> emptyList();
                final List<GroupKey> existGroupKeys = new ArrayList<>();
                for (final Group group : existGroups) {
                    existGroupKeys.add(group.getKey());
                }
                /* GroupDesc processing */
                statGroupDescCommit(txContainer, tx, fNodeIdent, existGroupKeys);
                /* Delete all not presented Group Nodes */
                deleteAllNotPresentNode(fNodeIdent, tx, Collections.unmodifiableList(existGroupKeys));
                /* Notification for continue collecting statistics */
                notifyToCollectNextStatistics(nodeIdent, transId);
            }
        });
    }

    @Override
    public void onGroupFeaturesUpdated(final GroupFeaturesUpdated notification) {
        Preconditions.checkNotNull(notification);
        final TransactionId transId = notification.getTransactionId();
        final NodeId nodeId = notification.getId();
        if ( ! isExpectedStatistics(transId, nodeId)) {
            LOG.debug("Unregistred notification detect TransactionId {}", transId);
            return;
        }
        manager.getRpcMsgManager().addNotification(notification, nodeId);
        if (notification.isMoreReplies()) {
            return;
        }

        /* Don't block RPC Notification thread */
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                /* Get and Validate TransactionCacheContainer */
                final Optional<TransactionCacheContainer<?>> txContainer = getTransactionCacheContainer(transId, nodeId);
                if ( ! isTransactionCacheContainerValid(txContainer)) {
                    return;
                }

                final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier
                        .create(Nodes.class).child(Node.class, new NodeKey(nodeId));

                final List<? extends TransactionAware> cacheNotifs = txContainer.get().getNotifications();
                for (final TransactionAware notif : cacheNotifs) {
                    if ( ! (notif instanceof GroupFeaturesUpdated)) {
                        break;
                    }
                    final GroupFeatures stats = new GroupFeaturesBuilder((GroupFeaturesUpdated)notif).build();
                    final InstanceIdentifier<NodeGroupFeatures> nodeGroupFeatureIdent =
                            nodeIdent.augmentation(NodeGroupFeatures.class);
                    final InstanceIdentifier<GroupFeatures> groupFeatureIdent = nodeGroupFeatureIdent
                            .child(GroupFeatures.class);
                    Optional<Node> node = Optional.absent();
                    try {
                        node = tx.read(LogicalDatastoreType.OPERATIONAL, nodeIdent).checkedGet();
                    }
                    catch (final ReadFailedException e) {
                        LOG.debug("Read Operational/DS for Node fail! {}", nodeIdent, e);
                    }
                    if (node.isPresent()) {
                        tx.merge(LogicalDatastoreType.OPERATIONAL, nodeGroupFeatureIdent, new NodeGroupFeaturesBuilder().build(), true);
                        tx.put(LogicalDatastoreType.OPERATIONAL, groupFeatureIdent, stats);
                        manager.registerAdditionalNodeFeature(nodeIdent, StatCapabTypes.GROUP_STATS);
                    }
                }
            }
        });
    }

    @Override
    public void onGroupStatisticsUpdated(final GroupStatisticsUpdated notification) {
        Preconditions.checkNotNull(notification);
        final TransactionId transId = notification.getTransactionId();
        final NodeId nodeId = notification.getId();
        if ( ! isExpectedStatistics(transId, nodeId)) {
            LOG.debug("STAT-MANAGER - GroupStatisticsUpdated: unregistred notification detect TransactionId {}", transId);
            return;
        }
        manager.getRpcMsgManager().addNotification(notification, nodeId);
        if (notification.isMoreReplies()) {
            return;
        }

        /* Don't block RPC Notification thread */
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {

                final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier
                        .create(Nodes.class).child(Node.class, new NodeKey(nodeId));
                /* Node exist check */
                Optional<Node> node = Optional.absent();
                try {
                    node = tx.read(LogicalDatastoreType.OPERATIONAL, nodeIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.debug("Read Operational/DS for Node fail! {}", nodeIdent, e);
                }
                if ( ! node.isPresent()) {
                    return;
                }

                /* Get and Validate TransactionCacheContainer */
                final Optional<TransactionCacheContainer<?>> txContainer = getTransactionCacheContainer(transId, nodeId);
                if ( ! isTransactionCacheContainerValid(txContainer)) {
                    return;
                }
                final List<? extends TransactionAware> cacheNotifs = txContainer.get().getNotifications();

                Optional<Group> notifGroup = Optional.absent();
                final Optional<? extends DataObject> inputObj = txContainer.get().getConfInput();
                if (inputObj.isPresent() && inputObj.get() instanceof Group) {
                    notifGroup = Optional.<Group> of((Group)inputObj.get());
                }
                for (final TransactionAware notif : cacheNotifs) {
                    if ( ! (notif instanceof GroupStatisticsUpdated)) {
                        break;
                    }
                    statGroupCommit(((GroupStatisticsUpdated) notif).getGroupStats(), nodeIdent, tx);
                }
                if ( ! notifGroup.isPresent()) {
                    notifyToCollectNextStatistics(nodeIdent, transId);
                }
            }
        });
    }

    private void statGroupCommit(final List<GroupStats> groupStats, final InstanceIdentifier<Node> nodeIdent,
            final ReadWriteTransaction tx) {

        Preconditions.checkNotNull(groupStats);
        Preconditions.checkNotNull(nodeIdent);
        Preconditions.checkNotNull(tx);

        final InstanceIdentifier<FlowCapableNode> fNodeIdent = nodeIdent.augmentation(FlowCapableNode.class);

        for (final GroupStats gStat : groupStats) {
            final GroupStatistics stats = new GroupStatisticsBuilder(gStat).build();

            final InstanceIdentifier<Group> groupIdent = fNodeIdent.child(Group.class, new GroupKey(gStat.getGroupId()));
            final InstanceIdentifier<NodeGroupStatistics> nGroupStatIdent =groupIdent
                    .augmentation(NodeGroupStatistics.class);
            final InstanceIdentifier<GroupStatistics> gsIdent = nGroupStatIdent.child(GroupStatistics.class);
            /* Statistics Writing */
            Optional<Group> group = Optional.absent();
            try {
                group = tx.read(LogicalDatastoreType.OPERATIONAL, groupIdent).checkedGet();
            }
            catch (final ReadFailedException e) {
                LOG.debug("Read Operational/DS for Group node fail! {}", groupIdent, e);
            }
            if (group.isPresent()) {
                tx.merge(LogicalDatastoreType.OPERATIONAL, nGroupStatIdent, new NodeGroupStatisticsBuilder().build(), true);
                tx.put(LogicalDatastoreType.OPERATIONAL, gsIdent, stats);
            }
        }
    }

    private void statGroupDescCommit(final Optional<TransactionCacheContainer<?>> txContainer, final ReadWriteTransaction tx,
            final InstanceIdentifier<FlowCapableNode> fNodeIdent, final List<GroupKey> existGroupKeys) {

        Preconditions.checkNotNull(existGroupKeys);
        Preconditions.checkNotNull(txContainer);
        Preconditions.checkNotNull(fNodeIdent);
        Preconditions.checkNotNull(tx);

        final List<? extends TransactionAware> cacheNotifs = txContainer.get().getNotifications();
        for (final TransactionAware notif : cacheNotifs) {
            if ( ! (notif instanceof GroupDescStatsUpdated)) {
                break;
            }
            final List<GroupDescStats> groupStats = ((GroupDescStatsUpdated) notif).getGroupDescStats();
            if (groupStats == null) {
                break;
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
                    existGroupKeys.remove(groupKey);
                    tx.put(LogicalDatastoreType.OPERATIONAL, groupRef, groupBuilder.build());
                }
            }
        }
    }

    private void deleteAllNotPresentNode(final InstanceIdentifier<FlowCapableNode> fNodeIdent,
            final ReadWriteTransaction trans, final List<GroupKey> deviceGroupKeys) {

        Preconditions.checkNotNull(fNodeIdent);
        Preconditions.checkNotNull(trans);

        if (deviceGroupKeys == null) {
            return;
        }

        for (final GroupKey key : deviceGroupKeys) {
            final InstanceIdentifier<Group> delGroupIdent = fNodeIdent.child(Group.class, key);
            LOG.trace("Group {} has to removed.", key);
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

