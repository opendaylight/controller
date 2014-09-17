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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterConfigStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
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
    public void createStat(final InstanceIdentifier<Meter> keyIdent, final Meter data,
            final InstanceIdentifier<Node> nodeIdent) {
        manager.getRpcMsgManager().getMeterStat(new NodeRef(nodeIdent), data.getMeterId());
    }

    @Override
    public void removeStat(final InstanceIdentifier<Meter> keyIdent) {
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                Optional<Meter> delMeter = Optional.absent();
                try {
                    delMeter = tx.read(LogicalDatastoreType.OPERATIONAL, keyIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.debug("Operational/DS for Meter stat fail! {}", keyIdent, e);
                }
                if (delMeter.isPresent()) {
                    tx.delete(LogicalDatastoreType.OPERATIONAL, keyIdent);
                }
            }
        });
    }

    @Override
    public void onMeterConfigStatsUpdated(final MeterConfigStatsUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! isExpectedStatistics(transId)) {
            LOG.debug("STAT-MANAGER - MeterConfigStatsUpdated: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<MeterConfigStats> meterConfStat = notification.getMeterConfigStats() == null
                ? new ArrayList<MeterConfigStats>() : new ArrayList<>(notification.getMeterConfigStats());
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId);
        if (txContainer.isPresent()) {
            final List<? extends TransactionAware> cacheNotifs = txContainer.get().getNotifications();
            for (final TransactionAware notif : cacheNotifs) {
                if (notif instanceof MeterConfigStatsUpdated) {
                    meterConfStat.addAll(((MeterConfigStatsUpdated) notif).getMeterConfigStats());
                }
            }
        }
        comitConfMeterStats(meterConfStat, nodeId);
    }

    @Override
    public void onMeterFeaturesUpdated(final MeterFeaturesUpdated notification) {
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
        final MeterFeatures stats = new MeterFeaturesBuilder(notification).build();
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier
                .create(Nodes.class).child(Node.class, new NodeKey(nodeId));
        final InstanceIdentifier<MeterFeatures> meterFeatureIdent = nodeIdent
                .augmentation(NodeMeterFeatures.class).child(MeterFeatures.class);
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                Optional<Node> node = Optional.absent();
                try {
                    node = tx.read(LogicalDatastoreType.OPERATIONAL, nodeIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.debug("Read Operational/DS for Node fail! {}", nodeIdent, e);
                }
                if (node.isPresent()) {
                    tx.put(LogicalDatastoreType.OPERATIONAL, meterFeatureIdent, stats, true);
                }
                manager.getStatCollector().collectNextStatistics();
            }
        });
    }

    @Override
    public void onMeterStatisticsUpdated(final MeterStatisticsUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! isExpectedStatistics(transId)) {
            LOG.debug("STAT-MANAGER - MeterStatisticsUpdated: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<MeterStats> meterStat = notification.getMeterStats() == null
                ? new ArrayList<MeterStats>() : new ArrayList<>(notification.getMeterStats());
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId);
        Optional<Meter> notifMenter = Optional.absent();
        if (txContainer.isPresent()) {
            final Optional<? extends DataObject> inputObj = txContainer.get().getConfInput();
            if (inputObj.isPresent() && inputObj.get() instanceof Meter) {
                notifMenter = Optional.<Meter> of((Meter)inputObj.get());
            }
            final List<? extends TransactionAware> cacheNotifs = txContainer.get().getNotifications();
            for (final TransactionAware notif : cacheNotifs) {
                if (notif instanceof MeterConfigStatsUpdated) {
                    meterStat.addAll(((MeterStatisticsUpdated) notif).getMeterStats());
                }
            }
        }
        final Optional<Meter> meter = notifMenter;
        statMeterCommit(meterStat, nodeId, meter);
    }

    private void statMeterCommit(final List<MeterStats> meterStats,
            final NodeId nodeId, final Optional<Meter> meter) {
        final InstanceIdentifier<FlowCapableNode> fNodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId)).augmentation(FlowCapableNode.class);
        for (final MeterStats mStat : meterStats) {
            final MeterStatistics stats = new MeterStatisticsBuilder(mStat).build();

            final MeterKey mKey = new MeterKey(mStat.getMeterId());
            final InstanceIdentifier<MeterStatistics> msIdent = fNodeIdent
                    .child(Meter.class, mKey).augmentation(NodeMeterStatistics.class)
                    .child(MeterStatistics.class);
            /* Meter Statistics commit */
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
                        trans.put(LogicalDatastoreType.OPERATIONAL, msIdent, stats, true);
                    }
                }
            });
        }
        if ( ! meter.isPresent()) {
            /* Notification */
            manager.enqueue(new StatDataStoreOperation() {
                @Override
                public void applyOperation(final ReadWriteTransaction tx) {
                    manager.getStatCollector().collectNextStatistics();
                }
            });
        }
    }

    private void comitConfMeterStats(final List<MeterConfigStats> meterConfStat, final NodeId nodeId) {
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId));
        final InstanceIdentifier<FlowCapableNode> fNodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId)).augmentation(FlowCapableNode.class);

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
                            trans.put(LogicalDatastoreType.OPERATIONAL, meterRef, meterBuilder.build());
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

                Optional<FlowCapableNode> fNode = Optional.absent();
                try {
                    fNode = trans.read(LogicalDatastoreType.OPERATIONAL, fNodeIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.debug("Read Operational/DS for FlowCapableNode fail! {}", fNodeIdent, e);
                    return;
                }
                if ( ! fNode.isPresent()) {
                    LOG.trace("Read Operational/DS for FlowCapableNode fail! Node {} doesn't exist.", fNodeIdent);
                    return;
                }
                final List<Meter> existMeters = fNode.get().getMeter().isEmpty()
                        ? Collections.<Meter> emptyList() : fNode.get().getMeter();
                /* Add all existed groups paths - no updated paths has to be removed */
                for (final Meter meter : existMeters) {
                    if ( ! deviceMeterKeys.contains(meter.getKey())) {
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
        });
    }
}

