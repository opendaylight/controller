/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class QueueStatsTracker extends AbstractListeningStatsTracker<QueueIdAndStatisticsMap, QueueStatsEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(QueueStatsTracker.class);
    private final OpendaylightQueueStatisticsService queueStatsService;

    QueueStatsTracker(OpendaylightQueueStatisticsService queueStatsService, final FlowCapableContext context) {
        super(context);
        this.queueStatsService = queueStatsService;
    }

    @Override
    protected void cleanupSingleStat(ReadWriteTransaction trans, QueueStatsEntry item) {
        InstanceIdentifier<?> queueRef = getNodeIdentifier()
                .child(NodeConnector.class, new NodeConnectorKey(item.getNodeConnectorId()))
                .augmentation(FlowCapableNodeConnector.class)
                .child(Queue.class, new QueueKey(item.getQueueId()))
                .augmentation(FlowCapableNodeConnectorQueueStatisticsData.class);
        trans.delete(LogicalDatastoreType.OPERATIONAL, queueRef);
    }

    @Override
    protected QueueStatsEntry updateSingleStat(ReadWriteTransaction trans, QueueIdAndStatisticsMap item) {

        QueueStatsEntry queueEntry = new QueueStatsEntry(item.getNodeConnectorId(), item.getQueueId());

        FlowCapableNodeConnectorQueueStatisticsDataBuilder queueStatisticsDataBuilder = new FlowCapableNodeConnectorQueueStatisticsDataBuilder();

        FlowCapableNodeConnectorQueueStatisticsBuilder queueStatisticsBuilder = new FlowCapableNodeConnectorQueueStatisticsBuilder();

        queueStatisticsBuilder.fieldsFrom(item);

        queueStatisticsDataBuilder.setFlowCapableNodeConnectorQueueStatistics(queueStatisticsBuilder.build());

        InstanceIdentifier<Queue> queueRef = getNodeIdentifier()
                .child(NodeConnector.class, new NodeConnectorKey(item.getNodeConnectorId()))
                .augmentation(FlowCapableNodeConnector.class)
                .child(Queue.class, new QueueKey(item.getQueueId()));

        QueueBuilder queueBuilder = new QueueBuilder();
        FlowCapableNodeConnectorQueueStatisticsData qsd = queueStatisticsDataBuilder.build();
        queueBuilder.addAugmentation(FlowCapableNodeConnectorQueueStatisticsData.class, qsd);
        queueBuilder.setKey(new QueueKey(item.getQueueId()));

        LOG.debug("Augmenting queue statistics {} of queue {} to port {}",
                                    qsd,
                                    item.getQueueId(),
                                    item.getNodeConnectorId());

        trans.merge(LogicalDatastoreType.OPERATIONAL, queueRef, queueBuilder.build(), true);
        return queueEntry;
    }

    @Override
    public void request() {
        if (queueStatsService != null) {
            GetAllQueuesStatisticsFromAllPortsInputBuilder input = new GetAllQueuesStatisticsFromAllPortsInputBuilder();
            input.setNode(getNodeRef());

            requestHelper(queueStatsService.getAllQueuesStatisticsFromAllPorts(input.build()));
        }
    }

    public void request(NodeConnectorId nodeConnectorId, QueueId queueId) {
        if (queueStatsService != null) {
            GetQueueStatisticsFromGivenPortInputBuilder input = new GetQueueStatisticsFromGivenPortInputBuilder();

            input.setNode(getNodeRef());
            input.setNodeConnectorId(nodeConnectorId);
            input.setQueueId(queueId);

            requestHelper(queueStatsService.getQueueStatisticsFromGivenPort(input.build()));
        }
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (Entry<InstanceIdentifier<?>, DataObject> e : change.getCreatedData().entrySet()) {
            if (Queue.class.equals(e.getKey().getTargetType())) {
                final Queue queue = (Queue) e.getValue();
                final NodeConnectorKey key = e.getKey().firstKeyOf(NodeConnector.class, NodeConnectorKey.class);
                LOG.debug("Key {} triggered request for connector {} queue {}", key.getId(), queue.getQueueId());
                request(key.getId(), queue.getQueueId());
            } else {
                LOG.debug("Ignoring key {}", e.getKey());
            }
        }

        final ReadWriteTransaction trans = startTransaction();
        for (InstanceIdentifier<?> key : change.getRemovedPaths()) {
            if (Queue.class.equals(key.getTargetType())) {
                @SuppressWarnings("unchecked")
                final InstanceIdentifier<Queue> queue = (InstanceIdentifier<Queue>)key;
                final InstanceIdentifier<?> del = queue
                        .augmentation(FlowCapableNodeConnectorQueueStatisticsData.class);
                LOG.debug("Key {} triggered remove of augmentation {}", key, del);

                trans.delete(LogicalDatastoreType.OPERATIONAL, del);
            }
        }
        trans.submit();
    }

    @Override
    protected InstanceIdentifier<?> listenPath() {
        return getNodeIdentifier().child(NodeConnector.class)
                .augmentation(FlowCapableNodeConnector.class).child(Queue.class);
    }

    @Override
    protected String statName() {
        return "Queue";
    }

    @Override
    public void start(final DataBroker dbs) {
        if (queueStatsService == null) {
            LOG.debug("No Queue Statistics service, not subscribing to queues on node {}", getNodeIdentifier());
            return;
        }

        super.start(dbs);
    }
}
