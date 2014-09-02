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
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.statistics.manager.StatDeviceMsgManager.TransactionCacheContainer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterConfigStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStats;
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
 * StatListenCommitMeter
 * Class is a NotifyListener for MeterStatistics and DataChangeListener for Config/DataStore for Meter node.
 * All expected (registered) MeterStatistics will be builded and commit to Operational/DataStore.
 * DataChangeEven should call create/delete Meter in Operational/DS
 *
 */
class StatListenCommitMeter extends StatAbstractListenCommit<Meter, OpendaylightMeterStatisticsListener>
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
            final InstanceIdentifier<FlowCapableNode> nodeIdent) {
        if (manager.getRepeatedlyEnforcer().isProvidedIdentLocked(keyIdent)) {
            return; // statistics will come soon
        }
        manager.getDeviceMsgManager().getMeterStat(new NodeRef(nodeIdent), data.getMeterId());
    }

    @Override
    public void removeStat(final InstanceIdentifier<Meter> keyIdent) {
        if ( ! manager.getRepeatedlyEnforcer().isProvidedIdentLocked(keyIdent)) {
            final WriteTransaction trans = manager.getWriteTransaction();
            trans.delete(LogicalDatastoreType.OPERATIONAL, keyIdent);
            trans.submit();
        } // OTHERWISE we expect to remove stat by StatRepeatedlyEnforcer
    }

    @Override
    public void onMeterConfigStatsUpdated(final MeterConfigStatsUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! manager.getDeviceMsgManager().isExpectedStatistics(transId)) {
//            return;
            LOG.warn("STAT-MANAGER - MeterConfigStatsUpdated: unregistred notification detect TransactionId {}", transId);
        }
        if (notification.isMoreReplies()) {
            manager.getDeviceMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<MeterConfigStats> meterConfStat = notification.getMeterConfigStats() == null
                ? new ArrayList<MeterConfigStats>() : new ArrayList<>(notification.getMeterConfigStats());
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getDeviceMsgManager().getTransactionCacheContainer(transId);
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
        // NOOP - JOB for Inventory Manager
        final TransactionId transId = notification.getTransactionId();
        if ( ! manager.getDeviceMsgManager().isExpectedStatistics(transId)) {
            LOG.trace("STAT-MANAGER - MeterFeaturesUpdated: unregistred notification detect TransactionId {}", transId);
        } else {
            LOG.error("STAT-MANAGER - MeterFeaturesUpdated: unimplement registred notification detect TransactionId {}", transId);
        }
    }

    @Override
    public void onMeterStatisticsUpdated(final MeterStatisticsUpdated notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! manager.getDeviceMsgManager().isExpectedStatistics(transId)) {
//            return;
            LOG.warn("STAT-MANAGER - MeterStatisticsUpdated: unregistred notification detect TransactionId {}", transId);
        }
        if (notification.isMoreReplies()) {
            manager.getDeviceMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<MeterStats> meterStat = notification.getMeterStats() == null
                ? new ArrayList<MeterStats>() : new ArrayList<>(notification.getMeterStats());
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getDeviceMsgManager().getTransactionCacheContainer(transId);
        Optional<Meter> meter = Optional.absent();
        if (txContainer.isPresent()) {
            final Optional<DataObject> inputObj = txContainer.get().getConfInput();
            if (inputObj.isPresent() && inputObj.get() instanceof Meter) {
                meter = Optional.<Meter> of((Meter)inputObj.get());
            }
            final List<? extends TransactionAware> cacheNotifs = txContainer.get().getNotifications();
            for (final TransactionAware notif : cacheNotifs) {
                if (notif instanceof MeterConfigStatsUpdated) {
                    meterStat.addAll(((MeterStatisticsUpdated) notif).getMeterStats());
                }
            }
        }
        statMeterCommit(meterStat, nodeId, meter);
    }

    private void statMeterCommit(final List<MeterStats> meterStats,
            final NodeId nodeId, final Optional<Meter> meter) {
        final InstanceIdentifier<FlowCapableNode> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId)).augmentation(FlowCapableNode.class);
        final InstanceIdentifier<Meter> wildCardedMeterPathForDelete = nodeIdent.child(Meter.class);
        final WriteTransaction trans = manager.getWriteTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, wildCardedMeterPathForDelete);
        for (final MeterStats mStat : meterStats) {
            final MeterBuilder mBuilder = new MeterBuilder();
            final MeterKey mKey = new MeterKey(mStat.getMeterId());
            final NodeMeterStatisticsBuilder msBuilder = new NodeMeterStatisticsBuilder();
            final KeyedInstanceIdentifier<Meter, MeterKey> mIdent = nodeIdent.child(Meter.class, mKey);

            msBuilder.setMeterStatistics(new MeterStatisticsBuilder(mStat).build());
            mBuilder.addAugmentation(NodeMeterStatistics.class, msBuilder.build());
            trans.put(LogicalDatastoreType.OPERATIONAL, mIdent, mBuilder.build(), true);
        }
        if (meter.isPresent()) {
            trans.submit();
        } else {
            continueStatCollecting(trans.submit());
        }
    }

    private void comitConfMeterStats(final List<MeterConfigStats> meterConfStat, final NodeId nodeId) {
        final InstanceIdentifier<FlowCapableNode> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId)).augmentation(FlowCapableNode.class);
        final WriteTransaction trans = manager.getWriteTransaction();

        final List<InstanceIdentifier<Meter>> actualMeters = new ArrayList<>();

        for (final MeterConfigStats meterConf : meterConfStat) {
            final MeterBuilder meterBuilder = new MeterBuilder(meterConf);
            final MeterKey meterKey = new MeterKey(meterConf.getMeterId());
            meterBuilder.setKey(meterKey);
            final InstanceIdentifier<Meter> meterRef = nodeIdent.child(Meter.class,meterKey);
            final NodeMeterConfigStatsBuilder meterConfig = new NodeMeterConfigStatsBuilder();
            meterConfig.setMeterConfigStats(new MeterConfigStatsBuilder(meterConf).build());
            //Update augmented data
            meterBuilder.addAugmentation(NodeMeterConfigStats.class, meterConfig.build());
            trans.merge(LogicalDatastoreType.OPERATIONAL, meterRef, meterBuilder.build(), true);
        }
        cleaningOperationalDS(actualMeters, trans);
    }
}

