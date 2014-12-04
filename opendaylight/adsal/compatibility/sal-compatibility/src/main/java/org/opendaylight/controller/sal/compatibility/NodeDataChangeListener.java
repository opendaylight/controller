/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeDataChangeListener extends AbstractDataChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(NodeDataChangeListener.class);


    public NodeDataChangeListener (final InventoryAndReadAdapter adapter, final DataBroker db) {
        super(adapter,db,Node.class);
    }

    protected void add(InstanceIdentifier<Node> createKeyIdent, Node node) {
        FlowCapableNode fcn = node.getAugmentation(FlowCapableNode.class);
        if(fcn != null) {
            FlowCapableNodeUpdatedBuilder fcbnu = new FlowCapableNodeUpdatedBuilder(fcn);
            NodeUpdatedBuilder builder = new NodeUpdatedBuilder();
            builder.setId(node.getId());
            builder.setNodeRef(new NodeRef(createKeyIdent));
            builder.setNodeConnector(node.getNodeConnector());
            builder.addAugmentation(FlowCapableNodeUpdated.class, fcbnu.build());
            adapter.onNodeUpdatedInternal(builder.build());
        }
    }

    protected void update(InstanceIdentifier<Node> updateKeyIdent, Node original,
            Node update) {
        this.add(updateKeyIdent, update);
    }

    protected void remove(InstanceIdentifier<Node> ident, Node removeValue) {
        NodeRemovedBuilder builder = new NodeRemovedBuilder();
        builder.setNodeRef(new NodeRef(ident));
        adapter.onNodeRemovedInternal(builder.build());
    }

    protected InstanceIdentifier<Node> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class);
    }

}
