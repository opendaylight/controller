/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager

import org.opendaylight.controller.sal.binding.api.NotificationProviderService
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowNodeInventoryListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.TableRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.TableUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.TableBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.TableKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.NotificationListener
import org.slf4j.LoggerFactory

import static extension org.opendaylight.controller.md.inventory.manager.InventoryMapping.*
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.Table

class FlowCapableInventoryProvider implements AutoCloseable {


    static val LOG = LoggerFactory.getLogger(FlowCapableInventoryProvider);

    @Property
    DataProviderService dataService;

    @Property
    NotificationProviderService notificationService;
    val NodeChangeCommiter changeCommiter = new NodeChangeCommiter(this);
    val TableChangeCommiter tableChangeCommiter = new TableChangeCommiter(this); 

    Registration<NotificationListener> listenerRegistration
    Registration<NotificationListener> tableListenerRegistration

    def void start() {
        listenerRegistration = notificationService.registerNotificationListener(changeCommiter);
        tableListenerRegistration = notificationService.registerNotificationListener(tableChangeCommiter);
        LOG.info("Flow Capable Inventory Provider started.");
        
    }

    protected def startChange() {
        return dataService.beginTransaction;
    }

    override close() {
       LOG.info("Flow Capable Inventory Provider stopped.");
        listenerRegistration?.close();
        tableListenerRegistration.close();
    }
    
}

class NodeChangeCommiter implements OpendaylightInventoryListener {
    static val LOG = LoggerFactory.getLogger(NodeChangeCommiter);
    @Property
    val FlowCapableInventoryProvider manager;

    new(FlowCapableInventoryProvider manager) {
        _manager = manager;
    }

    override onNodeConnectorRemoved(NodeConnectorRemoved connector) {
        val ref = connector.nodeConnectorRef;
        LOG.error("onNodeConnectorRemoved - removing: " + ref.value);

        // Check path
        val it = manager.startChange()
        removeRuntimeData(ref.value as InstanceIdentifier<? extends DataObject>);
        commit()
        LOG.error("onNodeConnectorRemoved - removed: " + ref.value);
    }

    override onNodeConnectorUpdated(NodeConnectorUpdated connector) {
        val ref = connector.nodeConnectorRef;
        LOG.error("onNodeConnectorUpdated - updating: " + ref.value + " " + connector);
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
        LOG.error("onNodeConnectorUpdated - updated: " + ref.value + " " + connector);
    }

    override onNodeRemoved(NodeRemoved node) {
        val ref = node.nodeRef;
        LOG.error("onNodeRemoved - removing: " + ref.value);
        val it = manager.startChange()

        removeRuntimeData(ref.value as InstanceIdentifier<? extends DataObject>);
        commit()
        LOG.error("onNodeRemoved - removed: " + ref.value);
    }

    override onNodeUpdated(NodeUpdated node) {
        val ref = node.nodeRef;
        LOG.error("onNodeUpdated - updating: " + ref.value + " " + node);
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
        LOG.error("onNodeUpdated - updated: " + ref.value + " " + node);
    }
}

class TableChangeCommiter implements FlowNodeInventoryListener {
    static val LOG = LoggerFactory.getLogger(TableChangeCommiter);
     @Property
    val FlowCapableInventoryProvider manager;

    new(FlowCapableInventoryProvider manager) {
        _manager = manager;
    }
    override onTableRemoved(TableRemoved table) {
        val ref = table.tableRef;
        LOG.error("onTableRemoved - removing " + ref.value + " " + table);
        val it = manager.startChange();
        removeRuntimeData(ref.value as InstanceIdentifier<? extends DataObject>);
        commit()
        LOG.error("onTableRemoved - removed: " + ref.value);
    }
    
    override onTableUpdated(TableUpdated table) {
        val ref = table.tableRef;
        LOG.error("onTableUpdated - updating " + ref.value + " " + table);
        val it = manager.startChange();
        val data = new TableBuilder(table);
        data.key = (new TableKey(table.id));
        putRuntimeData(ref.value as InstanceIdentifier<Table>, data.build);
        LOG.error("onTableUpdated - updating " + ref.value + " " + table);
    }
    
}
