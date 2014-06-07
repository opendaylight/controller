/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.manager;

import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.getNodeConnectorKey;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.getNodeKey;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTerminationPoint;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTerminationPointId;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTopologyLink;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTopologyNode;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTopologyNodeId;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscovered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkOverutilized;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkUtilizationNormal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

class FlowCapableTopologyExporter implements FlowTopologyDiscoveryListener, OpendaylightInventoryListener {
    public static final TopologyKey TOPOLOGY = new TopologyKey(new TopologyId("flow:1"));

    private static final Logger LOG = LoggerFactory.getLogger(FlowCapableTopologyExporter.class);
    private static final InstanceIdentifier<Topology> TOPOLOGY_PATH =
            InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, TOPOLOGY).build();
    private static final int MAX_TRANSACTION_OPERATIONS = 100;
    private static final int OPERATION_QUEUE_DEPTH = 500;

    /**
     * Internal interface for submitted tasks. Implementations of this interface are
     * enqueued and batched into data store transactions.
     */
    private interface TopologyTask {
        /**
         * Execute the task on top of the transaction.
         *
         * @param transaction Datastore transaction
         */
        void runTask(DataModificationTransaction transaction);
    }

    private final BlockingQueue<TopologyTask> operations = new LinkedBlockingQueue<>(OPERATION_QUEUE_DEPTH);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
            .setNameFormat("FlowCapableTopologyExporter-" + TOPOLOGY.getTopologyId().getValue())
            .build());
    private final Callable<Void> committer = new Callable<Void>() {
        @Override
        public Void call() throws InterruptedException {
            TopologyTask op = operations.take();

            LOG.debug("New operations available, starting transaction");
            final DataModificationTransaction tx = dataService.beginTransaction();

            int ops = 0;
            do {
                op.runTask(tx);

                ops++;
                if (ops < MAX_TRANSACTION_OPERATIONS) {
                    op = operations.poll();
                } else {
                    op = null;
                }
            } while (op != null);

            LOG.debug("Processed {} operations, submitting transaction", ops);
            listenOnTransactionState(tx.getIdentifier(), tx.commit());
            return null;
        }
    };

    // FIXME: Flow capable topology exporter should use transaction chaining API
    private DataProviderService dataService;

    public DataProviderService getDataService() {
        return dataService;
    }

    public void setDataService(final DataProviderService dataService) {
        this.dataService = dataService;
    }

    public void start() {
        TopologyBuilder tb = new TopologyBuilder().setKey(TOPOLOGY);
        DataModificationTransaction tx = dataService.beginTransaction();
        tx.putOperationalData(TOPOLOGY_PATH, tb.build());
        listenOnTransactionState(tx.getIdentifier(),tx.commit());
    }

    private void enqueueOperation(final TopologyTask task) {
        try {
            operations.put(task);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while submitting task {}", task, e);
        }
    }

    @Override
    public void onNodeRemoved(final NodeRemoved notification) {
        enqueueOperation(new TopologyTask() {
            @Override
            public void runTask(final DataModificationTransaction transaction) {
                NodeId nodeId = toTopologyNodeId(getNodeKey(notification.getNodeRef()).getId());
                InstanceIdentifier<Node> nodeInstance = toNodeIdentifier(notification.getNodeRef());
                transaction.removeOperationalData(nodeInstance);
                removeAffectedLinks(transaction, nodeId);
            }
        });
    }

    @Override
    public void onNodeUpdated(final NodeUpdated notification) {
        FlowCapableNodeUpdated fcnu = notification.getAugmentation(FlowCapableNodeUpdated.class);
        if (fcnu != null) {
            enqueueOperation(new TopologyTask() {
                @Override
                public void runTask(final DataModificationTransaction transaction) {
                    Node node = toTopologyNode(toTopologyNodeId(notification.getId()), notification.getNodeRef());
                    InstanceIdentifier<Node> path = getNodePath(toTopologyNodeId(notification.getId()));
                    transaction.putOperationalData(path, node);
                }
            });
        }
    }

    @Override
    public void onNodeConnectorRemoved(final NodeConnectorRemoved notification) {
        enqueueOperation(new TopologyTask() {
            @Override
            public void runTask(final DataModificationTransaction transaction) {
                InstanceIdentifier<TerminationPoint> tpInstance = toTerminationPointIdentifier(notification
                        .getNodeConnectorRef());
                TpId tpId = toTerminationPointId(getNodeConnectorKey(notification.getNodeConnectorRef()).getId());

                transaction.removeOperationalData(tpInstance);
                removeAffectedLinks(transaction, tpId);
            }
        });
    }

    @Override
    public void onNodeConnectorUpdated(final NodeConnectorUpdated notification) {
        final FlowCapableNodeConnectorUpdated fcncu = notification.getAugmentation(FlowCapableNodeConnectorUpdated.class);
        if (fcncu != null) {
            enqueueOperation(new TopologyTask() {
                @Override
                public void runTask(final DataModificationTransaction transaction) {
                    NodeId nodeId = toTopologyNodeId(getNodeKey(notification.getNodeConnectorRef()).getId());
                    TerminationPoint point = toTerminationPoint(toTerminationPointId(notification.getId()),
                            notification.getNodeConnectorRef());
                    InstanceIdentifier<TerminationPoint> path = tpPath(nodeId, point.getKey().getTpId());

                    transaction.putOperationalData(path, point);
                    if ((fcncu.getState() != null && fcncu.getState().isLinkDown())
                            || (fcncu.getConfiguration() != null && fcncu.getConfiguration().isPORTDOWN())) {
                        removeAffectedLinks(transaction, point.getTpId());
                    }
                }
            });
        }
    }

    @Override
    public void onLinkDiscovered(final LinkDiscovered notification) {
        enqueueOperation(new TopologyTask() {
            @Override
            public void runTask(final DataModificationTransaction transaction) {
                Link link = toTopologyLink(notification);
                InstanceIdentifier<Link> path = linkPath(link);
                transaction.putOperationalData(path, link);
            }
        });
    }

    @Override
    public void onLinkOverutilized(final LinkOverutilized notification) {
        // NOOP
    }

    @Override
    public void onLinkRemoved(final LinkRemoved notification) {
        enqueueOperation(new TopologyTask() {
            @Override
            public void runTask(final DataModificationTransaction transaction) {
                transaction.removeOperationalData(linkPath(toTopologyLink(notification)));
            }
        });
    }

    @Override
    public void onLinkUtilizationNormal(final LinkUtilizationNormal notification) {
        // NOOP
    }

    private static InstanceIdentifier<Node> toNodeIdentifier(final NodeRef ref) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey invNodeKey = getNodeKey(ref);
        NodeKey nodeKey = new NodeKey(toTopologyNodeId(invNodeKey.getId()));
        return TOPOLOGY_PATH.child(Node.class, nodeKey);
    }

    private static InstanceIdentifier<TerminationPoint> toTerminationPointIdentifier(final NodeConnectorRef ref) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey invNodeKey = getNodeKey(ref);
        NodeConnectorKey invNodeConnectorKey = getNodeConnectorKey(ref);
        return tpPath(toTopologyNodeId(invNodeKey.getId()), toTerminationPointId(invNodeConnectorKey.getId()));
    }

    private void removeAffectedLinks(final DataModificationTransaction transaction, final NodeId id) {
        TypeSafeDataReader reader = TypeSafeDataReader.forReader(transaction);
        Topology topologyData = reader.readOperationalData(TOPOLOGY_PATH);
        if (topologyData != null) {
            for (Link link : topologyData.getLink()) {
                if (id.equals(link.getSource().getSourceNode()) || id.equals(link.getDestination().getDestNode())) {
                    transaction.removeOperationalData(linkPath(link));
                }
            }
        }
    }

    private void removeAffectedLinks(final DataModificationTransaction transaction, final TpId id) {
        TypeSafeDataReader reader = TypeSafeDataReader.forReader(transaction);
        Topology topologyData = reader.readOperationalData(TOPOLOGY_PATH);
        if (topologyData != null) {
            for (Link link : topologyData.getLink()) {
                if (id.equals(link.getSource().getSourceTp()) || id.equals(link.getDestination().getDestTp())) {
                    transaction.removeOperationalData(linkPath(link));
                }
            }
        }
    }

    private static InstanceIdentifier<Node> getNodePath(final NodeId nodeId) {
        return TOPOLOGY_PATH.child(Node.class, new NodeKey(nodeId));
    }

    private static InstanceIdentifier<TerminationPoint> tpPath(final NodeId nodeId, final TpId tpId) {
        NodeKey nodeKey = new NodeKey(nodeId);
        TerminationPointKey tpKey = new TerminationPointKey(tpId);
        return TOPOLOGY_PATH.child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);
    }

    private static InstanceIdentifier<Link> linkPath(final Link link) {
        return TOPOLOGY_PATH.child(Link.class, link.getKey());
    }

    /**
     * @param txId transaction identificator
     * @param future transaction result
     */
    private void listenOnTransactionState(final Object txId, final Future<RpcResult<TransactionStatus>> future) {
        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(future),new FutureCallback<RpcResult<TransactionStatus>>() {
            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Topology export failed for Tx:{}", txId, t);
                executor.submit(committer);
            }

            @Override
            public void onSuccess(final RpcResult<TransactionStatus> result) {
                if(!result.isSuccessful()) {
                    LOG.error("Topology export failed for Tx:{}", txId);
                }
                executor.submit(committer);
            }
        });
    }
}
