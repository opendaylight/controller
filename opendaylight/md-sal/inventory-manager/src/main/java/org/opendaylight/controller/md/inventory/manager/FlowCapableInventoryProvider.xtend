/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated
import org.opendaylight.controller.sal.binding.api.NotificationProviderService
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.yangtools.yang.binding.NotificationListener
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
import static extension org.opendaylight.controller.md.inventory.manager.InventoryMapping.*
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
import org.slf4j.LoggerFactory

class FlowCapableInventoryProvider implements AutoCloseable {


    static val LOG = LoggerFactory.getLogger(FlowCapableInventoryProvider);

    @Property
    DataProviderService dataService;

    @Property
    NotificationProviderService notificationService;
    val NodeChangeCommiter changeCommiter = new NodeChangeCommiter(this);

    Registration<NotificationListener> listenerRegistration

    def void start() {
        listenerRegistration = notificationService.registerNotificationListener(changeCommiter);
        LOG.info("Flow Capable Inventory Provider started.");
        
    }

    protected def startChange() {
        return dataService.beginTransaction;
    }

    override close() {
       LOG.info("Flow Capable Inventory Provider stopped.");
        listenerRegistration?.close();
    }
    
}

class NodeChangeCommiter implements OpendaylightInventoryListener {

    @Property
    val FlowCapableInventoryProvider manager;

    new(FlowCapableInventoryProvider manager) {
        _manager = manager;
    }

    override onNodeConnectorRemoved(NodeConnectorRemoved connector) {
        val ref = connector.nodeConnectorRef;

        // Check path
        val it = manager.startChange()
        removeRuntimeData(ref.value as InstanceIdentifier<? extends DataObject>);
        commit()
    }

    override onNodeConnectorUpdated(NodeConnectorUpdated connector) {
        val ref = connector.nodeConnectorRef;

        val flowConnector = connector.getAugmentation(FlowCapableNodeConnectorUpdated);

        val it = manager.startChange()
        val data = new NodeConnectorBuilder(connector);
        data.key = (new NodeConnectorKey(connector.id))
        if (flowConnector != null) {
            val augment = flowConnector.toInventoryAugment()
            data.addAugmentation(FlowCapableNodeConnector, augment)
        }

        putRuntimeData(ref.value as InstanceIdentifier<NodeConnector>, data.build());
        commit()
    }

    override onNodeRemoved(NodeRemoved node) {
        val ref = node.nodeRef;
        val it = manager.startChange()

        removeRuntimeData(ref.value as InstanceIdentifier<? extends DataObject>);
        commit()
    }

    override onNodeUpdated(NodeUpdated node) {
        val ref = node.nodeRef;
        val flowNode = node.getAugmentation(FlowCapableNodeUpdated);

        val it = manager.startChange()
        val data = new NodeBuilder(node);
        data.key = (new NodeKey(node.id))
        if (flowNode != null) {
            val augment = flowNode.toInventoryAugment();
            data.addAugmentation(FlowCapableNode, augment)
        }

        putRuntimeData(ref.value as InstanceIdentifier<Node>, data.build())
        commit()
    }
}
