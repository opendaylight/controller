/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.md.statistics.manager.NodeStatisticsAger.FlowEntry;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowsStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupDescStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterConfigStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.GenericStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.NodeConnectorStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.QueueStatisticsUpdate;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class implement statistics manager related listener interface and augment all the
 * received statistics data to data stores.
 * TODO: Need to add error message listener and clean-up the associated tx id
 * if it exists in the tx-id cache.
 * @author vishnoianil
 *
 */
public class StatisticsUpdateCommiter implements OpendaylightGroupStatisticsListener,
        OpendaylightMeterStatisticsListener,
        OpendaylightFlowStatisticsListener,
        OpendaylightPortStatisticsListener,
        OpendaylightFlowTableStatisticsListener,
        OpendaylightQueueStatisticsListener{

    private final static Logger sucLogger = LoggerFactory.getLogger(StatisticsUpdateCommiter.class);

    private final StatisticsProvider statisticsManager;
    private final MultipartMessageManager messageManager;

    private int unaccountedFlowsCounter = 1;

    /**
     * default ctor
     * @param manager
     */
    public StatisticsUpdateCommiter(final StatisticsProvider manager){
        this.statisticsManager = manager;
        this.messageManager = this.statisticsManager.getMultipartMessageManager();
    }

    @Override
    public void onMeterConfigStatsUpdated(final MeterConfigStatsUpdated notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        //Add statistics to local cache
        final NodeStatisticsAger sna = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (sna != null) {
            sna.updateMeterConfigStats(notification.getMeterConfigStats());
        }
    }

    @Override
    public void onMeterStatisticsUpdated(MeterStatisticsUpdated notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        //Add statistics to local cache
        final NodeStatisticsAger nsa = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (nsa != null) {
            nsa.updateMeterStats(notification.getMeterStats());
        }
    }

    @Override
    public void onGroupDescStatsUpdated(GroupDescStatsUpdated notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        final NodeStatisticsAger nsa = statisticsManager.getStatisticsHandler(notification.getId());
        if (nsa != null) {
            nsa.updateGroupDescStats(notification.getGroupDescStats());
        }
    }

    @Override
    public void onGroupStatisticsUpdated(GroupStatisticsUpdated notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        final NodeStatisticsAger nsa = statisticsManager.getStatisticsHandler(notification.getId());
        if (nsa != null) {
            nsa.updateGroupStats(notification.getGroupStats());
        }
    }

    @Override
    public void onMeterFeaturesUpdated(MeterFeaturesUpdated notification) {
        final NodeStatisticsAger sna = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (sna != null) {
            sna.updateMeterFeatures(notification);
        }
    }

    @Override
    public void onGroupFeaturesUpdated(GroupFeaturesUpdated notification) {
        final NodeStatisticsAger sna = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (sna != null) {
            sna.updateGroupFeatures(notification);
        }
    }

    @Override
    public void onFlowsStatisticsUpdate(final FlowsStatisticsUpdate notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        sucLogger.debug("Received flow stats update : {}",notification.toString());

        final NodeKey key = new NodeKey(notification.getId());
        final NodeStatisticsAger nsa =  this.statisticsManager.getStatisticsHandler(key.getId());
        DataModificationTransaction it = this.statisticsManager.startChange();

        for(FlowAndStatisticsMapList map: notification.getFlowAndStatisticsMapList()){
            short tableId = map.getTableId();


            boolean foundOriginalFlow = false;

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

            sucLogger.debug("Flow : {}",flowRule.toString());
            sucLogger.debug("Statistics to augment : {}",flowStatistics.build().toString());

            InstanceIdentifier<Table> tableRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, key)
                    .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(tableId)).toInstance();

            Table table= (Table)it.readConfigurationData(tableRef);

            //TODO: Not a good way to do it, need to figure out better way.
            //TODO: major issue in any alternate approach is that flow key is incrementally assigned
            //to the flows stored in data store.
            // Augment same statistics to all the matching masked flow
            if(table != null){

                for(Flow existingFlow : table.getFlow()){
                    sucLogger.debug("Existing flow in data store : {}",existingFlow.toString());
                    if(FlowComparator.flowEquals(flowRule,existingFlow)){
                        InstanceIdentifier<Flow> flowRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, key)
                                .augmentation(FlowCapableNode.class)
                                .child(Table.class, new TableKey(tableId))
                                .child(Flow.class,existingFlow.getKey()).toInstance();
                        flowBuilder.setKey(existingFlow.getKey());
                        flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
                        sucLogger.debug("Found matching flow in the datastore, augmenting statistics");
                        foundOriginalFlow = true;
                        // Update entry with timestamp of latest response
                        flow.setKey(existingFlow.getKey());
                        FlowEntry flowStatsEntry = nsa.new FlowEntry(tableId,flow.build());
                        nsa.updateFlowStats(flowStatsEntry);

                        it.putOperationalData(flowRef, flowBuilder.build());
                    }
                }
            }

            table= (Table)it.readOperationalData(tableRef);
            if(!foundOriginalFlow && table != null){

                for(Flow existingFlow : table.getFlow()){
                    FlowStatisticsData augmentedflowStatisticsData = existingFlow.getAugmentation(FlowStatisticsData.class);
                    if(augmentedflowStatisticsData != null){
                        FlowBuilder existingOperationalFlow = new FlowBuilder();
                        existingOperationalFlow.fieldsFrom(augmentedflowStatisticsData.getFlowStatistics());
                        sucLogger.debug("Existing unaccounted flow in operational data store : {}",existingFlow.toString());
                        if(FlowComparator.flowEquals(flowRule,existingOperationalFlow.build())){
                            InstanceIdentifier<Flow> flowRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, key)
                                    .augmentation(FlowCapableNode.class)
                                    .child(Table.class, new TableKey(tableId))
                                    .child(Flow.class,existingFlow.getKey()).toInstance();
                            flowBuilder.setKey(existingFlow.getKey());
                            flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
                            sucLogger.debug("Found matching unaccounted flow in the operational datastore, augmenting statistics");
                            foundOriginalFlow = true;

                            // Update entry with timestamp of latest response
                            flow.setKey(existingFlow.getKey());
                            FlowEntry flowStatsEntry = nsa.new FlowEntry(tableId,flow.build());
                            nsa.updateFlowStats(flowStatsEntry);

                            it.putOperationalData(flowRef, flowBuilder.build());
                            break;
                        }
                    }
                }
            }
            if(!foundOriginalFlow){
                String flowKey = "#UF$TABLE*"+Short.toString(tableId)+"*"+Integer.toString(this.unaccountedFlowsCounter);
                this.unaccountedFlowsCounter++;
                FlowKey newFlowKey = new FlowKey(new FlowId(flowKey));
                InstanceIdentifier<Flow> flowRef = InstanceIdentifier.builder(Nodes.class).child(Node.class, key)
                        .augmentation(FlowCapableNode.class)
                        .child(Table.class, new TableKey(tableId))
                        .child(Flow.class,newFlowKey).toInstance();
                flowBuilder.setKey(newFlowKey);
                flowBuilder.addAugmentation(FlowStatisticsData.class, flowStatisticsData.build());
                sucLogger.debug("Flow {} is not present in config data store, augmenting statistics as an unaccounted flow",flowBuilder.build());

                // Update entry with timestamp of latest response
                flow.setKey(newFlowKey);
                FlowEntry flowStatsEntry = nsa.new FlowEntry(tableId,flow.build());
                nsa.updateFlowStats(flowStatsEntry);

                it.putOperationalData(flowRef, flowBuilder.build());
            }
        }
        it.commit();
    }

    @Override
    public void onAggregateFlowStatisticsUpdate(AggregateFlowStatisticsUpdate notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        final NodeStatisticsAger nsa = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (nsa != null) {
            final Short tableId = messageManager.getTableIdForTxId(notification.getId(),notification.getTransactionId());
            nsa.updateAggregateFlowStats(tableId, notification);
        }
    }

    @Override
    public void onNodeConnectorStatisticsUpdate(NodeConnectorStatisticsUpdate notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        final NodeStatisticsAger nsa = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (nsa != null) {
            nsa.updateNodeConnectorStats(notification.getNodeConnectorStatisticsAndPortNumberMap());
        }
    }

    @Override
    public void onFlowTableStatisticsUpdate(FlowTableStatisticsUpdate notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        final NodeStatisticsAger nsa = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (nsa != null) {
            nsa.updateFlowTableStats(notification.getFlowTableAndStatisticsMap());
        }
    }

    @Override
    public void onQueueStatisticsUpdate(QueueStatisticsUpdate notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        //Add statistics to local cache
        final NodeStatisticsAger nsa = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (nsa != null) {
            nsa.updateQueueStats(notification.getQueueIdAndStatisticsMap());
        }
    }
}

