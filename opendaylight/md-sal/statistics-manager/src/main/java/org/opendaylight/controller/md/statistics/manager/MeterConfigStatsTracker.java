/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterConfigStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStats;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MeterConfigStatsTracker extends AbstractListeningStatsTracker<MeterConfigStats, MeterConfigStats> {
    private static final Logger LOG = LoggerFactory.getLogger(MeterConfigStatsTracker.class);
    private final OpendaylightMeterStatisticsService meterStatsService;

    protected MeterConfigStatsTracker(OpendaylightMeterStatisticsService meterStatsService, final FlowCapableContext context) {
        super(context);
        this.meterStatsService = meterStatsService;
    }

    @Override
    protected void cleanupSingleStat(ReadWriteTransaction trans, MeterConfigStats item) {
        InstanceIdentifier<NodeMeterConfigStats> meterRef = getNodeIdentifier()
                            .augmentation(FlowCapableNode.class)
                            .child(Meter.class, new MeterKey(item.getMeterId()))
                            .augmentation(NodeMeterConfigStats.class);
        trans.delete(LogicalDatastoreType.OPERATIONAL, meterRef);
    }

    @Override
    protected MeterConfigStats updateSingleStat(ReadWriteTransaction trans, MeterConfigStats item) {
        MeterBuilder meterBuilder = new MeterBuilder();
        MeterKey meterKey = new MeterKey(item.getMeterId());
        meterBuilder.setKey(meterKey);

        InstanceIdentifier<Meter> meterRef = getNodeIdentifier().augmentation(FlowCapableNode.class)
                .child(Meter.class,meterKey);

        NodeMeterConfigStatsBuilder meterConfig = new NodeMeterConfigStatsBuilder();
        meterConfig.setMeterConfigStats(new MeterConfigStatsBuilder(item).build());

        //Update augmented data
        meterBuilder.addAugmentation(NodeMeterConfigStats.class, meterConfig.build());

        trans.merge(LogicalDatastoreType.OPERATIONAL, meterRef, meterBuilder.build(), true);
        return item;
    }

    @Override
    public void request() {
        if (meterStatsService != null) {
            GetAllMeterConfigStatisticsInputBuilder input = new GetAllMeterConfigStatisticsInputBuilder();
            input.setNode(getNodeRef());

            requestHelper(meterStatsService.getAllMeterConfigStatistics(input.build()));
        }
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        final ReadWriteTransaction trans = startTransaction();

        for (InstanceIdentifier<?> key : change.getRemovedPaths()) {
            if (Meter.class.equals(key.getTargetType())) {
                @SuppressWarnings("unchecked")
                InstanceIdentifier<Meter> meter = (InstanceIdentifier<Meter>)key;

                InstanceIdentifier<?> nodeMeterStatisticsAugmentation =
                        meter.augmentation(NodeMeterConfigStats.class);
                trans.delete(LogicalDatastoreType.OPERATIONAL, nodeMeterStatisticsAugmentation);
            }
        }
        trans.submit();
    }

    @Override
    protected InstanceIdentifier<?> listenPath() {
        return getNodeIdentifier().augmentation(FlowCapableNode.class).child(Meter.class);
    }

    @Override
    protected String statName() {
        return "Meter Config";
    }

    @Override
    public void start(final DataBroker dbs) {
        if (meterStatsService == null) {
            LOG.debug("No Meter Statistics service, not subscribing to meter on node {}", getNodeIdentifier());
            return;
        }

        super.start(dbs);
    }
}
