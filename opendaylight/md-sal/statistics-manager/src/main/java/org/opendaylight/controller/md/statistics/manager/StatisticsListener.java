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
 * This class is responsible for listening for statistics update notifications and
 * routing them to the appropriate NodeStatisticsHandler.

 * TODO: Need to add error message listener and clean-up the associated tx id
 * if it exists in the tx-id cache.
 * @author vishnoianil
 */
public class StatisticsListener implements OpendaylightGroupStatisticsListener,
        OpendaylightMeterStatisticsListener,
        OpendaylightFlowStatisticsListener,
        OpendaylightPortStatisticsListener,
        OpendaylightFlowTableStatisticsListener,
        OpendaylightQueueStatisticsListener{

    private final static Logger sucLogger = LoggerFactory.getLogger(StatisticsListener.class);
    private final StatisticsProvider statisticsManager;
    private final MultipartMessageManager messageManager;

    /**
     * default ctor
     * @param manager
     */
    public StatisticsListener(final StatisticsProvider manager){
        this.statisticsManager = manager;
        this.messageManager = this.statisticsManager.getMultipartMessageManager();
    }

    @Override
    public void onMeterConfigStatsUpdated(final MeterConfigStatsUpdated notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        //Add statistics to local cache
        final NodeStatisticsHandler handler = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (handler != null) {
            handler.updateMeterConfigStats(notification.getMeterConfigStats());
        }
    }

    @Override
    public void onMeterStatisticsUpdated(MeterStatisticsUpdated notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        //Add statistics to local cache
        final NodeStatisticsHandler handler = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (handler != null) {
            handler.updateMeterStats(notification.getMeterStats());
        }
    }

    @Override
    public void onGroupDescStatsUpdated(GroupDescStatsUpdated notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        final NodeStatisticsHandler handler = statisticsManager.getStatisticsHandler(notification.getId());
        if (handler != null) {
            handler.updateGroupDescStats(notification.getGroupDescStats());
        }
    }

    @Override
    public void onGroupStatisticsUpdated(GroupStatisticsUpdated notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        final NodeStatisticsHandler handler = statisticsManager.getStatisticsHandler(notification.getId());
        if (handler != null) {
            handler.updateGroupStats(notification.getGroupStats());
        }
    }

    @Override
    public void onMeterFeaturesUpdated(MeterFeaturesUpdated notification) {
        final NodeStatisticsHandler sna = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (sna != null) {
            sna.updateMeterFeatures(notification);
        }
    }

    @Override
    public void onGroupFeaturesUpdated(GroupFeaturesUpdated notification) {
        final NodeStatisticsHandler sna = this.statisticsManager.getStatisticsHandler(notification.getId());
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
        final NodeStatisticsHandler sna = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (sna != null) {
            sna.updateFlowStats(notification.getFlowAndStatisticsMapList());
        }
    }

    @Override
    public void onAggregateFlowStatisticsUpdate(AggregateFlowStatisticsUpdate notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        final NodeStatisticsHandler handler = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (handler != null) {
            final Short tableId = messageManager.getTableIdForTxId(notification.getId(),notification.getTransactionId());
            handler.updateAggregateFlowStats(tableId, notification);
        }
    }

    @Override
    public void onNodeConnectorStatisticsUpdate(NodeConnectorStatisticsUpdate notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        final NodeStatisticsHandler handler = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (handler != null) {
            handler.updateNodeConnectorStats(notification.getNodeConnectorStatisticsAndPortNumberMap());
        }
    }

    @Override
    public void onFlowTableStatisticsUpdate(FlowTableStatisticsUpdate notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        final NodeStatisticsHandler handler = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (handler != null) {
            handler.updateFlowTableStats(notification.getFlowTableAndStatisticsMap());
        }
    }

    @Override
    public void onQueueStatisticsUpdate(QueueStatisticsUpdate notification) {
        //Check if response is for the request statistics-manager sent.
        if(!messageManager.isRequestTxIdExist(notification.getId(),notification.getTransactionId(),notification.isMoreReplies()))
            return;

        //Add statistics to local cache
        final NodeStatisticsHandler handler = this.statisticsManager.getStatisticsHandler(notification.getId());
        if (handler != null) {
            handler.updateQueueStats(notification.getQueueIdAndStatisticsMap());
        }
    }
}

