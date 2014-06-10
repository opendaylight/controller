/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FlowStatsTracker extends AbstractListeningStatsTracker<FlowAndStatisticsMapList, FlowStatsEntry> {
    private static final Logger logger = LoggerFactory.getLogger(FlowStatsTracker.class);
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
        InstanceIdentifier<?> flowRef = getNodeIdentifierBuilder()
                            .augmentation(FlowCapableNode.class)
                            .child(Table.class, new TableKey(item.getTableId()))
                            .child(Flow.class,item.getFlow().getKey())
                            .augmentation(FlowStatisticsData.class).toInstance();
        trans.removeOperationalData(flowRef);
    }

    @Override
    protected FlowStatsEntry updateSingleStat(final DataModificationTransaction trans, final FlowAndStatisticsMapList map) {
        short tableId = map.getTableId();

        FlowBuilder flowBuilder = new FlowBuilder();

        FlowStatisticsDataBuilder flowStatisticsData = new FlowStatisticsDataBuilder();

        FlowBuilder flow = new FlowBuilder();
        flow.setContainerName(map.getContainerName());
        flow.setBufferId(map.getBufferId());
        flow.setCookie(map.getCookie());
        flow.setCookieMask(map.getCookieMask());
        flow.setFlags(map.getFlags());
        flow.setFlowName(map.getFlowName());
        flow.setHardTimeout(map.getHardTimeout());
        if(map.getFlowId() != null)
            flow.setId(new FlowId(map.getFlowId().getValue()));
        flow.setIdleTimeout(map.getIdleTimeout());
        flow.setInstallHw(map.isInstallHw());
        flow.setInstructions(map.getInstructions());
        if(map.getFlowId()!= null)
            flow.setKey(new FlowKey(new FlowId(map.getKey().getFlowId().getValue())));
        flow.setMatch(map.getMatch());
        flow.setOutGroup(map.getOutGroup());
        flow.setOutPort(map.getOutPort());
        flow.setPriority(map.getPriority());
        flow.setStrict(map.isStrict());
        flow.setTableId(tableId);

        Flow flowRule = flow.build();

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
        flowStatistics.setContainerName(map.getContainerName());
        flowStatistics.setBufferId(map.getBufferId());
        flowStatistics.setCookie(map.getCookie());
        flowStatistics.setCookieMask(map.getCookieMask());
        flowStatistics.setFlags(map.getFlags());
        flowStatistics.setFlowName(map.getFlowName());
        flowStatistics.setHardTimeout(map.getHardTimeout());
        flowStatistics.setIdleTimeout(map.getIdleTimeout());
        flowStatistics.setInstallHw(map.isInstallHw());
        flowStatistics.setInstructions(map.getInstructions());
        flowStatistics.setMatch(map.getMatch());
        flowStatistics.setOutGroup(map.getOutGroup());
        flowStatistics.setOutPort(map.getOutPort());
        flowStatistics.setPriority(map.getPriority());
        flowStatistics.setStrict(map.isStrict());
        flowStatistics.setTableId(tableId);

        flowStatisticsData.setFlowStatistics(flowStatistics.build());

        logger.debug("Flow : {}",flowRule.toString());
        logger.debug("Statistics to augment : {}",flowStatistics.build().toString());

        InstanceIdentifier<Table> tableRef = getNodeIdentifierBuilder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(tableId)).toInstance();

        final FlowCookie flowCookie =
                flow.getCookie() != null ? flow.getCookie() : new FlowCookie(BigInteger.ZERO);

        /* find flowKey in FlowCookieMap from Operational DataStore */
        FlowKey flowKey = this.getFlowKey(flowCookie, flowRule, tableRef, trans);
        InstanceIdentifier<Flow> flowRef = getNodeIdentifierBuilder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(tableId))
                .child(Flow.class,flowKey).toInstance();
        flowBuilder.setKey(flowKey);
        flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
        // Update entry with timestamp of latest response
        flow.setKey(flowKey);
        FlowStatsEntry flowStatsEntry = new FlowStatsEntry(tableId,flow.build());
        trans.putOperationalData(flowRef, flowBuilder.build());
        return flowStatsEntry;
    }

    /* Returns FlowKey created from FlowId which is identified by cookie
     * and by switch flow identification (priority and match) */
    private FlowKey getFlowKey(final FlowCookie flowCookie,
                               final Flow flow,
                               final InstanceIdentifier<Table> tableRef,
                               final DataModificationTransaction trans) {

        InstanceIdentifier<FlowCookieMap> flowCookieRef = tableRef
                .augmentation(FlowCookieMapping.class)
                .child(FlowCookieMap.class, new FlowCookieMapKey(flowCookie));

        FlowCookieMap cookie = (FlowCookieMap) trans.readOperationalData(flowCookieRef);
        if (cookie != null) {
            FlowId flowId = findFlowId(cookie.getFlowIds(), flow, tableRef, trans);
            if (flowId != null) {
                return new FlowKey(flowId);
            }
        }
        /* Doesn't exist in DataStore for now */
        StringBuilder sBuilder = new StringBuilder(ALIEN_SYSTEM_FLOW_ID)
            .append(flow.getTableId()).append("-").append(this.unaccountedFlowsCounter);
        this.unaccountedFlowsCounter++;
        final FlowId flowId = new FlowId(sBuilder.toString());
        if (cookie != null) {
            cookie.getFlowIds().add(flowId);
        } else {
            final FlowCookieMapBuilder flowCookieMapBuilder = new FlowCookieMapBuilder();
            flowCookieMapBuilder.setCookie(flowCookie);
            flowCookieMapBuilder.setFlowIds(new ArrayList<FlowId>(2) {{ this.add(flowId); }});
            cookie = flowCookieMapBuilder.build();
        }
        trans.putOperationalData(flowCookieRef, cookie);
        return new FlowKey(flowId);
    }

    /* Returns FlowId identified by cookie and by switch flow identification (priority and match) */
    private FlowId findFlowId (final List<FlowId> flowIds,
                               final Flow flow,
                               final InstanceIdentifier<Table> tableRef,
                               final DataModificationTransaction trans) {

        for (FlowId flowId : flowIds) {
            InstanceIdentifier<Flow> flowIdent = tableRef.child(Flow.class, new FlowKey(flowId));
            if (flowId.getValue().startsWith(ALIEN_SYSTEM_FLOW_ID)) {
                logger.debug("Search for flow in the operational datastore by flowID: {} ", flowIdent);
                Flow readedFlow = (Flow) trans.readOperationalData(flowIdent);
                FlowStatisticsData augmentedflowStatisticsData = readedFlow.getAugmentation(FlowStatisticsData.class);
                if(augmentedflowStatisticsData != null){
                    FlowBuilder existingOperationalFlow = new FlowBuilder();
                    existingOperationalFlow.fieldsFrom(augmentedflowStatisticsData.getFlowStatistics());
                    if (FlowComparator.flowEquals(flow, existingOperationalFlow.build())) {
                        return flowId;
                    }
                }
            } else {
                logger.debug("Search for flow in the configuration datastore by flowID: {} ", flowIdent);
                Flow readedFlow = (Flow) trans.readConfigurationData(flowIdent);
                if (FlowComparator.flowEquals(flow, readedFlow)) {
                    return flowId;
                }
            }
        }
        logger.debug("Flow was not found in the datastore. Flow {} ", flow);
        return null;
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
        logger.debug("Node {} supports {} table(s)", this.getNodeRef(), tables.size());
        for (final TableKey key : tables) {
            logger.debug("Send aggregate stats request for flow table {} to node {}", key.getId(), this.getNodeRef());
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
                logger.debug("Key {} triggered request for flow {}", e.getKey(), flow);
                requestFlow(flow);
            } else {
                logger.debug("Ignoring key {}", e.getKey());
            }
        }

        final DataModificationTransaction trans = startTransaction();
        for (InstanceIdentifier<?> key : change.getRemovedConfigurationData()) {
            if (Flow.class.equals(key.getTargetType())) {
                @SuppressWarnings("unchecked")
                final InstanceIdentifier<Flow> flow = (InstanceIdentifier<Flow>)key;
                logger.debug("Key {} triggered remove of Flow from operational space.", key);
                trans.removeOperationalData(flow);
            }
        }
        trans.commit();
    }

    @Override
    public void start(final DataBrokerService dbs) {
        if (flowStatsService == null) {
            logger.debug("No Flow Statistics service, not subscribing to flows on node {}", getNodeIdentifier());
            return;
        }

        super.start(dbs);
    }
}
