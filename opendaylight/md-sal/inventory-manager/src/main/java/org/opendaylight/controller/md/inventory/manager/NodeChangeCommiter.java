/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NodeChangeCommiter implements OpendaylightInventoryListener {
    private static final Logger LOG = LoggerFactory.getLogger(NodeChangeCommiter.class);

    private final FlowCapableInventoryProvider manager;

    public NodeChangeCommiter(final FlowCapableInventoryProvider manager) {
        this.manager = Preconditions.checkNotNull(manager);
    }

    @Override
    public synchronized void onNodeConnectorRemoved(final NodeConnectorRemoved connector) {
        manager.enqueue(new InventoryOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                final NodeConnectorRef ref = connector.getNodeConnectorRef();
                LOG.debug("removing node connector {} ", ref.getValue());
                tx.delete(LogicalDatastoreType.OPERATIONAL, ref.getValue());
                CheckedFuture deleteFuture = tx.submit();
                Futures.addCallback(deleteFuture, new FutureCallback() {
                    @Override
                    public void onSuccess(Object o) {
                        LOG.debug("deletion of node connector {} successful", ref.getValue());
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.error("deletion of node connector {} failed", ref.getValue(), throwable);
                    }
                });
            }
        });
    }

    @Override
    public synchronized void onNodeConnectorUpdated(final NodeConnectorUpdated connector) {
        final NodeConnectorRef ref = connector.getNodeConnectorRef();
        manager.enqueue(new InventoryOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                final NodeConnectorRef ref = connector.getNodeConnectorRef();
                final NodeConnectorBuilder data = new NodeConnectorBuilder(connector);
                data.setKey(new NodeConnectorKey(connector.getId()));

                final FlowCapableNodeConnectorUpdated flowConnector = connector
                        .getAugmentation(FlowCapableNodeConnectorUpdated.class);
                if (flowConnector != null) {
                    final FlowCapableNodeConnector augment = InventoryMapping.toInventoryAugment(flowConnector);
                    data.addAugmentation(FlowCapableNodeConnector.class, augment);
                }


                InstanceIdentifier<NodeConnector> value = (InstanceIdentifier<NodeConnector>) ref.getValue();

                LOG.debug("updating node connector : {}.", value);
                NodeConnector build = data.build();
                tx.put(LogicalDatastoreType.OPERATIONAL, value, build);
                CheckedFuture updateFuture = tx.submit();
                Futures.addCallback(updateFuture, new FutureCallback() {
                    @Override
                    public void onSuccess(Object o) {
                        LOG.debug("update of node connector {} successful", ref.getValue());
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.debug("update of node connector {} unsuccessful", ref.getValue(), throwable);
                    }
                });
            }
        });
    }

    @Override
    public synchronized void onNodeRemoved(final NodeRemoved node) {
        manager.enqueue(new InventoryOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                final NodeRef ref = node.getNodeRef();
                LOG.debug("removing node : {}", ref.getValue());
                tx.delete(LogicalDatastoreType.OPERATIONAL, ref.getValue());
                CheckedFuture deleteFuture = tx.submit();
                Futures.addCallback(deleteFuture, new FutureCallback() {
                    @Override
                    public void onSuccess(Object o) {
                        LOG.debug("successfully removed node : {}", ref.getValue());
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.error("node : {} removal unsuccessful", ref.getValue(), throwable);
                    }
                });
            }
        });
    }

    @Override
    public synchronized void onNodeUpdated(final NodeUpdated node) {
        final FlowCapableNodeUpdated flowNode = node.getAugmentation(FlowCapableNodeUpdated.class);
        if (flowNode == null) {
            return;
        }
        manager.enqueue(new InventoryOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction tx) {
                final NodeRef ref = node.getNodeRef();
                @SuppressWarnings("unchecked")
                InstanceIdentifierBuilder<Node> builder = ((InstanceIdentifier<Node>) ref.getValue()).builder();
                InstanceIdentifierBuilder<FlowCapableNode> augmentation = builder.augmentation(FlowCapableNode.class);
                final InstanceIdentifier<FlowCapableNode> path = augmentation.build();
                tx.read(LogicalDatastoreType.OPERATIONAL, path);
                CheckedFuture readFuture = tx.submit();
                Futures.addCallback(readFuture, new FutureCallback<Optional<? extends DataObject>>() {
                    @Override
                    public void onSuccess(Optional<? extends DataObject> optional) {
                        if (!optional.isPresent()) {
                            enqueuePutTable0Tx(ref);
                        }
                        enqueueWriteNodeDataTx(node, flowNode, path);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.error(String.format("Can't retrieve node data for node %s", node));
                    }
                });
            }
        });
    }

    private void enqueueWriteNodeDataTx(final NodeUpdated node, final FlowCapableNodeUpdated flowNode, final InstanceIdentifier<FlowCapableNode> path) {
        manager.enqueue(new InventoryOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                final NodeBuilder nodeBuilder = new NodeBuilder(node);
                nodeBuilder.setKey(new NodeKey(node.getId()));

                final FlowCapableNode augment = InventoryMapping.toInventoryAugment(flowNode);
                nodeBuilder.addAugmentation(FlowCapableNode.class, augment);
                LOG.debug("updating node :{} ", path);
                tx.put(LogicalDatastoreType.OPERATIONAL, path, augment);
                CheckedFuture nodeUpdateTx = tx.submit();
                Futures.addCallback(nodeUpdateTx, new FutureCallback() {
                    @Override
                    public void onSuccess(Object o) {
                        LOG.debug("Successfully written node data: {}", path);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.debug("Failed writing data for node {}.", path, throwable);
                    }
                });
            }
        });
    }

    private void enqueuePutTable0Tx(final NodeRef ref) {
        manager.enqueue(new InventoryOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction tx) {
                final TableKey tKey = new TableKey((short) 0);
                final InstanceIdentifier<Table> tableIdentifier =
                        ((InstanceIdentifier<Node>) ref.getValue()).augmentation(FlowCapableNode.class).child(Table.class, new TableKey(tKey));
                TableBuilder tableBuilder = new TableBuilder();
                Table table0 = tableBuilder.setId((short) 0).build();
                LOG.debug("writing table :{} ", tableIdentifier);
                tx.put(LogicalDatastoreType.OPERATIONAL, tableIdentifier, table0, true);
                CheckedFuture writeTableTx = tx.submit();
                Futures.addCallback(writeTableTx, new FutureCallback() {
                    @Override
                    public void onSuccess(Object o) {
                        LOG.debug("Successfully written table: {}", tableIdentifier);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.debug("Failed writing data for table {}.", tableIdentifier, throwable);
                    }
                });
            }
        });
    }
}
