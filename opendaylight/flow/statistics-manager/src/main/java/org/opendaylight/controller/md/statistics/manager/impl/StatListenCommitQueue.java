/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import com.google.common.base.Preconditions;

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
        manager.getRpcMsgManager().addNotification(notification, nodeId);
        if (notification.isMoreReplies()) {
            return;
        }

        /* Don't block RPC Notification thread */
        manager.enqueue(new StatDataStoreOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {

                final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class)
                        .child(Node.class, new NodeKey(nodeId));

                /* Validate exist Node */
                Optional<Node> fNode = Optional.absent();
                try {
                    fNode = tx.read(LogicalDatastoreType.OPERATIONAL, nodeIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.debug("Read Operational/DS for Node fail! {}", nodeIdent, e);
                }
                if ( ! fNode.isPresent()) {
                    LOG.trace("Read Operational/DS for Node fail! Node {} doesn't exist.", nodeIdent);
                    return;
                }

                /* Get and Validate TransactionCacheContainer */
                final Optional<TransactionCacheContainer<?>> txContainer = getTransactionCacheContainer(transId, nodeId);
                if ( ! isTransactionCacheContainerValid(txContainer)) {
                    return;
                }
                /* Prepare List actual Queues and not updated Queues will be removed */
                final List<NodeConnector> existConnectors = fNode.get().getNodeConnector() != null
                        ? fNode.get().getNodeConnector() : Collections.<NodeConnector> emptyList();
                final Map<QueueKey, NodeConnectorKey> existQueueKeys = new HashMap<>();
                for (final NodeConnector connect : existConnectors) {
                    final List<Queue> listQueues = connect.getAugmentation(FlowCapableNodeConnector.class).getQueue();
                    if (listQueues != null) {
                        for (final Queue queue : listQueues) {
                            existQueueKeys.put(queue.getKey(), connect.getKey());
                        }
                    }
                }
                /* Queue processing */
                statQueueCommit(txContainer, tx, nodeIdent, existQueueKeys);
                /* Delete all not presented Group Nodes */
                deleteAllNotPresentedNodes(nodeIdent, tx, Collections.unmodifiableMap(existQueueKeys));
                /* Notification for continue collecting statistics */
                notifyToCollectNextStatistics(nodeIdent, transId);
            }
        });
    }

    private void statQueueCommit(
            final Optional<TransactionCacheContainer<?>> txContainer, final ReadWriteTransaction tx,
            final InstanceIdentifier<Node> nodeIdent, final Map<QueueKey, NodeConnectorKey> existQueueKeys) {

        Preconditions.checkNotNull(existQueueKeys);
        Preconditions.checkNotNull(txContainer);
        Preconditions.checkNotNull(nodeIdent);
        Preconditions.checkNotNull(tx);

        final List<? extends TransactionAware> cacheNotifs = txContainer.get().getNotifications();
        for (final TransactionAware notif : cacheNotifs) {
            if ( ! (notif instanceof QueueStatisticsUpdate)) {
                break;
            }
            final List<QueueIdAndStatisticsMap> queueStats = ((QueueStatisticsUpdate) notif).getQueueIdAndStatisticsMap();
            if (queueStats == null) {
                break;
            }
            for (final QueueIdAndStatisticsMap queueStat : queueStats) {
                if (queueStat.getQueueId() != null) {
                    final FlowCapableNodeConnectorQueueStatistics statChild =
                            new FlowCapableNodeConnectorQueueStatisticsBuilder(queueStat).build();
                    final FlowCapableNodeConnectorQueueStatisticsDataBuilder statBuild =
                            new FlowCapableNodeConnectorQueueStatisticsDataBuilder();
                    statBuild.setFlowCapableNodeConnectorQueueStatistics(statChild);
                    final QueueKey qKey = new QueueKey(queueStat.getQueueId());
                    final InstanceIdentifier<Queue> queueIdent = nodeIdent
                            .child(NodeConnector.class, new NodeConnectorKey(queueStat.getNodeConnectorId()))
                            .augmentation(FlowCapableNodeConnector.class)
                            .child(Queue.class, qKey);
                    final InstanceIdentifier<FlowCapableNodeConnectorQueueStatisticsData> queueStatIdent = queueIdent.augmentation(FlowCapableNodeConnectorQueueStatisticsData.class);
                    existQueueKeys.remove(qKey);
                    tx.merge(LogicalDatastoreType.OPERATIONAL, queueIdent, new QueueBuilder().setKey(qKey).build());
                    tx.put(LogicalDatastoreType.OPERATIONAL, queueStatIdent, statBuild.build());
                }
            }
        }
    }

    private void deleteAllNotPresentedNodes(final InstanceIdentifier<Node> nodeIdent,
            final ReadWriteTransaction tx, final Map<QueueKey, NodeConnectorKey> existQueueKeys) {

        Preconditions.checkNotNull(nodeIdent);
        Preconditions.checkNotNull(tx);

        if (existQueueKeys == null) {
            return;
        }

        for (final Entry<QueueKey, NodeConnectorKey> entry : existQueueKeys.entrySet()) {
            final InstanceIdentifier<Queue> queueIdent = nodeIdent.child(NodeConnector.class, entry.getValue())
                    .augmentation(FlowCapableNodeConnector.class).child(Queue.class, entry.getKey());
            LOG.trace("Queue {} has to removed.", queueIdent);
            Optional<Queue> delQueue = Optional.absent();
            try {
                delQueue = tx.read(LogicalDatastoreType.OPERATIONAL, queueIdent).checkedGet();
            }
            catch (final ReadFailedException e) {
                // NOOP - probably another transaction delete that node
            }
            if (delQueue.isPresent()) {
                tx.delete(LogicalDatastoreType.OPERATIONAL, queueIdent);
            }
        }
    }
}

