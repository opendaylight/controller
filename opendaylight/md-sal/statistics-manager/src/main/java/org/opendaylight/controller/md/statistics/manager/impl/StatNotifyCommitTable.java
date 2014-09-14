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
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

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
                .getFlowTableAndStatisticsMap() == null ? new ArrayList<FlowTableAndStatisticsMap>(10)
                  : new ArrayList<FlowTableAndStatisticsMap>(notification.getFlowTableAndStatisticsMap());
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId);
        if (txContainer.isPresent()) {
            final List<? extends TransactionAware> cachedNotifs =
                    txContainer.get().getNotifications();
            for (final TransactionAware notif : cachedNotifs) {
                if (notif instanceof FlowTableStatisticsUpdate) {
                    tableStats.addAll(((FlowTableStatisticsUpdate) notif).getFlowTableAndStatisticsMap());
                }
            }
        }
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                statTableCommit(tableStats, nodeId, tx);
            }
        });
    }

    private void statTableCommit(final List<FlowTableAndStatisticsMap> tableStats, final NodeId nodeId,
            final ReadWriteTransaction trans) {
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId));
        final InstanceIdentifier<FlowCapableNode> fNodeIdent = nodeIdent.augmentation(FlowCapableNode.class);
        for (final FlowTableAndStatisticsMap tableStat : tableStats) {
            final FlowTableStatistics stats = new FlowTableStatisticsBuilder(tableStat).build();

            final KeyedInstanceIdentifier<Table, TableKey> tableIdent = fNodeIdent
                    .child(Table.class, new TableKey(tableStat.getTableId().getValue()));
            final InstanceIdentifier<FlowTableStatistics> tableStatRef = tableIdent
                    .augmentation(FlowTableStatisticsData.class).child(FlowTableStatistics.class);
            /* check flow Capable Node and write statistics */
            // FIXME : we need to add check for Table after fix InventoryManager and remove true for create parent
            final CheckedFuture<Optional<FlowCapableNode>, ReadFailedException> hashIdUpd = trans
                    .read(LogicalDatastoreType.OPERATIONAL, fNodeIdent);
            Futures.addCallback(hashIdUpd, new FutureCallback<Optional<FlowCapableNode>>() {
                @Override
                public void onSuccess(final Optional<FlowCapableNode> result) {
                    if (result.isPresent()) {
                        trans.put(LogicalDatastoreType.OPERATIONAL, tableStatRef, stats, true);
                    }
                }
                @Override
                public void onFailure(final Throwable t) {
                    //NOOP
                }
            });
        }
        manager.getStatCollector().collectNextStatistics();
    }
}

