/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.math.BigInteger;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.GenericStatistics;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

final class FlowStatsTracker extends AbstractListeningStatsTracker<FlowAndStatisticsMapList, FlowStatsEntry> {
    private static final Logger logger = LoggerFactory.getLogger(FlowStatsTracker.class);
    private final OpendaylightFlowStatisticsService flowStatsService;
    private int unaccountedFlowsCounter = 1;

    /*
     * This is our cookie -> flow ID cache. It is maintained by listening for configuration data changes
     * and adjusting it. We need it for quickly matching flows coming up from the switch back to MD-SAL
     * data store.
     *
     * TODO: this can easily be maintained inside MD-SAL operational data store, using fast reads, as we
     *       would be performing reads of small data by its primary key.
     */
    private final BiMap<BigInteger, FlowKey> cookieMap = HashBiMap.create();

    /*
     * This is our flow -> flow ID cache. We need to maintain this for unaccounted threads, so hopefully
     * this is going to be small. We are using weak keys, as flows may come and go without us knowing --
     * WeakHashMap will provide us with consistency as long as no memory pressure is built up or as long
     * as the flow stat entry is still hanging somewhere.
     *
     * TODO: this too can be stored in MD-SAL. Here the additional challenge is to create a primary key.
     *       Fortunately the YANG Java binding spec guarantees that DTOs which compare equal have the same
     *       toString() result, so the table will be String->String :)
     */
    private final Map<Flow, FlowKey> flowMap = new WeakHashMap<>();


    FlowStatsTracker(OpendaylightFlowStatisticsService flowStatsService, final FlowCapableContext context, long lifetimeNanos) {
        super(context, lifetimeNanos);
        this.flowStatsService = flowStatsService;
    }

    @Override
    protected void cleanupSingleStat(DataModificationTransaction trans, FlowStatsEntry item) {
        InstanceIdentifier<?> flowRef = getNodeIdentifierBuilder()
                            .augmentation(FlowCapableNode.class)
                            .child(Table.class, new TableKey(item.getTableId()))
                            .child(Flow.class, item.getFlow())
                            .augmentation(FlowStatisticsData.class).toInstance();
        trans.removeOperationalData(flowRef);
    }

    private FlowKey keyForCookie(final BigInteger cookie) {
        if (cookie == null) {
            logger.debug("Encountered a foreign flow");
            return null;
        }

        final FlowKey key = cookieMap.get(cookie);
        if (key == null) {
            logger.debug("Cookie {} is not currently known", cookie);
        } else {
            logger.debug("Cookie {} mapped to flow {}", cookie, key);
        }

        return key;
    }

    private FlowKey keyForFlow(final FlowAndStatisticsMapList map) {
        final FlowBuilder fb = new FlowBuilder();
        fb.setContainerName(map.getContainerName());
        fb.setBufferId(map.getBufferId());
        fb.setCookie(map.getCookie());
        fb.setCookieMask(map.getCookieMask());
        fb.setFlags(map.getFlags());
        fb.setFlowName(map.getFlowName());
        fb.setHardTimeout(map.getHardTimeout());
        fb.setIdleTimeout(map.getIdleTimeout());
        fb.setInstallHw(map.isInstallHw());
        fb.setInstructions(map.getInstructions());
        fb.setMatch(map.getMatch());
        fb.setOutGroup(map.getOutGroup());
        fb.setOutPort(map.getOutPort());
        fb.setPriority(map.getPriority());
        fb.setStrict(map.isStrict());
        fb.setTableId(map.getTableId());

        final Flow flow = fb.build();
        FlowKey key = flowMap.get(flow);
        if (key != null) {
            logger.debug("Unaccounted flow reused at {}", key);
            return key;
        }

        final String keyName = "#UF$TABLE*" + String.valueOf(map.getTableId()) + '*' + String.valueOf(unaccountedFlowsCounter);
        unaccountedFlowsCounter++;

        key = new FlowKey(new FlowId(keyName));
        logger.debug("Created new key {} for unaccounted flow {}", key, flow);
        flowMap.put(flow, key);
        return key;
    }

    @Override
    protected FlowStatsEntry updateSingleStat(DataModificationTransaction trans, FlowAndStatisticsMapList map) {
        short tableId = map.getTableId();

        final FlowStatisticsDataBuilder flowStatisticsData = new FlowStatisticsDataBuilder();


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

        FlowStatistics fst = flowStatistics.build();
        flowStatisticsData.setFlowStatistics(fst);

        logger.debug("Statistics to augment : {}", fst);

        FlowKey key = keyForCookie(map.getCookie());
        if (key == null) {
            key = keyForFlow(map);
        }
        Preconditions.checkState(key != null, "Key should have been asigned");

        InstanceIdentifier<Flow> flowRef = getNodeIdentifierBuilder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(tableId))
                .child(Flow.class, key).build();

        final FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setId(key.getId());
        flowBuilder.setKey(key);
        flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());

        trans.putOperationalData(flowRef, flowBuilder.build());
        return new FlowStatsEntry(tableId, key);
    }

    @Override
    protected InstanceIdentifier<?> listenPath() {
        return getNodeIdentifierBuilder().augmentation(FlowCapableNode.class).child(Table.class).child(Flow.class).build();
    }

    @Override
    protected String statName() {
        return "Flow";
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
    public synchronized void onDataChanged(DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (Entry<InstanceIdentifier<?>, DataObject> e : change.getCreatedConfigurationData().entrySet()) {
            if (Flow.class.equals(e.getKey().getTargetType())) {
                final Flow flow = (Flow) e.getValue();
                logger.debug("Key {} triggered request for flow {}", e.getKey(), flow);

                if (flow.getCookie() != null) {
                    cookieMap.put(flow.getCookie(), flow.getKey());
                    logger.debug("Key {} added flow {} cookie {}", e.getKey(), flow.getKey(), flow.getCookie());
                } else {
                    logger.debug("Not adding flow {} into cache, as it does not have a cookie", flow);
                }

                requestFlow(flow);
            } else {
                logger.debug("Ignoring created key {}", e.getKey());
            }
        }

        for (Entry<InstanceIdentifier<?>, DataObject> e : change.getCreatedConfigurationData().entrySet()) {
            if (Flow.class.equals(e.getKey().getTargetType())) {
                final Flow flow = (Flow) e.getValue();

                final BigInteger oc = cookieMap.inverse().remove(flow.getKey());
                final BigInteger nc = flow.getCookie();
                if (nc != null) {
                    cookieMap.put(nc, flow.getKey());
                }

                logger.debug("Key {} updated cookie mapping for flow {} from {} to {}", e.getKey(), flow.getKey(), oc, nc);
            } else {
                logger.debug("Ignoring updated key {}", e.getKey());
            }
        }

        final DataModificationTransaction trans = startTransaction();
        for (InstanceIdentifier<?> key : change.getRemovedConfigurationData()) {
            if (Flow.class.equals(key.getTargetType())) {
                @SuppressWarnings("unchecked")
                final InstanceIdentifier<Flow> flow = (InstanceIdentifier<Flow>)key;
                final InstanceIdentifier<?> del = InstanceIdentifier.builder(flow)
                        .augmentation(FlowStatisticsData.class).build();
                logger.debug("Key {} triggered remove of augmentation {}", key, del);

                final FlowKey fk = flow.firstKeyOf(Flow.class, FlowKey.class);
                final BigInteger cookie = cookieMap.inverse().remove(fk);
                logger.debug("Key {} removed flow {} cookie {}", key, fk, cookie);

                trans.removeOperationalData(del);
            } else {
                logger.debug("Ignoring removed key {}", key);
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
