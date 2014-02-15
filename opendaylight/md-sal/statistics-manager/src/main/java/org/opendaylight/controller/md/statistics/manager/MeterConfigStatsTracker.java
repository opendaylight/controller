/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
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
    private static final Logger logger = LoggerFactory.getLogger(MeterConfigStatsTracker.class);
    private final OpendaylightMeterStatisticsService meterStatsService;

    protected MeterConfigStatsTracker(OpendaylightMeterStatisticsService meterStatsService, final FlowCapableContext context, long lifetimeNanos) {
        super(context, lifetimeNanos);
        this.meterStatsService = meterStatsService;
    }

    @Override
    protected void cleanupSingleStat(DataModificationTransaction trans, MeterConfigStats item) {
        InstanceIdentifier<NodeMeterConfigStats> meterRef = getNodeIdentifierBuilder()
                            .augmentation(FlowCapableNode.class)
                            .child(Meter.class, new MeterKey(item.getMeterId()))
                            .augmentation(NodeMeterConfigStats.class).build();
        trans.removeOperationalData(meterRef);
    }

    @Override
    protected MeterConfigStats updateSingleStat(DataModificationTransaction trans, MeterConfigStats item) {
        MeterBuilder meterBuilder = new MeterBuilder();
        MeterKey meterKey = new MeterKey(item.getMeterId());
        meterBuilder.setKey(meterKey);

        InstanceIdentifier<Meter> meterRef = getNodeIdentifierBuilder().augmentation(FlowCapableNode.class)
                .child(Meter.class,meterKey).toInstance();

        NodeMeterConfigStatsBuilder meterConfig = new NodeMeterConfigStatsBuilder();
        meterConfig.setMeterConfigStats(new MeterConfigStatsBuilder(item).build());

        //Update augmented data
        meterBuilder.addAugmentation(NodeMeterConfigStats.class, meterConfig.build());

        trans.putOperationalData(meterRef, meterBuilder.build());
        return item;
    }

    public void request() {
        if (meterStatsService != null) {
            GetAllMeterConfigStatisticsInputBuilder input = new GetAllMeterConfigStatisticsInputBuilder();
            input.setNode(getNodeRef());

            requestHelper(meterStatsService.getAllMeterConfigStatistics(input.build()));
        }
    }

    @Override
    public void onDataChanged(DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        final DataModificationTransaction trans = startTransaction();

        for (InstanceIdentifier<?> key : change.getRemovedConfigurationData()) {
            if (Meter.class.equals(key.getTargetType())) {
                @SuppressWarnings("unchecked")
                InstanceIdentifier<Meter> meter = (InstanceIdentifier<Meter>)key;

                InstanceIdentifier<?> nodeMeterStatisticsAugmentation =
                        InstanceIdentifier.builder(meter).augmentation(NodeMeterConfigStats.class).toInstance();
                trans.removeOperationalData(nodeMeterStatisticsAugmentation);
            }
        }

        trans.commit();
    }

    @Override
    protected InstanceIdentifier<?> listenPath() {
        return getNodeIdentifierBuilder().augmentation(FlowCapableNode.class).child(Meter.class).build();
    }

    @Override
    protected String statName() {
        return "Meter Config";
    }

    @Override
    public void start(final DataBrokerService dbs) {
        if (meterStatsService == null) {
            logger.debug("No Meter Statistics service, not subscribing to meter on node {}", getNodeIdentifier());
            return;
        }

        super.start(dbs);
    }
}
