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
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStats;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MeterStatsTracker extends AbstractListeningStatsTracker<MeterStats, MeterStats> {
    private static final Logger logger = LoggerFactory.getLogger(MeterStatsTracker.class);
    private final OpendaylightMeterStatisticsService meterStatsService;

    MeterStatsTracker(OpendaylightMeterStatisticsService meterStatsService, final FlowCapableContext context, long lifetimeNanos) {
        super(context, lifetimeNanos);
        this.meterStatsService = meterStatsService;
    }

    @Override
    protected void cleanupSingleStat(DataModificationTransaction trans, MeterStats item) {
        InstanceIdentifier<NodeMeterStatistics> meterRef = getNodeIdentifierBuilder()
                            .augmentation(FlowCapableNode.class)
                            .child(Meter.class,new MeterKey(item.getMeterId()))
                            .augmentation(NodeMeterStatistics.class).build();
        trans.removeOperationalData(meterRef);
    }

    @Override
    protected MeterStats updateSingleStat(DataModificationTransaction trans, MeterStats item) {
        MeterBuilder meterBuilder = new MeterBuilder();
        MeterKey meterKey = new MeterKey(item.getMeterId());
        meterBuilder.setKey(meterKey);

        InstanceIdentifier<Meter> meterRef = getNodeIdentifierBuilder()
                .augmentation(FlowCapableNode.class).child(Meter.class,meterKey).build();

        NodeMeterStatisticsBuilder meterStatsBuilder= new NodeMeterStatisticsBuilder();
        meterStatsBuilder.setMeterStatistics(new MeterStatisticsBuilder(item).build());

        //Update augmented data
        meterBuilder.addAugmentation(NodeMeterStatistics.class, meterStatsBuilder.build());
        trans.putOperationalData(meterRef, meterBuilder.build());
        return item;
    }

    public void request() {
        if (meterStatsService != null) {
            GetAllMeterStatisticsInputBuilder input = new GetAllMeterStatisticsInputBuilder();
            input.setNode(getNodeRef());

            requestHelper(meterStatsService.getAllMeterStatistics(input.build()));
        }
    }

    @Override
    public void onDataChanged(DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (InstanceIdentifier<?> key : change.getCreatedConfigurationData().keySet()) {
            if (Meter.class.equals(key.getTargetType())) {
                request();
            }
        }

        final DataModificationTransaction trans = startTransaction();
        for (InstanceIdentifier<?> key : change.getRemovedConfigurationData()) {
            if (Meter.class.equals(key.getTargetType())) {
                @SuppressWarnings("unchecked")
                InstanceIdentifier<Meter> meter = (InstanceIdentifier<Meter>)key;

                InstanceIdentifier<?> nodeMeterStatisticsAugmentation =
                        InstanceIdentifier.builder(meter).augmentation(NodeMeterStatistics.class).toInstance();
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
        return "Meter";
    }

    @Override
    public void start(final DataBrokerService dbs) {
        if (meterStatsService == null) {
            logger.debug("No Meter Statistics service, not subscribing to meters on node {}", getNodeIdentifier());
            return;
        }

        super.start(dbs);
    }
}
