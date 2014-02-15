/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.queue.rev130925.QueueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetQueueStatisticsFromGivenPortInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.queue.id.and.statistics.map.QueueIdAndStatisticsMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;

final class QueueStatsTracker extends AbstractStatsTracker<QueueIdAndStatisticsMap, QueueStatsEntry> {
    private static final Logger logger = LoggerFactory.getLogger(QueueStatsTracker.class);
    private final OpendaylightQueueStatisticsService queueStatsService;

    QueueStatsTracker(OpendaylightQueueStatisticsService queueStatsService, final FlowCapableContext context, long lifetimeNanos) {
        super(context, lifetimeNanos);
        this.queueStatsService = Preconditions.checkNotNull(queueStatsService);
    }

    @Override
    protected void cleanupSingleStat(DataModificationTransaction trans, QueueStatsEntry item) {
        InstanceIdentifier<?> queueRef
                            = getNodeIdentifierBuilder().child(NodeConnector.class, new NodeConnectorKey(item.getNodeConnectorId()))
                                                .augmentation(FlowCapableNodeConnector.class)
                                                .child(Queue.class, new QueueKey(item.getQueueId()))
                                                .augmentation(FlowCapableNodeConnectorQueueStatisticsData.class).build();
        trans.removeOperationalData(queueRef);
    }

    @Override
    protected QueueStatsEntry updateSingleStat(DataModificationTransaction trans, QueueIdAndStatisticsMap item) {

        QueueStatsEntry queueEntry = new QueueStatsEntry(item.getNodeConnectorId(), item.getQueueId());

        FlowCapableNodeConnectorQueueStatisticsDataBuilder queueStatisticsDataBuilder = new FlowCapableNodeConnectorQueueStatisticsDataBuilder();

        FlowCapableNodeConnectorQueueStatisticsBuilder queueStatisticsBuilder = new FlowCapableNodeConnectorQueueStatisticsBuilder();

        queueStatisticsBuilder.fieldsFrom(item);

        queueStatisticsDataBuilder.setFlowCapableNodeConnectorQueueStatistics(queueStatisticsBuilder.build());

        InstanceIdentifier<Queue> queueRef = getNodeIdentifierBuilder().child(NodeConnector.class, new NodeConnectorKey(item.getNodeConnectorId()))
                                    .augmentation(FlowCapableNodeConnector.class)
                                    .child(Queue.class, new QueueKey(item.getQueueId())).toInstance();

        QueueBuilder queueBuilder = new QueueBuilder();
        FlowCapableNodeConnectorQueueStatisticsData qsd = queueStatisticsDataBuilder.build();
        queueBuilder.addAugmentation(FlowCapableNodeConnectorQueueStatisticsData.class, qsd);
        queueBuilder.setKey(new QueueKey(item.getQueueId()));

        logger.debug("Augmenting queue statistics {} of queue {} to port {}",
                                    qsd,
                                    item.getQueueId(),
                                    item.getNodeConnectorId());

        trans.putOperationalData(queueRef, queueBuilder.build());
        return queueEntry;
    }

    public ListenableFuture<TransactionId> request() {
        GetAllQueuesStatisticsFromAllPortsInputBuilder input = new GetAllQueuesStatisticsFromAllPortsInputBuilder();
        input.setNode(getNodeRef());

        return requestHelper(queueStatsService.getAllQueuesStatisticsFromAllPorts(input.build()));
    }

    public ListenableFuture<TransactionId> request(NodeConnectorId nodeConnectorId, QueueId queueId) {
        GetQueueStatisticsFromGivenPortInputBuilder input = new GetQueueStatisticsFromGivenPortInputBuilder();

        input.setNode(getNodeRef());
        input.setNodeConnectorId(nodeConnectorId);
        input.setQueueId(queueId);

        return requestHelper(queueStatsService.getQueueStatisticsFromGivenPort(input.build()));
    }
}
