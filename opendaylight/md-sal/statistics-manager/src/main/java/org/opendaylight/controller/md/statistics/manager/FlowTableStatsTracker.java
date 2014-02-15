/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatisticsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class FlowTableStatsTracker extends AbstractStatsTracker<FlowTableAndStatisticsMap, FlowTableAndStatisticsMap> {
    private final Set<TableKey> privateTables = new ConcurrentSkipListSet<>();
    private final Set<TableKey> tables = Collections.unmodifiableSet(privateTables);
    private final OpendaylightFlowTableStatisticsService flowTableStatsService;

    FlowTableStatsTracker(OpendaylightFlowTableStatisticsService flowTableStatsService, final FlowCapableContext context, long lifetimeNanos) {
        super(context, lifetimeNanos);
        this.flowTableStatsService = flowTableStatsService;
    }

    Set<TableKey> getTables() {
        return tables;
    }

    @Override
    protected void cleanupSingleStat(DataModificationTransaction trans, FlowTableAndStatisticsMap item) {
        // TODO: do we want to do this?
    }

    @Override
    protected FlowTableAndStatisticsMap updateSingleStat(DataModificationTransaction trans, FlowTableAndStatisticsMap item) {

        InstanceIdentifier<Table> tableRef = getNodeIdentifierBuilder()
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(item.getTableId().getValue())).build();

        FlowTableStatisticsDataBuilder statisticsDataBuilder = new FlowTableStatisticsDataBuilder();
        final FlowTableStatistics stats = new FlowTableStatisticsBuilder(item).build();
        statisticsDataBuilder.setFlowTableStatistics(stats);

        TableBuilder tableBuilder = new TableBuilder();
        tableBuilder.setKey(new TableKey(item.getTableId().getValue()));
        tableBuilder.addAugmentation(FlowTableStatisticsData.class, statisticsDataBuilder.build());
        trans.putOperationalData(tableRef, tableBuilder.build());
        return item;
    }

    public void request() {
        if (flowTableStatsService != null) {
            final GetFlowTablesStatisticsInputBuilder input = new GetFlowTablesStatisticsInputBuilder();
            input.setNode(getNodeRef());

            requestHelper(flowTableStatsService.getFlowTablesStatistics(input.build()));
        }
    }
}
