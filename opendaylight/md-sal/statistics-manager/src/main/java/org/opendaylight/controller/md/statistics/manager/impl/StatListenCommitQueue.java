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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.QueueStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.queue.id.and.statistics.map.QueueIdAndStatisticsMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

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
    public void onQueueStatisticsUpdate(final QueueStatisticsUpdate notification) {
        final TransactionId transId = notification.getTransactionId();
        final NodeId nodeId = notification.getId();
        if ( ! isExpectedStatistics(transId, nodeId)) {
            LOG.debug("STAT-MANAGER - QueueStatisticsUpdate: unregistred notification detect TransactionId {}", transId);
            return;
        }
        if (notification.isMoreReplies()) {
            manager.getRpcMsgManager().addNotification(notification, nodeId);
            return;
        }
        final List<QueueIdAndStatisticsMap> queueStats = notification.getQueueIdAndStatisticsMap() != null
                ? new ArrayList<>(notification.getQueueIdAndStatisticsMap()) : new ArrayList<QueueIdAndStatisticsMap>(10);
        final Optional<TransactionCacheContainer<?>> txContainer = getTransactionCacheContainer(transId, nodeId);
        if (txContainer.isPresent()) {
            final List<? extends TransactionAware> cachedNotifs =
                    txContainer.get().getNotifications();
            for (final TransactionAware notif : cachedNotifs) {
                if (notif instanceof QueueStatisticsUpdate) {
                    queueStats.addAll(((QueueStatisticsUpdate) notif).getQueueIdAndStatisticsMap());
                }
            }
        }
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId));
        /* Queue statistics are small size and we are not able to change for OF cross controller
         * - don't need to make are atomic */
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction trans) {
                /* Notification for continue */
                notifyToCollectNextStatistics(nodeIdent);
                statQueueCommit(queueStats, nodeIdent, trans);
            }
        });
    }

    private void statQueueCommit(final List<QueueIdAndStatisticsMap> queueStats,
            final InstanceIdentifier<Node> nodeIdent, final ReadWriteTransaction trans) {

        /* check exist FlowCapableNode and write statistics */
        Optional<Node> fNode = Optional.absent();
        try {
            fNode = trans.read(LogicalDatastoreType.OPERATIONAL, nodeIdent).checkedGet();
        }
        catch (final ReadFailedException e) {
            LOG.debug("Read Operational/DS for Node fail! {}", nodeIdent, e);
            return;
        }
        if ( ! fNode.isPresent()) {
            LOG.trace("Read Operational/DS for Node fail! Node {} doesn't exist.", nodeIdent);
            return;
        }

        for (final QueueIdAndStatisticsMap queueEntry : queueStats) {
            final FlowCapableNodeConnectorQueueStatistics statChild =
                    new FlowCapableNodeConnectorQueueStatisticsBuilder(queueEntry).build();
            final FlowCapableNodeConnectorQueueStatisticsDataBuilder statBuild =
                    new FlowCapableNodeConnectorQueueStatisticsDataBuilder();
            statBuild.setFlowCapableNodeConnectorQueueStatistics(statChild);

            final QueueKey qKey = new QueueKey(queueEntry.getQueueId());
            QueueBuilder queueBuilder = new QueueBuilder().setKey(qKey);
            queueBuilder.addAugmentation(FlowCapableNodeConnectorQueueStatisticsData.class, statBuild.build());

            final InstanceIdentifier<Queue> queueStatIdent = nodeIdent
                    .child(NodeConnector.class, new NodeConnectorKey(queueEntry.getNodeConnectorId()))
                    .augmentation(FlowCapableNodeConnector.class)
                    .child(Queue.class, qKey);
            trans.put(LogicalDatastoreType.OPERATIONAL, queueStatIdent, queueBuilder.build());
        }
    }
}

