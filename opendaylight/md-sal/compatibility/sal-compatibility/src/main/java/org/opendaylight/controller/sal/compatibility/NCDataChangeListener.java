/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NCDataChangeListener extends AbstractDataChangeListener<NodeConnector> {
    private static final Logger LOG = LoggerFactory.getLogger(NodeDataChangeListener.class);
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    public NCDataChangeListener (final InventoryAndReadAdapter adapter, final DataBroker db) {
        super(adapter,db,NodeConnector.class);
    }

    @Override
    protected void add(InstanceIdentifier<NodeConnector> createKeyIdent, NodeConnector node) {
        FlowCapableNodeConnector fcnc = node.getAugmentation(FlowCapableNodeConnector.class);
        if(fcnc != null) {
            FlowCapableNodeConnectorUpdatedBuilder fcncub = new FlowCapableNodeConnectorUpdatedBuilder(fcnc);
            NodeConnectorUpdatedBuilder builder = new NodeConnectorUpdatedBuilder();
            builder.setId(node.getId());
            builder.setNodeConnectorRef(new NodeConnectorRef(createKeyIdent));
            builder.addAugmentation(FlowCapableNodeConnectorUpdated.class, fcncub.build());
            adapter.onNodeConnectorUpdatedInternal(builder.build());
        }
    }

    @Override
    protected void update(InstanceIdentifier<NodeConnector> updateKeyIdent, NodeConnector original,
            NodeConnector update) {
        add(updateKeyIdent,update);
    }

    @Override
    protected void remove(InstanceIdentifier<NodeConnector> ident, NodeConnector removeValue) {
        NodeConnectorRemovedBuilder builder = new NodeConnectorRemovedBuilder();
        builder.setNodeConnectorRef(new NodeConnectorRef(ident));
        adapter.onNodeConnectorRemovedInternal(builder.build());
    }

    protected InstanceIdentifier<NodeConnector> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).child(NodeConnector.class);
    }
}
