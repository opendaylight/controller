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
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterConfigStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStats;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

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
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification, nodeId);
            return;
        }
        final List<MeterConfigStats> meterConfStat = notification.getMeterConfigStats() != null
                ? new ArrayList<>(notification.getMeterConfigStats()) : new ArrayList<MeterConfigStats>(10);
        final Optional<TransactionCacheContainer<?>> txContainer = getTransactionCacheContainer(transId, nodeId);
        if (txContainer.isPresent()) {
            final List<? extends TransactionAware> cacheNotifs = txContainer.get().getNotifications();
            for (final TransactionAware notif : cacheNotifs) {
                if (notif instanceof MeterConfigStatsUpdated) {
                    meterConfStat.addAll(((MeterConfigStatsUpdated) notif).getMeterConfigStats());
                }
            }
        }
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class).child(Node.class, new NodeKey(nodeId));
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                /* Notification for continue collecting statistics */
                notifyToCollectNextStatistics(nodeIdent);
                comitConfMeterStats(meterConfStat, nodeIdent, tx);
            }
        });
    }

    @Override
    public void onMeterFeaturesUpdated(final MeterFeaturesUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        final NodeId nodeId = notification.getId();
        if ( ! isExpectedStatistics(transId, nodeId)) {
            LOG.debug("STAT-MANAGER - MeterFeaturesUpdated: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification, nodeId);
            return;
        }
        final Optional<TransactionCacheContainer<?>> txContainer = getTransactionCacheContainer(transId, nodeId);
        if ( ! txContainer.isPresent()) {
            return;
        }
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier
                .create(Nodes.class).child(Node.class, new NodeKey(nodeId));

        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                /* Notification for continue collecting statistics */
                notifyToCollectNextStatistics(nodeIdent);
                final MeterFeatures stats = new MeterFeaturesBuilder(notification).build();
                final NodeMeterFeaturesBuilder nodeMeterFeaturesBuilder = new NodeMeterFeaturesBuilder().setMeterFeatures(stats);
                final InstanceIdentifier<NodeMeterFeatures> nodeMeterFeatureIdent = nodeIdent
                        .augmentation(NodeMeterFeatures.class);
                Optional<Node> node = Optional.absent();
                try {
                    node = tx.read(LogicalDatastoreType.OPERATIONAL, nodeIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.debug("Read Operational/DS for Node fail! {}", nodeIdent, e);
                }
                if (node.isPresent()) {
                    tx.put(LogicalDatastoreType.OPERATIONAL, nodeMeterFeatureIdent, nodeMeterFeaturesBuilder.build());
                }
            }
        });
    }

    @Override
    public void onMeterStatisticsUpdated(final MeterStatisticsUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        final NodeId nodeId = notification.getId();
        if ( ! isExpectedStatistics(transId, nodeId)) {
            LOG.debug("STAT-MANAGER - MeterStatisticsUpdated: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification, nodeId);
            return;
        }
        final List<MeterStats> meterStat = notification.getMeterStats() != null
                ? new ArrayList<>(notification.getMeterStats()) : new ArrayList<MeterStats>(10);
        final Optional<TransactionCacheContainer<?>> txContainer = getTransactionCacheContainer(transId, nodeId);
        if (txContainer.isPresent()) {
            final List<? extends TransactionAware> cacheNotifs = txContainer.get().getNotifications();
            for (final TransactionAware notif : cacheNotifs) {
                if (notif instanceof MeterConfigStatsUpdated) {
                    meterStat.addAll(((MeterStatisticsUpdated) notif).getMeterStats());
                }
            }
        }
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class).child(Node.class, new NodeKey(nodeId));
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                statMeterCommit(meterStat, nodeIdent, tx);
                /* Notification for continue collecting statistics */
                notifyToCollectNextStatistics(nodeIdent);
            }
        });
    }

    private void statMeterCommit(final List<MeterStats> meterStats,
            final InstanceIdentifier<Node> nodeIdent, final ReadWriteTransaction trans) {

        final InstanceIdentifier<FlowCapableNode> fNodeIdent = nodeIdent.augmentation(FlowCapableNode.class);
        for (final MeterStats mStat : meterStats) {
            final MeterStatistics stats = new MeterStatisticsBuilder(mStat).build();

            final MeterKey mKey = new MeterKey(mStat.getMeterId());
            final InstanceIdentifier<MeterStatistics> msIdent = fNodeIdent
                    .child(Meter.class, mKey).augmentation(NodeMeterStatistics.class)
                    .child(MeterStatistics.class);
            /* Meter Statistics commit */
            Optional<FlowCapableNode> fNode = Optional.absent();
            try {
                fNode = trans.read(LogicalDatastoreType.OPERATIONAL, fNodeIdent).checkedGet();
            }
            catch (final ReadFailedException e) {
                LOG.debug("Read Operational/DS for FlowCapableNode fail! {}", fNodeIdent, e);
            }
            if (fNode.isPresent()) {
                trans.put(LogicalDatastoreType.OPERATIONAL, msIdent, stats);
            }
        }
    }

    private void comitConfMeterStats(final List<MeterConfigStats> meterConfStat,
            final InstanceIdentifier<Node> nodeIdent, final ReadWriteTransaction trans) {

        final InstanceIdentifier<FlowCapableNode> fNodeIdent = nodeIdent.augmentation(FlowCapableNode.class);
        final List<MeterKey> deviceMeterKeys = new ArrayList<>();

        for (final MeterConfigStats meterConf : meterConfStat) {
            final MeterBuilder meterBuilder = new MeterBuilder(meterConf);
            if (meterConf.getMeterId() != null) {
                final MeterKey meterKey = new MeterKey(meterConf.getMeterId());
                meterBuilder.setKey(meterKey);
                final InstanceIdentifier<Meter> meterRef = nodeIdent
                        .augmentation(FlowCapableNode.class).child(Meter.class,meterKey);
                final NodeMeterConfigStatsBuilder meterConfig = new NodeMeterConfigStatsBuilder();
                meterConfig.setMeterConfigStats(new MeterConfigStatsBuilder(meterConf).build());
                //Update augmented data
                meterBuilder.addAugmentation(NodeMeterConfigStats.class, meterConfig.build());
                deviceMeterKeys.add(meterKey);
                Optional<FlowCapableNode> fNode = Optional.absent();
                try {
                    fNode = trans.read(LogicalDatastoreType.OPERATIONAL, fNodeIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.debug("Read Operational/DS for FlowCapableNode fail! {}", fNodeIdent, e);
                }
                if (fNode.isPresent()) {
                    trans.put(LogicalDatastoreType.OPERATIONAL, meterRef, meterBuilder.build());
                }
            }
        }
        /* Delete all not presented Meter Nodes */
        deleteAllNotPresentedNodes(fNodeIdent, trans, deviceMeterKeys);
    }

    private void deleteAllNotPresentedNodes(final InstanceIdentifier<FlowCapableNode> fNodeIdent,
            final ReadWriteTransaction trans, final List<MeterKey> deviceMeterKeys) {
        /* Delete all not presented meters */
        final Optional<FlowCapableNode> fNode = readLatestConfiguration(fNodeIdent);

        if ( ! fNode.isPresent()) {
            LOG.trace("Read Operational/DS for FlowCapableNode fail! Node {} doesn't exist.", fNodeIdent);
            return;
        }
        final List<Meter> existMeters = fNode.get().getMeter() != null
                ? fNode.get().getMeter() : Collections.<Meter> emptyList();
        /* Add all existed groups paths - no updated paths has to be removed */
        for (final Meter meter : existMeters) {
            if (deviceMeterKeys.remove(meter.getKey())) {
                break; // Meter still exist on device
            }
            final InstanceIdentifier<Meter> delMeterIdent = fNodeIdent.child(Meter.class, meter.getKey());
            Optional<Meter> delMeter = Optional.absent();
            try {
                delMeter = trans.read(LogicalDatastoreType.OPERATIONAL, delMeterIdent).checkedGet();
            }
            catch (final ReadFailedException e) {
                // NOOP - probably another transaction delete that node
            }
            if (delMeter.isPresent()) {
                trans.delete(LogicalDatastoreType.OPERATIONAL, delMeterIdent);
            }
        }
    }
}

