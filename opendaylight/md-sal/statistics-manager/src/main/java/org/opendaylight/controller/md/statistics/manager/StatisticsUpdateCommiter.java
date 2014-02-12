/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowsStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupDescStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterConfigStatsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterFeaturesUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterStatisticsUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.NodeConnectorStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.QueueStatisticsUpdate;
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
        final NodeStatisticsAger sna = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (sna != null) {
            sna.updateFlowStats(notification.getFlowAndStatisticsMapList());
        }
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

