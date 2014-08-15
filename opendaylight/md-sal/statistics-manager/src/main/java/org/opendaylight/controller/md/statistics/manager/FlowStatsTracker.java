/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCookieMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.nodes.node.table.FlowCookieMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.nodes.node.table.FlowCookieMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.nodes.node.table.FlowCookieMapKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.GenericStatistics;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

final class FlowStatsTracker extends AbstractListeningStatsTracker<FlowAndStatisticsMapList, FlowStatsEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(FlowStatsTracker.class);
    private static final String ALIEN_SYSTEM_FLOW_ID = "#UF$TABLE*";
    private final OpendaylightFlowStatisticsService flowStatsService;
    private FlowTableStatsTracker flowTableStats;
    private int unaccountedFlowsCounter = 1;


    FlowStatsTracker(final OpendaylightFlowStatisticsService flowStatsService, final FlowCapableContext context) {
        super(context);
        this.flowStatsService = flowStatsService;
    }
    FlowStatsTracker(final OpendaylightFlowStatisticsService flowStatsService, final FlowCapableContext context, final FlowTableStatsTracker flowTableStats) {
        this(flowStatsService, context);
        this.flowTableStats = flowTableStats;
    }

    @Override
    protected void cleanupSingleStat(final DataModificationTransaction trans, final FlowStatsEntry item) {
        KeyedInstanceIdentifier<Flow, FlowKey> flowRef = getNodeIdentifier()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(item.getTableId()))
                .child(Flow.class, item.getFlow().getKey());
        trans.removeOperationalData(flowRef);
    }

    @Override
    protected FlowStatsEntry updateSingleStat(final DataModificationTransaction trans, final FlowAndStatisticsMapList map) {
        short tableId = map.getTableId();

        FlowStatisticsDataBuilder flowStatisticsData = new FlowStatisticsDataBuilder();

        FlowBuilder flowBuilder = new FlowBuilder(map);
        if (map.getFlowId() != null) {
            flowBuilder.setId(new FlowId(map.getFlowId().getValue()));
        }
        if (map.getFlowId() != null) {
            flowBuilder.setKey(new FlowKey(new FlowId(map.getKey().getFlowId().getValue())));
        }

        Flow flowRule = flowBuilder.build();

        FlowAndStatisticsMapListBuilder stats = new FlowAndStatisticsMapListBuilder();
        stats.setByteCount(map.getByteCount());
        stats.setPacketCount(map.getPacketCount());
        stats.setDuration(map.getDuration());

        GenericStatistics flowStats = stats.build();

        //Augment the data to the flow node

        FlowStatisticsBuilder flowStatistics = new FlowStatisticsBuilder();
        flowStatistics.setByteCount(flowStats.getByteCount());
        flowStatistics.setPacketCount(flowStats.getPacketCount());
        flowStatistics.setDuration(flowStats.getDuration());

        flowStatisticsData.setFlowStatistics(flowStatistics.build());

        LOG.debug("Flow : {}",flowRule.toString());
        LOG.debug("Statistics to augment : {}",flowStatistics.build().toString());

        InstanceIdentifier<Table> tableRef = getNodeIdentifierBuilder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(tableId)).toInstance();

        final FlowCookie flowCookie = flowRule.getCookie() != null
                ? flowRule.getCookie() : new FlowCookie(BigInteger.ZERO);
        final InstanceIdentifier<FlowCookieMap> flowCookieRef = tableRef
                .augmentation(FlowCookieMapping.class)
                .child(FlowCookieMap.class, new FlowCookieMapKey(flowCookie));

        FlowCookieMap cookieMap = (FlowCookieMap) trans.readOperationalData(flowCookieRef);

        /* find flowKey in FlowCookieMap from DataStore/OPERATIONAL */
        Optional<FlowKey> flowKey = this.getExistFlowKey(flowRule, tableRef, trans, cookieMap);
        if ( ! flowKey.isPresent()) {
            /* DataStore/CONFIG For every first statistic needs to be created */
            flowKey = this.getFlowKeyFromExistFlow(flowRule, tableRef, trans);
            if ( ! flowKey.isPresent()) {
                /* Alien flow */
                flowKey = this.makeAlienFlowKey(flowRule);
            }
            cookieMap = applyNewFlowKey(cookieMap, flowKey, flowCookie);
            trans.putOperationalData(flowCookieRef, cookieMap);
        }

        InstanceIdentifier<Flow> flowRef = getNodeIdentifierBuilder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(tableId))
                .child(Flow.class, flowKey.get()).toInstance();
        flowBuilder.setKey(flowKey.get());
        flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());

        // Update entry with timestamp of latest response
        flowBuilder.setKey(flowKey.get());
        FlowStatsEntry flowStatsEntry = new FlowStatsEntry(tableId, flowBuilder.build());
        trans.putOperationalData(flowRef, flowBuilder.build());
        return flowStatsEntry;
    }

    @Override
    protected InstanceIdentifier<?> listenPath() {
        return getNodeIdentifierBuilder().augmentation(FlowCapableNode.class).child(Table.class).child(Flow.class).build();
    }

    @Override
    protected String statName() {
        return "Flow";
    }

    @Override
    public void request() {
        // FIXME: it does not make sense to trigger this before sendAllFlowTablesStatisticsRequest()
        //        comes back -- we do not have any tables anyway.
        final Collection<TableKey> tables = flowTableStats.getTables();
        LOG.debug("Node {} supports {} table(s)", this.getNodeRef(), tables.size());
        for (final TableKey key : tables) {
            LOG.debug("Send aggregate stats request for flow table {} to node {}", key.getId(), this.getNodeRef());
            this.requestAggregateFlows(key);
        }

        this.requestAllFlowsAllTables();

    }
    public void requestAllFlowsAllTables() {
        if (flowStatsService != null) {
            final GetAllFlowsStatisticsFromAllFlowTablesInputBuilder input = new GetAllFlowsStatisticsFromAllFlowTablesInputBuilder();
            input.setNode(getNodeRef());

            requestHelper(flowStatsService.getAllFlowsStatisticsFromAllFlowTables(input.build()));
        }
    }

    public void requestAggregateFlows(final TableKey key) {
        if (flowStatsService != null) {
            GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder input =
                    new GetAggregateFlowStatisticsFromFlowTableForAllFlowsInputBuilder();

            input.setNode(getNodeRef());
            input.setTableId(new org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId(key.getId()));
            requestHelper(flowStatsService.getAggregateFlowStatisticsFromFlowTableForAllFlows(input.build()));
        }
    }

    public void requestFlow(final Flow flow) {
        if (flowStatsService != null) {
            final GetFlowStatisticsFromFlowTableInputBuilder input =
                    new GetFlowStatisticsFromFlowTableInputBuilder(flow);
            input.setNode(getNodeRef());

            requestHelper(flowStatsService.getFlowStatisticsFromFlowTable(input.build()));
        }
    }

    @Override
    public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (Entry<InstanceIdentifier<?>, DataObject> e : change.getCreatedConfigurationData().entrySet()) {
            if (Flow.class.equals(e.getKey().getTargetType())) {
                final Flow flow = (Flow) e.getValue();
                LOG.debug("Key {} triggered request for flow {}", e.getKey(), flow);
                requestFlow(flow);
            } else {
                LOG.debug("Ignoring key {}", e.getKey());
            }
        }

        final DataModificationTransaction trans = startTransaction();
        for (InstanceIdentifier<?> key : change.getRemovedConfigurationData()) {
            if (Flow.class.equals(key.getTargetType())) {
                @SuppressWarnings("unchecked")
                final InstanceIdentifier<Flow> flow = (InstanceIdentifier<Flow>)key;
                LOG.debug("Key {} triggered remove of Flow from operational space.", key);
                trans.removeOperationalData(flow);
            }
        }
        trans.commit();
    }

    @Override
    public void start(final DataBrokerService dbs) {
        if (flowStatsService == null) {
            LOG.debug("No Flow Statistics service, not subscribing to flows on node {}", getNodeIdentifier());
            return;
        }

        super.start(dbs);
    }

    /* Returns Exist FlowKey from exist FlowCookieMap identified by cookie
     * and by switch flow identification (priority and match)*/
    private Optional<FlowKey> getExistFlowKey(final Flow flowRule, final InstanceIdentifier<Table> tableRef,
            final DataModificationTransaction trans, final FlowCookieMap cookieMap) {

        if (cookieMap != null) {
            for (FlowId flowId : cookieMap.getFlowIds()) {
                InstanceIdentifier<Flow> flowIdent = tableRef.child(Flow.class, new FlowKey(flowId));
                if (flowId.getValue().startsWith(ALIEN_SYSTEM_FLOW_ID)) {
                    LOG.debug("Search for flow in the operational datastore by flowID: {} ", flowIdent);
                    Flow readedFlow = (Flow) trans.readOperationalData(flowIdent);
                    if (FlowComparator.flowEquals(flowRule, readedFlow)) {
                        return Optional.<FlowKey> of(new FlowKey(flowId));
                    }
                } else {
                    LOG.debug("Search for flow in the configuration datastore by flowID: {} ", flowIdent);
                    Flow readedFlow = (Flow) trans.readConfigurationData(flowIdent);
                    if (FlowComparator.flowEquals(flowRule, readedFlow)) {
                        return Optional.<FlowKey> of(new FlowKey(flowId));
                    }
                }
            }
            LOG.debug("Flow was not found in the datastore. Flow {} ", flowRule);
        }
        return Optional.absent();
    }

    /* Returns FlowKey from existing Flow in DataStore/CONFIGURATIONAL which is identified by cookie
     * and by switch flow identification (priority and match) */
    private Optional<FlowKey> getFlowKeyFromExistFlow(final Flow flowRule, final InstanceIdentifier<Table> tableRef,
            final DataModificationTransaction trans) {

        /* Try to find it in DataSotre/CONFIG */
        Table table= (Table)trans.readConfigurationData(tableRef);
        if(table != null) {
            for(Flow existingFlow : table.getFlow()) {
                LOG.debug("Existing flow in data store : {}",existingFlow.toString());
                if(FlowComparator.flowEquals(flowRule,existingFlow)){
                    return Optional.<FlowKey> of(new FlowKey(existingFlow.getId()));
                }
            }
        }
        return Optional.absent();
    }

    /* Returns FlowKey which doesn't exist in any DataStore for now */
    private Optional<FlowKey> makeAlienFlowKey(final Flow flowRule) {

        StringBuilder sBuilder = new StringBuilder(ALIEN_SYSTEM_FLOW_ID)
            .append(flowRule.getTableId()).append("-").append(this.unaccountedFlowsCounter);
        this.unaccountedFlowsCounter++;
        final FlowId flowId = new FlowId(sBuilder.toString());
        return Optional.<FlowKey> of(new FlowKey(flowId));
    }

    /* Build new whole FlowCookieMap or add new flowKey */
    private FlowCookieMap applyNewFlowKey(FlowCookieMap flowCookieMap, final Optional<FlowKey> flowKey,
            final FlowCookie flowCookie) {
        if (flowCookieMap != null) {
            flowCookieMap.getFlowIds().add(flowKey.get().getId());
        } else {
            final FlowCookieMapBuilder flowCookieMapBuilder = new FlowCookieMapBuilder();
            flowCookieMapBuilder.setCookie(flowCookie);
            flowCookieMapBuilder.setFlowIds(Collections.singletonList(flowKey.get().getId()));
            flowCookieMap = flowCookieMapBuilder.build();
        }
        return flowCookieMap;
    }
}
