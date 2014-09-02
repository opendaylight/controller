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
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.statistics.manager.StatDeviceMsgManager.TransactionCacheContainer;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.QueueStatisticsUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.queue.id.and.statistics.map.QueueIdAndStatisticsMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
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
    public void createStat(final InstanceIdentifier<Queue> keyIdent, final Queue data,
            final InstanceIdentifier<FlowCapableNode> nodeIdent) {
        final NodeConnectorKey key = keyIdent.firstKeyOf(NodeConnector.class, NodeConnectorKey.class);
        manager.getDeviceMsgManager().getQueueStatForGivenPort(new NodeRef(nodeIdent),
                key.getId(), data.getQueueId());
    }

    @Override
    public void removeStat(final InstanceIdentifier<Queue> keyIdent) {
        // TODO probably don't need
        final InstanceIdentifier<FlowCapableNodeConnectorQueueStatisticsData> del = keyIdent
                .augmentation(FlowCapableNodeConnectorQueueStatisticsData.class);
        final WriteTransaction trans = manager.getWriteTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, del);
        trans.submit();
    }

    @Override
    public void onQueueStatisticsUpdate(final QueueStatisticsUpdate notification) {
        final TransactionId transId = notification.getTransactionId();
        if ( ! manager.getDeviceMsgManager().isExpectedStatistics(transId)) {
//            return;
            LOG.warn("STAT-MANAGER - QueueStatisticsUpdate: unregistred notification detect TransactionId {}", transId);
        }
        if (notification.isMoreReplies()) {
            manager.getDeviceMsgManager().addNotification(notification);
            return;
        }
        final NodeId nodeId = notification.getId();
        final List<QueueIdAndStatisticsMap> queueStats = notification
                .getQueueIdAndStatisticsMap() == null ? new ArrayList<QueueIdAndStatisticsMap>(10)
                 : new ArrayList<QueueIdAndStatisticsMap>(notification.getQueueIdAndStatisticsMap());
        final Optional<TransactionCacheContainer<?>> txContainer =
                manager.getDeviceMsgManager().getTransactionCacheContainer(transId);
        if (txContainer.isPresent()) {
            final List<? extends TransactionAware> cachedNotifs =
                    txContainer.get().getNotifications();
            for (final TransactionAware notif : cachedNotifs) {
                if (notif instanceof QueueStatisticsUpdate) {
                    queueStats.addAll(((QueueStatisticsUpdate) notif).getQueueIdAndStatisticsMap());
                }
            }
        }
        statQueueCommit(queueStats, nodeId);
    }

    private void statQueueCommit(final List<QueueIdAndStatisticsMap> queueStats, final NodeId nodeId) {
        final WriteTransaction trans = manager.getWriteTransaction();
        final InstanceIdentifier<Node> nodeIdent = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(nodeId));

        for (final QueueIdAndStatisticsMap queueEntry : queueStats) {
            final FlowCapableNodeConnectorQueueStatisticsDataBuilder queueStatisticsDataBuilder =
                    new FlowCapableNodeConnectorQueueStatisticsDataBuilder();
            final FlowCapableNodeConnectorQueueStatisticsBuilder queueStatisticsBuilder =
                    new FlowCapableNodeConnectorQueueStatisticsBuilder(queueEntry);

            queueStatisticsDataBuilder.setFlowCapableNodeConnectorQueueStatistics(queueStatisticsBuilder.build());

            final QueueKey qKey = new QueueKey(queueEntry.getQueueId());
            final KeyedInstanceIdentifier<Queue, QueueKey> queueIdent = nodeIdent
                    .child(NodeConnector.class, new NodeConnectorKey(queueEntry.getNodeConnectorId()))
                    .augmentation(FlowCapableNodeConnector.class)
                    .child(Queue.class, qKey);

            final QueueBuilder queueBuilder = new QueueBuilder();
            final FlowCapableNodeConnectorQueueStatisticsData qsd = queueStatisticsDataBuilder.build();
            queueBuilder.addAugmentation(FlowCapableNodeConnectorQueueStatisticsData.class, qsd);
            queueBuilder.setKey(qKey);
            trans.merge(LogicalDatastoreType.OPERATIONAL, queueIdent, queueBuilder.build(), true);
        }
        continueStatCollecting(trans.submit());
    }
}

