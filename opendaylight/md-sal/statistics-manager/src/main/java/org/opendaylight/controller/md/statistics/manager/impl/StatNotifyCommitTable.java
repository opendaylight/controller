/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.statistics.manager.StatDeviceMsgManager.TransactionCacheContainer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * StatNotifyCommitTable
 * Class is a NotifyListener for TableStatistics
 * All expected (registered) tableStatistics will be builded and
 * commit to Operational/DataStore
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
class StatNotifyCommitTable extends StatAbstractNotifyCommit<OpendaylightFlowTableStatisticsListener>
                                        implements OpendaylightFlowTableStatisticsListener {

    private final static Logger LOG = LoggerFactory.getLogger(StatNotifyCommitTable.class);

    public StatNotifyCommitTable(final StatisticsManager manager,
            final NotificationProviderService nps) {
        super(manager, nps);
    }

    @Override
    protected OpendaylightFlowTableStatisticsListener getStatNotificationListener() {
        return this;
    }

    @Override
    public void onFlowTableStatisticsUpdate(final FlowTableStatisticsUpdate notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! manager.getDeviceMsgManager().isExpectedStatistics(transId)) {
//            return;
            LOG.warn("STAT-MANAGER - FlowTableStatisticsUpdate: unregistred notification detect TransactionId {}", transId);
        }
        if (notification.isMoreReplies()) {
            manager.getDeviceMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<FlowTableAndStatisticsMap> tableStats = notification
                .getFlowTableAndStatisticsMap() == null ? new ArrayList<FlowTableAndStatisticsMap>(10)
                  : new ArrayList<FlowTableAndStatisticsMap>(notification.getFlowTableAndStatisticsMap());
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getDeviceMsgManager().getTransactionCacheContainer(transId);
        if (txContainer.isPresent()) {
            final List<? extends TransactionAware> cachedNotifs =
                    txContainer.get().getNotifications();
            for (final TransactionAware notif : cachedNotifs) {
                if (notif instanceof FlowTableStatisticsUpdate) {
                    tableStats.addAll(((FlowTableStatisticsUpdate) notif).getFlowTableAndStatisticsMap());
                }
            }
        }
        statTableCommit(tableStats, nodeId);
    }

    private void statTableCommit(final List<FlowTableAndStatisticsMap> tableStats, final NodeId nodeId) {
        final WriteTransaction trans = manager.getWriteTransaction();
        final InstanceIdentifier<FlowCapableNode> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId))
                .augmentation(FlowCapableNode.class);
        for (final FlowTableAndStatisticsMap tableStat : tableStats) {
            final InstanceIdentifier<Table> tableRef = nodeIdent
                    .child(Table.class, new TableKey(tableStat.getTableId().getValue()));
            final FlowTableStatisticsDataBuilder statisticsDataBuilder = new FlowTableStatisticsDataBuilder();
            final FlowTableStatistics stats = new FlowTableStatisticsBuilder(tableStat).build();
            statisticsDataBuilder.setFlowTableStatistics(stats);

            final TableBuilder tableBuilder = new TableBuilder();
            tableBuilder.setKey(new TableKey(tableStat.getTableId().getValue()));
            tableBuilder.addAugmentation(FlowTableStatisticsData.class, statisticsDataBuilder.build());
            trans.merge(LogicalDatastoreType.OPERATIONAL, tableRef, tableBuilder.build(), true);
        }
        continueStatCollecting(trans.submit());
    }
}

