/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.statistics.manager.StatListeningCommiter;
import org.opendaylight.controller.md.statistics.manager.StatRepeatedlyEnforcer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 27, 2014
 */
public class StatisticManagerImpl implements StatisticsManager {

    private final static Logger LOG = LoggerFactory.getLogger(StatisticManagerImpl.class);

    private final DataBroker dataBroker;
    private StatRepeatedlyEnforcer repeatedlyEnforcer;

    private StatListeningCommiter<FlowCapableNode> nodeRegistrator;
    private StatListeningCommiter<Flow> flowListeningCommiter;
    private StatListeningCommiter<Meter> meterListeningCommiter;
    private StatListeningCommiter<Group> groupListeningCommiter;
    private StatListeningCommiter<Table> tableListeningCommiter;

    private final OpendaylightGroupStatisticsService groupStatsService;
    private final OpendaylightMeterStatisticsService meterStatsService;
    private final OpendaylightFlowStatisticsService flowStatsService;
    private final OpendaylightPortStatisticsService portStatsService;
    private final OpendaylightFlowTableStatisticsService flowTableStatsService;
    private final OpendaylightQueueStatisticsService queueStatsService;

    public StatisticManagerImpl (final DataBroker dataBroker,
            final RpcConsumerRegistry rpcRegistry) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker, "DataBroker can not be null!");
        Preconditions.checkArgument(rpcRegistry != null, "RpcConsumerRegistry can not be null !");
        groupStatsService = Preconditions.checkNotNull(
                rpcRegistry.getRpcService(OpendaylightGroupStatisticsService.class),
                "OpendaylightGroupStatisticsService can not be null!");
        meterStatsService = Preconditions.checkNotNull(
                rpcRegistry.getRpcService(OpendaylightMeterStatisticsService.class),
                "OpendaylightMeterStatisticsService can not be null!");
        flowStatsService = Preconditions.checkNotNull(
                rpcRegistry.getRpcService(OpendaylightFlowStatisticsService.class),
                "OpendaylightFlowStatisticsService can not be null!");
        portStatsService = Preconditions.checkNotNull(
                rpcRegistry.getRpcService(OpendaylightPortStatisticsService.class),
                "OpendaylightPortStatisticsService can not be null!");
        flowTableStatsService = Preconditions.checkNotNull(
                rpcRegistry.getRpcService(OpendaylightFlowTableStatisticsService.class),
                "OpendaylightFlowTableStatisticsService can not be null!");
        queueStatsService = Preconditions.checkNotNull(
                rpcRegistry.getRpcService(OpendaylightQueueStatisticsService.class),
                "OpendaylightQueueStatisticsService can not be null!");
    }

    public void start(final NotificationProviderService notifService) {
        repeatedlyEnforcer = new StatRepeatedlyEnforcerImpl(StatisticManagerImpl.this);

    }

    @Override
    public ReadWriteTransaction getReadWriteTransaction() {
        return dataBroker.newReadWriteTransaction();
    }

    @Override
    public WriteTransaction getWriteTransaction() {
        return dataBroker.newWriteOnlyTransaction();
    }

    @Override
    public OpendaylightGroupStatisticsService getGroupStatsService() {
        return groupStatsService;
    }

    @Override
    public OpendaylightMeterStatisticsService getMeterStatsService() {
        return meterStatsService;
    }

    @Override
    public OpendaylightFlowStatisticsService getFlowStatsService() {
        return flowStatsService;
    }

    @Override
    public OpendaylightPortStatisticsService getPortStatsService() {
        return portStatsService;
    }

    @Override
    public OpendaylightFlowTableStatisticsService getFlowTableStatsService() {
        return flowTableStatsService;
    }

    @Override
    public OpendaylightQueueStatisticsService getQueueStatsService() {
        return queueStatsService;
    }
}

