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

    private static final Logger LOG = LoggerFactory.getLogger(MeterStatsTracker.class);

    private final OpendaylightMeterStatisticsService meterStatsService;

    MeterStatsTracker(OpendaylightMeterStatisticsService meterStatsService, final FlowCapableContext context) {
        super(context);
        this.meterStatsService = meterStatsService;
    }


    @Override
    protected void cleanupSingleStat(ReadWriteTransaction trans, MeterStats item) {
        InstanceIdentifier<NodeMeterStatistics> meterRef = getNodeIdentifier()
                            .augmentation(FlowCapableNode.class)
                            .child(Meter.class, new MeterKey(item.getMeterId()))
                            .augmentation(NodeMeterStatistics.class);
        trans.delete(LogicalDatastoreType.OPERATIONAL, meterRef);
    }

    @Override
    protected MeterStats updateSingleStat(ReadWriteTransaction trans, MeterStats item) {
        MeterBuilder meterBuilder = new MeterBuilder();
        MeterKey meterKey = new MeterKey(item.getMeterId());
        meterBuilder.setKey(meterKey);

        InstanceIdentifier<Meter> meterRef = getNodeIdentifier()
                .augmentation(FlowCapableNode.class).child(Meter.class,meterKey);

        NodeMeterStatisticsBuilder meterStatsBuilder= new NodeMeterStatisticsBuilder();
        meterStatsBuilder.setMeterStatistics(new MeterStatisticsBuilder(item).build());

        //Update augmented data
        meterBuilder.addAugmentation(NodeMeterStatistics.class, meterStatsBuilder.build());
        trans.merge(LogicalDatastoreType.OPERATIONAL, meterRef, meterBuilder.build(), true);
        return item;
    }

    @Override
    public void request() {
        if (meterStatsService != null) {
            GetAllMeterStatisticsInputBuilder input = new GetAllMeterStatisticsInputBuilder();
            input.setNode(getNodeRef());

            requestHelper(meterStatsService.getAllMeterStatistics(input.build()));
        }
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (InstanceIdentifier<?> key : change.getCreatedData().keySet()) {
            if (Meter.class.equals(key.getTargetType())) {
                request();
            }
        }

        final ReadWriteTransaction trans = startTransaction();
        for (InstanceIdentifier<?> key : change.getRemovedPaths()) {
            if (Meter.class.equals(key.getTargetType())) {
                @SuppressWarnings("unchecked")
                InstanceIdentifier<Meter> meter = (InstanceIdentifier<Meter>)key;

                InstanceIdentifier<?> nodeMeterStatisticsAugmentation =
                        meter.augmentation(NodeMeterStatistics.class);
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
        return "Meter";
    }

    @Override
    public void start(final DataBroker dataBroker) {
        if (meterStatsService == null) {
            LOG.debug("No Meter Statistics service, not subscribing to meters on node {}", getNodeIdentifier());
            return;
        }

        super.start(dataBroker);
    }
}
