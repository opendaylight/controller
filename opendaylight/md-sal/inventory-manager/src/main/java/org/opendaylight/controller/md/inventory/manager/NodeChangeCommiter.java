/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

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
            public void applyOperation(final DataModificationTransaction tx) {
                final NodeConnectorRef ref = connector.getNodeConnectorRef();
                LOG.debug("removing node connector {} ", ref.getValue());
                tx.removeOperationalData(ref.getValue());
            }
        });
    }

    @Override
    public synchronized void onNodeConnectorUpdated(final NodeConnectorUpdated connector) {
        manager.enqueue(new InventoryOperation() {
            @Override
            public void applyOperation(final DataModificationTransaction tx) {
                final NodeConnectorRef ref = connector.getNodeConnectorRef();
                final NodeConnectorBuilder data = new NodeConnectorBuilder(connector);
                data.setKey(new NodeConnectorKey(connector.getId()));

                final FlowCapableNodeConnectorUpdated flowConnector = connector
                        .getAugmentation(FlowCapableNodeConnectorUpdated.class);
                if (flowConnector != null) {
                    final FlowCapableNodeConnector augment = InventoryMapping.toInventoryAugment(flowConnector);
                    data.addAugmentation(FlowCapableNodeConnector.class, augment);
                }
                InstanceIdentifier<? extends Object> value = ref.getValue();
                LOG.debug("updating node connector : {}.", value);
                NodeConnector build = data.build();
                tx.putOperationalData(value, build);
            }
        });
    }

    @Override
    public synchronized void onNodeRemoved(final NodeRemoved node) {
        manager.enqueue(new InventoryOperation() {
            @Override
            public void applyOperation(final DataModificationTransaction tx) {
                final NodeRef ref = node.getNodeRef();
                LOG.debug("removing node : {}", ref.getValue());
                tx.removeOperationalData((ref.getValue()));
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
            public void applyOperation(final DataModificationTransaction tx) {
                final NodeRef ref = node.getNodeRef();
                final NodeBuilder nodeBuilder = new NodeBuilder(node);
                nodeBuilder.setKey(new NodeKey(node.getId()));

                final FlowCapableNode augment = InventoryMapping.toInventoryAugment(flowNode);
                nodeBuilder.addAugmentation(FlowCapableNode.class, augment);

                @SuppressWarnings("unchecked")
                InstanceIdentifierBuilder<Node> builder = ((InstanceIdentifier<Node>) ref.getValue()).builder();
                InstanceIdentifierBuilder<FlowCapableNode> augmentation = builder.augmentation(FlowCapableNode.class);
                final InstanceIdentifier<FlowCapableNode> path = augmentation.build();
                LOG.debug("updating node :{} ", path);
                tx.putOperationalData(path, augment);
            }
        });
    }
}
