/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsListener;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager
 *
 * StatisticsManager
 * It represent a central point for whole module. Implementation
 * StatisticsManager registers all Operation/DS {@link StatNotifyCommiter} and
 * Config/DS {@StatListeningCommiter}, as well as {@link StatRepeatedlyEnforcer}
 * for statistic collecting and {@StatDeviceCollecto} as Device RPCs provider.
 * In next, StatisticsManager provides all DS contact Transaction services.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 27, 2014
 */
public interface StatisticsManager extends AutoCloseable {

    void start(final NotificationProviderService notifService,
            final RpcConsumerRegistry rpcRegistry, final long minReqNetMonitInt);

    /**
     * Method provides access to StatisticCollector process represent by
     * {@link StatRepeatedlyEnforcer} Access is used mainly for notification
     * to the statistic collecting continuation -> collectNextStatistics()
     * or for check actually locked or registered {@link FlowCapable}
     *
     * @return
     */
    StatRepeatedlyEnforcer getRepeatedlyEnforcer();

    /**
     * Method provides access to Device RPC methods by wrapped
     * internal method. In next {@link StatDeviceMsgManager} is registered all
     * Multipart device msg response and joining all to be able run all
     * collected statistics in one time (easy identification Data for delete)
     *
     * @return
     */
    StatDeviceMsgManager getDeviceMsgManager();

    /**
     * Method returns NEW ReadWriteTransaction service
     * from {@link org.opendaylight.controller.md.sal.binding.api.DataBroker}
     * @return
     */
    ReadWriteTransaction getReadWriteTransaction();

    /**
     * Method returns NEW WriteTransaction service
     * from {@link org.opendaylight.controller.md.sal.binding.api.DataBroker}
     * @return
     */
    WriteTransaction getWriteTransaction();

    /**
     * Method returns NEW ReadTransaction service
     * from {@link org.opendaylight.controller.md.sal.binding.api.DataBroker}
     * @return
     */
    ReadTransaction getReadTransaction();

    /**
     * Define Method : FlowCapableNode Operational/DS data change listener ->
     * -> impl. target -> register FlowCapableNode to Statistic Collecting process
     * @return
     */
    StatListeningCommiter<FlowCapableNode> getFlowNodeListenRegistrator();

    /**
     * Define Method : Flow Config/DS data change listener -> impl. target ->
     * -> make pair between Config/DS FlowId and Device Flow response Hash
     * @return
     */
    StatListeningCommiter<Flow> getFlowListenComit();

    /**
     * Define Method : Meter Config/DS data change listener and notify commit
     * functionality
     * @return
     */
    StatListeningCommiter<Meter> getMeterListenCommit();

    /**
     * Define Method : Group Config/DS data change listener and notify commit
     * functionality
     * @return
     */
    StatListeningCommiter<Group> getGroupListenCommit();

    /**
     * Define Method : Table Operation/DS notify commit functionality
     * @return
     */
    StatNotifyCommiter<OpendaylightFlowTableStatisticsListener> getTableNotifCommit();

    /**
     * Define Method : Port Operation/DS notify commit functionality
     * @return
     */
    StatNotifyCommiter<OpendaylightPortStatisticsListener> getPortNotifyCommit();

    /**
     * Define Method : Table Operation/DS notify commit functionality
     * @return
     */
    StatNotifyCommiter<OpendaylightQueueStatisticsListener> getQueueNotifyCommit();

}

