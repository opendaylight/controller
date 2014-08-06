/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.statistics.manager.StatRepeatedlyEnforcer.StatCapabTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 27, 2014
 */
public interface StatisticsManager {

    StatRepeatedlyEnforcer getRepeatedlyEnforcer();

    ReadWriteTransaction getReadWriteTransaction();

    WriteTransaction getWriteTransaction();

    OpendaylightGroupStatisticsService getGroupStatsService();

    OpendaylightMeterStatisticsService getMeterStatsService();

    OpendaylightFlowStatisticsService getFlowStatsService();

    OpendaylightPortStatisticsService getPortStatsService();

    OpendaylightFlowTableStatisticsService getFlowTableStatsService();

    OpendaylightQueueStatisticsService getQueueStatsService();
}
