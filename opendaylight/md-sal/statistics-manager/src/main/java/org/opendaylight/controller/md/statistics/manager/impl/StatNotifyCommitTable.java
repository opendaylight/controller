/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.statistics.manager.StatRpcMsgManager.TransactionCacheContainer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager.StatDataStoreOperation;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
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
public class StatNotifyCommitTable extends StatAbstractNotifyCommit<OpendaylightFlowTableStatisticsListener>
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
        if ( ! isExpectedStatistics(transId)) {
            LOG.debug("STAT-MANAGER - FlowTableStatisticsUpdate: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<FlowTableAndStatisticsMap> tableStats = notification
                .getFlowTableAndStatisticsMap() == null ? Collections.<FlowTableAndStatisticsMap> emptyList()
                  : notification.getFlowTableAndStatisticsMap();
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId);
        /* Don't block RPC Notification thread */
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction trans) {
                /* Notification for continue collecting statistics - Tables statistics are still same size
                 * and they are small - don't need to wait to whole apply operation */
                manager.getStatCollector().collectNextStatistics();

                if (txContainer.isPresent()) {
                    final List<? extends TransactionAware> cachedNotifs =
                            txContainer.get().getNotifications();
                    for (final TransactionAware notif : cachedNotifs) {
                        if (notif instanceof FlowTableStatisticsUpdate) {
                            tableStats.addAll(((FlowTableStatisticsUpdate) notif).getFlowTableAndStatisticsMap());
                        }
                    }
                }
                /* write stat to trans */
                statTableCommit(tableStats, nodeId, trans);
            }
        });
    }

    private void statTableCommit(final List<FlowTableAndStatisticsMap> tableStats, final NodeId nodeId,
            final ReadWriteTransaction trans) {
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId));
        final InstanceIdentifier<FlowCapableNode> fNodeIdent = nodeIdent.augmentation(FlowCapableNode.class);
        /* check flow Capable Node and write statistics */
        Optional<FlowCapableNode> fNode = Optional.absent();
        try {
            fNode = trans.read(LogicalDatastoreType.OPERATIONAL, fNodeIdent).checkedGet();
        }
        catch (final ReadFailedException e) {
            LOG.debug("Read Operational/DS for FlowCapableNode fail! {}", fNodeIdent, e);
            return;
        }
        if ( ! fNode.isPresent()) {
            LOG.trace("Read Operational/DS for FlowCapableNode fail! Node {} doesn't exist.", fNodeIdent);
            return;
        }
        for (final FlowTableAndStatisticsMap tableStat : tableStats) {
            final FlowTableStatistics stats = new FlowTableStatisticsBuilder(tableStat).build();

            final InstanceIdentifier<FlowTableStatistics> tableStatRef = fNodeIdent
                    .child(Table.class, new TableKey(tableStat.getTableId().getValue()))
                    .augmentation(FlowTableStatisticsData.class)
                    .child(FlowTableStatistics.class);
            trans.put(LogicalDatastoreType.OPERATIONAL, tableStatRef, stats, true);
        }
    }
}

