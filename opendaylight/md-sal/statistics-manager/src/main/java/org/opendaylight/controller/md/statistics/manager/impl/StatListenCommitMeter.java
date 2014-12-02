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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterConfigStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterConfigStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStats;
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
 * StatListenCommitMeter
 * Class is a NotifyListener for MeterStatistics and DataChangeListener for Config/DataStore for Meter node.
 * All expected (registered) MeterStatistics will be builded and commit to Operational/DataStore.
 * DataChangeEven should call create/delete Meter in Operational/DS
 *
 */
public class StatListenCommitMeter extends StatAbstractListenCommit<Meter, OpendaylightMeterStatisticsListener>
                                            implements OpendaylightMeterStatisticsListener {

    private static final Logger LOG = LoggerFactory.getLogger(StatListenCommitMeter.class);

    public StatListenCommitMeter(final StatisticsManager manager, final DataBroker db,
            final NotificationProviderService nps) {
        super(manager, db, nps, Meter.class);
    }

    @Override
    protected InstanceIdentifier<Meter> getWildCardedRegistrationPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class)
                .augmentation(FlowCapableNode.class).child(Meter.class);
    }

    @Override
    protected OpendaylightMeterStatisticsListener getStatNotificationListener() {
        return this;
    }

    @Override
    public void onMeterConfigStatsUpdated(final MeterConfigStatsUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        final NodeId nodeId = notification.getId();
        if ( ! isExpectedStatistics(transId, nodeId)) {
            LOG.debug("STAT-MANAGER - MeterConfigStatsUpdated: unregistred notification detect TransactionId {}", transId);
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
                /* Prepare List actual Meters and not updated Meters will be removed */
                final List<Meter> existMeters = fNode.get().getMeter() != null
                        ? fNode.get().getMeter() : Collections.<Meter> emptyList();
                final List<MeterKey> existMeterKeys = new ArrayList<>();
                for (final Meter meter : existMeters) {
                    existMeterKeys.add(meter.getKey());
                }
                /* MeterConfig processing */
                comitConfMeterStats(txContainer, tx, fNodeIdent, existMeterKeys);
                /* Delete all not presented Meter Nodes */
                deleteAllNotPresentedNodes(fNodeIdent, tx, Collections.unmodifiableList(existMeterKeys));
                /* Notification for continue collecting statistics */
                notifyToCollectNextStatistics(nodeIdent, transId);
            }
        });
    }

    @Override
    public void onMeterFeaturesUpdated(final MeterFeaturesUpdated notification) {
        Preconditions.checkNotNull(notification);
        final TransactionId transId = notification.getTransactionId();
        final NodeId nodeId = notification.getId();
        if ( ! isExpectedStatistics(transId, nodeId)) {
            LOG.debug("STAT-MANAGER - MeterFeaturesUpdated: unregistred notification detect TransactionId {}", transId);
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
                    if ( ! (notif instanceof MeterFeaturesUpdated)) {
                        break;
                    }
                    final MeterFeatures stats = new MeterFeaturesBuilder((MeterFeaturesUpdated)notif).build();
                    final InstanceIdentifier<NodeMeterFeatures> nodeMeterFeatureIdent =
                            nodeIdent.augmentation(NodeMeterFeatures.class);
                    final InstanceIdentifier<MeterFeatures> meterFeatureIdent = nodeMeterFeatureIdent
                            .child(MeterFeatures.class);
                    Optional<Node> node = Optional.absent();
                    try {
                        node = tx.read(LogicalDatastoreType.OPERATIONAL, nodeIdent).checkedGet();
                    }
                    catch (final ReadFailedException e) {
                        LOG.debug("Read Operational/DS for Node fail! {}", nodeIdent, e);
                    }
                    if (node.isPresent()) {
                        tx.merge(LogicalDatastoreType.OPERATIONAL, nodeMeterFeatureIdent, new NodeMeterFeaturesBuilder().build(), true);
                        tx.put(LogicalDatastoreType.OPERATIONAL, meterFeatureIdent, stats);
                        manager.registerAdditionalNodeFeature(nodeIdent, StatCapabTypes.METER_STATS);
                    }
                }
            }
        });
    }

    @Override
    public void onMeterStatisticsUpdated(final MeterStatisticsUpdated notification) {
        Preconditions.checkNotNull(notification);
        final TransactionId transId = notification.getTransactionId();
        final NodeId nodeId = notification.getId();
        if ( ! isExpectedStatistics(transId, nodeId)) {
            LOG.debug("STAT-MANAGER - MeterStatisticsUpdated: unregistred notification detect TransactionId {}", transId);
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

                Optional<Meter> notifMeter = Optional.absent();
                final Optional<? extends DataObject> inputObj = txContainer.get().getConfInput();
                if (inputObj.isPresent() && inputObj.get() instanceof Meter) {
                    notifMeter = Optional.<Meter> of((Meter)inputObj.get());
                }
                for (final TransactionAware notif : cacheNotifs) {
                    if ( ! (notif instanceof MeterStatisticsUpdated)) {
                        break;
                    }
                    statMeterCommit(((MeterStatisticsUpdated) notif).getMeterStats(), nodeIdent, tx);
                }
                if (notifMeter.isPresent()) {
                    notifyToCollectNextStatistics(nodeIdent, transId);
                }
            }
        });
    }

    private void statMeterCommit(final List<MeterStats> meterStats,
            final InstanceIdentifier<Node> nodeIdent, final ReadWriteTransaction tx) {

        Preconditions.checkNotNull(meterStats);
        Preconditions.checkNotNull(nodeIdent);
        Preconditions.checkNotNull(tx);

        final InstanceIdentifier<FlowCapableNode> fNodeIdent = nodeIdent.augmentation(FlowCapableNode.class);

        for (final MeterStats mStat : meterStats) {
            final MeterStatistics stats = new MeterStatisticsBuilder(mStat).build();

            final InstanceIdentifier<Meter> meterIdent = fNodeIdent.child(Meter.class, new MeterKey(mStat.getMeterId()));
            final InstanceIdentifier<NodeMeterStatistics> nodeMeterStatIdent = meterIdent
                    .augmentation(NodeMeterStatistics.class);
            final InstanceIdentifier<MeterStatistics> msIdent = nodeMeterStatIdent.child(MeterStatistics.class);
            /* Meter Statistics commit */
            Optional<Meter> meter = Optional.absent();
            try {
                meter = tx.read(LogicalDatastoreType.OPERATIONAL, meterIdent).checkedGet();
            }
            catch (final ReadFailedException e) {
                LOG.debug("Read Operational/DS for FlowCapableNode fail! {}", fNodeIdent, e);
            }
            if (meter.isPresent()) {
                tx.merge(LogicalDatastoreType.OPERATIONAL, nodeMeterStatIdent, new NodeMeterStatisticsBuilder().build(), true);
                tx.put(LogicalDatastoreType.OPERATIONAL, msIdent, stats);
            }
        }
    }

    private void comitConfMeterStats(final Optional<TransactionCacheContainer<?>> txContainer, final ReadWriteTransaction tx,
            final InstanceIdentifier<FlowCapableNode> fNodeIdent, final List<MeterKey> existMeterKeys) {

        Preconditions.checkNotNull(existMeterKeys);
        Preconditions.checkNotNull(txContainer);
        Preconditions.checkNotNull(fNodeIdent);
        Preconditions.checkNotNull(tx);

        final List<? extends TransactionAware> cacheNotifs = txContainer.get().getNotifications();
        for (final TransactionAware notif : cacheNotifs) {
            if ( ! (notif instanceof MeterConfigStatsUpdated)) {
                break;
            }
            final List<MeterConfigStats> meterStats = ((MeterConfigStatsUpdated) notif).getMeterConfigStats();
            if (meterStats == null) {
                break;
            }
            for (final MeterConfigStats meterStat : meterStats) {
                if (meterStat.getMeterId() != null) {
                    final MeterBuilder meterBuilder = new MeterBuilder(meterStat);
                    final MeterKey meterKey = new MeterKey(meterStat.getMeterId());
                    final InstanceIdentifier<Meter> meterRef = fNodeIdent.child(Meter.class, meterKey);

                    final NodeMeterConfigStatsBuilder meterConfig = new NodeMeterConfigStatsBuilder();
                    meterConfig.setMeterConfigStats(new MeterConfigStatsBuilder(meterStat).build());
                    //Update augmented data
                    meterBuilder.addAugmentation(NodeMeterConfigStats.class, meterConfig.build());
                    existMeterKeys.remove(meterKey);
                    tx.put(LogicalDatastoreType.OPERATIONAL, meterRef, meterBuilder.build());
                }
            }
        }
    }

    private void deleteAllNotPresentedNodes(final InstanceIdentifier<FlowCapableNode> fNodeIdent,
            final ReadWriteTransaction tx, final List<MeterKey> deviceMeterKeys) {

        Preconditions.checkNotNull(fNodeIdent);
        Preconditions.checkNotNull(tx);

        if (deviceMeterKeys == null) {
            return;
        }

        for (final MeterKey key : deviceMeterKeys) {
            final InstanceIdentifier<Meter> delMeterIdent = fNodeIdent.child(Meter.class, key);
            LOG.trace("Meter {} has to removed.", key);
            Optional<Meter> delMeter = Optional.absent();
            try {
                delMeter = tx.read(LogicalDatastoreType.OPERATIONAL, delMeterIdent).checkedGet();
            }
            catch (final ReadFailedException e) {
                // NOOP - probably another transaction delete that node
            }
            if (delMeter.isPresent()) {
                tx.delete(LogicalDatastoreType.OPERATIONAL, delMeterIdent);
            }
        }
    }
}

