/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.GenericStatistics;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FlowStatsTracker extends AbstractStatsTracker<FlowAndStatisticsMapList, FlowStatsEntry> {
    private static final Logger logger = LoggerFactory.getLogger(FlowStatsTracker.class);
    private int unaccountedFlowsCounter = 1;

    FlowStatsTracker(InstanceIdentifier<Node> nodeIdentifier, DataProviderService dps, long lifetimeNanos) {
        super(nodeIdentifier, dps, lifetimeNanos);
    }

    @Override
    protected void cleanupSingleStat(DataModificationTransaction trans, FlowStatsEntry item) {
        InstanceIdentifier<?> flowRef = getNodeIdentifierBuilder()
                            .augmentation(FlowCapableNode.class)
                            .child(Table.class, new TableKey(item.getTableId()))
                            .child(Flow.class,item.getFlow().getKey())
                            .augmentation(FlowStatisticsData.class).toInstance();
        trans.removeOperationalData(flowRef);
    }

    @Override
    protected FlowStatsEntry updateSingleStat(DataModificationTransaction trans, FlowAndStatisticsMapList map) {
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
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(tableId)).toInstance();

        //TODO: Not a good way to do it, need to figure out better way.
        //TODO: major issue in any alternate approach is that flow key is incrementally assigned
        //to the flows stored in data store.
        // Augment same statistics to all the matching masked flow
        Table table= (Table)trans.readConfigurationData(tableRef);
        if(table != null){
            for(Flow existingFlow : table.getFlow()){
                logger.debug("Existing flow in data store : {}",existingFlow.toString());
                if(FlowComparator.flowEquals(flowRule,existingFlow)){
                    InstanceIdentifier<Flow> flowRef = getNodeIdentifierBuilder()
                            .augmentation(FlowCapableNode.class)
                            .child(Table.class, new TableKey(tableId))
                            .child(Flow.class,existingFlow.getKey()).toInstance();
                    flowBuilder.setKey(existingFlow.getKey());
                    flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
                    logger.debug("Found matching flow in the datastore, augmenting statistics");
                    // Update entry with timestamp of latest response
                    flow.setKey(existingFlow.getKey());
                    FlowStatsEntry flowStatsEntry = new FlowStatsEntry(tableId,flow.build());
                    trans.putOperationalData(flowRef, flowBuilder.build());
                    return flowStatsEntry;
                }
            }
        }

        table = (Table)trans.readOperationalData(tableRef);
        if(table != null){
            for(Flow existingFlow : table.getFlow()){
                FlowStatisticsData augmentedflowStatisticsData = existingFlow.getAugmentation(FlowStatisticsData.class);
                if(augmentedflowStatisticsData != null){
                    FlowBuilder existingOperationalFlow = new FlowBuilder();
                    existingOperationalFlow.fieldsFrom(augmentedflowStatisticsData.getFlowStatistics());
                    logger.debug("Existing unaccounted flow in operational data store : {}",existingFlow.toString());
                    if(FlowComparator.flowEquals(flowRule,existingOperationalFlow.build())){
                        InstanceIdentifier<Flow> flowRef = getNodeIdentifierBuilder()
                                .augmentation(FlowCapableNode.class)
                                .child(Table.class, new TableKey(tableId))
                                .child(Flow.class,existingFlow.getKey()).toInstance();
                        flowBuilder.setKey(existingFlow.getKey());
                        flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
                        logger.debug("Found matching unaccounted flow in the operational datastore, augmenting statistics");
                        // Update entry with timestamp of latest response
                        flow.setKey(existingFlow.getKey());
                        FlowStatsEntry flowStatsEntry = new FlowStatsEntry(tableId,flow.build());
                        trans.putOperationalData(flowRef, flowBuilder.build());
                        return flowStatsEntry;
                    }
                }
            }
        }

        String flowKey = "#UF$TABLE*"+Short.toString(tableId)+"*"+Integer.toString(this.unaccountedFlowsCounter);
        this.unaccountedFlowsCounter++;
        FlowKey newFlowKey = new FlowKey(new FlowId(flowKey));
        InstanceIdentifier<Flow> flowRef = getNodeIdentifierBuilder().augmentation(FlowCapableNode.class)
                    .child(Table.class, new TableKey(tableId))
                    .child(Flow.class,newFlowKey).toInstance();
        flowBuilder.setKey(newFlowKey);
        flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
        logger.debug("Flow {} is not present in config data store, augmenting statistics as an unaccounted flow",
                    flowBuilder.build());

        // Update entry with timestamp of latest response
        flow.setKey(newFlowKey);
        FlowStatsEntry flowStatsEntry = new FlowStatsEntry(tableId,flow.build());
        trans.putOperationalData(flowRef, flowBuilder.build());
        return flowStatsEntry;
    }
}
