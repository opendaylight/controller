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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.statistics.manager.StatRpcMsgManager.TransactionCacheContainer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager.StatDataStoreOperation;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.QueueStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.queue.id.and.statistics.map.QueueIdAndStatisticsMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
 * StatNotifyCommitQueue
 * Class is a NotifyListner for Queues Statistics
 * All expected (registered) queueStatistics will be builded and
 * commit to Operational/DataStore
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class StatListenCommitQueue extends StatAbstractListenCommit<Queue, OpendaylightQueueStatisticsListener>
                                        implements OpendaylightQueueStatisticsListener {

    private final static Logger LOG = LoggerFactory.getLogger(StatListenCommitQueue.class);

    public StatListenCommitQueue(final StatisticsManager manager, final DataBroker db,
            final NotificationProviderService nps) {
        super(manager, db, nps, Queue.class);
    }

    @Override
    protected OpendaylightQueueStatisticsListener getStatNotificationListener() {
        return this;
    }

    @Override
    protected InstanceIdentifier<Queue> getWildCardedRegistrationPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).child(NodeConnector.class)
            .augmentation(FlowCapableNodeConnector.class).child(Queue.class);
    }

    @Override
    public void createStat(final InstanceIdentifier<Queue> keyIdent, final Queue data,
            final InstanceIdentifier<Node> nodeIdent) {
        final NodeConnectorKey key = keyIdent.firstKeyOf(NodeConnector.class, NodeConnectorKey.class);
        manager.getRpcMsgManager().getQueueStatForGivenPort(new NodeRef(nodeIdent),
                key.getId(), data.getQueueId());
    }

    @Override
    public void removeStat(final InstanceIdentifier<Queue> keyIdent) {
        final InstanceIdentifier<FlowCapableNodeConnectorQueueStatisticsData> del = keyIdent
                .augmentation(FlowCapableNodeConnectorQueueStatisticsData.class);

        manager.enqueue(new StatDataStoreOperation() {

            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                final CheckedFuture<Optional<FlowCapableNodeConnectorQueueStatisticsData>, ReadFailedException> future = tx
                        .read(LogicalDatastoreType.OPERATIONAL, del);
                Futures.addCallback(future, new FutureCallback<Optional<FlowCapableNodeConnectorQueueStatisticsData>>() {
                    @Override
                    public void onSuccess(final Optional<FlowCapableNodeConnectorQueueStatisticsData> result) {
                        if (result.isPresent()) {
                            tx.delete(LogicalDatastoreType.OPERATIONAL, del);
                        }
                    }
                    @Override
                    public void onFailure(final Throwable t) {
                        //NOOP
                    }
                });
            }
        });
    }

    @Override
    public void onQueueStatisticsUpdate(final QueueStatisticsUpdate notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! isExpectedStatistics(transId)) {
            LOG.debug("STAT-MANAGER - QueueStatisticsUpdate: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<QueueIdAndStatisticsMap> queueStats = notification
                .getQueueIdAndStatisticsMap() == null ? new ArrayList<QueueIdAndStatisticsMap>(10)
                 : new ArrayList<QueueIdAndStatisticsMap>(notification.getQueueIdAndStatisticsMap());
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getRpcMsgManager().getTransactionCacheContainer(transId);
        if (txContainer.isPresent()) {
            final List<? extends TransactionAware> cachedNotifs =
                    txContainer.get().getNotifications();
            for (final TransactionAware notif : cachedNotifs) {
                if (notif instanceof QueueStatisticsUpdate) {
                    queueStats.addAll(((QueueStatisticsUpdate) notif).getQueueIdAndStatisticsMap());
                }
            }
        }
        manager.enqueue(new StatDataStoreOperation() {

            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                statQueueCommit(queueStats, nodeId, tx);
            }
        });
    }

    private void statQueueCommit(final List<QueueIdAndStatisticsMap> queueStats, final NodeId nodeId,
            final ReadWriteTransaction trans) {
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId));

        for (final QueueIdAndStatisticsMap queueEntry : queueStats) {
            final FlowCapableNodeConnectorQueueStatistics stats =
                    new FlowCapableNodeConnectorQueueStatisticsBuilder(queueEntry).build();

            final QueueKey qKey = new QueueKey(queueEntry.getQueueId());
            final InstanceIdentifier<FlowCapableNodeConnector> flowCapNodeConnIdent = nodeIdent
                    .child(NodeConnector.class, new NodeConnectorKey(queueEntry.getNodeConnectorId()))
                    .augmentation(FlowCapableNodeConnector.class);
            final InstanceIdentifier<FlowCapableNodeConnectorQueueStatistics> queueStatIdent = flowCapNodeConnIdent
                    .child(Queue.class, qKey).augmentation(FlowCapableNodeConnectorQueueStatisticsData.class)
                    .child(FlowCapableNodeConnectorQueueStatistics.class);
            /* check exist FlowCapableNode and write statistics probable with parent */
            // FIXME : we need to add check for flowCapNodeConnIdent after fix InventoryManager and remove true for create parent
            final CheckedFuture<Optional<Node>, ReadFailedException> hashIdUpd = trans
                    .read(LogicalDatastoreType.OPERATIONAL, nodeIdent);
            Futures.addCallback(hashIdUpd, new FutureCallback<Optional<Node>>() {
                @Override
                public void onSuccess(final Optional<Node> result) {
                    if (result.isPresent()) {
                        trans.put(LogicalDatastoreType.OPERATIONAL, queueStatIdent, stats, true);
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

