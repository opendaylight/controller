/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

class NodeChangeCommiter implements OpendaylightInventoryListener {
    private static final Logger LOG = LoggerFactory.getLogger(NodeChangeCommiter.class);

    private final FlowCapableInventoryProvider manager;

    public NodeChangeCommiter(final FlowCapableInventoryProvider manager) {
        this.manager = Preconditions.checkNotNull(manager);
    }

    @Override
    public synchronized void onNodeConnectorRemoved(final NodeConnectorRemoved connector) {
        LOG.debug("Node connector removed notification received.");
        manager.enqueue(new InventoryOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                final NodeConnectorRef ref = connector.getNodeConnectorRef();
                LOG.debug("removing node connector {} ", ref.getValue());
                tx.delete(LogicalDatastoreType.OPERATIONAL, ref.getValue());
            }
        });
    }

    @Override
    public synchronized void onNodeConnectorUpdated(final NodeConnectorUpdated connector) {
        LOG.debug("Node connector updated notification received.");
        manager.enqueue(new InventoryOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                final NodeConnectorRef ref = connector.getNodeConnectorRef();
                final NodeConnectorBuilder data = new NodeConnectorBuilder(connector);
                data.setKey(new NodeConnectorKey(connector.getId()));

                final FlowCapableNodeConnectorUpdated flowConnector = connector
                        .getAugmentation(FlowCapableNodeConnectorUpdated.class);
                if (flowConnector != null) {
                    final FlowCapableNodeConnector augment =
                            InventoryMapping.toInventoryAugment(flowConnector);
                    data.addAugmentation(FlowCapableNodeConnector.class, augment);
                }
                final InstanceIdentifier<NodeConnector> nConnectIdent =
                        ref.getValue().firstIdentifierOf(NodeConnector.class);
                LOG.debug("updating node connector : {}.", nConnectIdent);
                final NodeConnector build = data.build();
                tx.put(LogicalDatastoreType.OPERATIONAL, nConnectIdent, build, true);
            }
        });
    }

    @Override
    public synchronized void onNodeRemoved(final NodeRemoved node) {
        LOG.debug("Node removed notification received.");
        manager.enqueue(new InventoryOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                final NodeRef ref = node.getNodeRef();
                LOG.debug("removing node : {}", ref.getValue());
                tx.delete(LogicalDatastoreType.OPERATIONAL, ref.getValue());
            }
        });
    }

    @Override
    public synchronized void onNodeUpdated(final NodeUpdated node) {
        final FlowCapableNodeUpdated flowNode = node.getAugmentation(FlowCapableNodeUpdated.class);
        if (flowNode == null) {
            return;
        }
        final NodeRef ref = node.getNodeRef();
        final InstanceIdentifier<Node> nodeIdent = ref.getValue().firstIdentifierOf(Node.class);
        final InstanceIdentifier<FlowCapableNode> flowNodeIdent = nodeIdent.augmentation(FlowCapableNode.class);

        if (flowNode.getSwitchFeatures() != null
                && flowNode.getSwitchFeatures().getMaxTables() != null) {
            createFlowCapableNode(flowNodeIdent, flowNode);
        } else {
            updateFlowCapableNode(flowNodeIdent, flowNode);
        }
    }

    private void createTables(final Short maxTables, final InstanceIdentifier<FlowCapableNode> fNodeIdent,
            final ReadWriteTransaction tx) {
        for (short tId = 0; tId < maxTables.shortValue(); tId++) {
            final TableKey tKey = new TableKey(tId);
            final InstanceIdentifier<Table> tableIdent = fNodeIdent.child(Table.class, tKey);
            final TableBuilder tableBuild = new TableBuilder();
            tableBuild.setId(tId);
            tableBuild.setKey(tKey);
            tx.put(LogicalDatastoreType.OPERATIONAL, tableIdent, tableBuild.build());
        }
    }

    private void updateFlowCapableNode(final InstanceIdentifier<FlowCapableNode> flowNodeIdent,
            final FlowCapableNodeUpdated flowNode) {
        manager.enqueue(new InventoryOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                Optional<FlowCapableNode> fNodeIdentExist;
                try {
                    fNodeIdentExist = tx.read(LogicalDatastoreType.OPERATIONAL, flowNodeIdent).checkedGet();
                }
                catch (final ReadFailedException e) {
                    LOG.warn("Update FlowCapableNode {} fail!", flowNodeIdent, e);
                    return;
                }
                if ( ! fNodeIdentExist.isPresent()) {
                    LOG.warn("Update FlowCapableNode {} fail, Node is not presented!", flowNodeIdent);
                    return;
                }
                LOG.debug("updating FlowCapableNode :{} ", flowNodeIdent);
                final FlowCapableNode fNode = new FlowCapableNodeBuilder(flowNode).build();
                tx.merge(LogicalDatastoreType.OPERATIONAL, flowNodeIdent, fNode);
            }
        });
    }

    private void createFlowCapableNode(final InstanceIdentifier<FlowCapableNode> flowNodeIdent,
            final FlowCapableNodeUpdated flowNode) {
        manager.enqueue(new InventoryOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction tx) {
                LOG.debug("creating FlowCapableNode :{} ", flowNodeIdent);
                final FlowCapableNode fNode = new FlowCapableNodeBuilder(flowNode).build();
                ensureFlowCapableNodeAugmentation(tx, flowNodeIdent);
                tx.put(LogicalDatastoreType.OPERATIONAL, flowNodeIdent, fNode);
                createTables(flowNode.getSwitchFeatures().getMaxTables(), flowNodeIdent, tx);
            }
        });
    }

    private void ensureFlowCapableNodeAugmentation(ReadWriteTransaction tx, InstanceIdentifier<FlowCapableNode> flowNodeIdent) {
        tx.merge(LogicalDatastoreType.OPERATIONAL, flowNodeIdent, new FlowCapableNodeBuilder().build(), true);
    }
}

