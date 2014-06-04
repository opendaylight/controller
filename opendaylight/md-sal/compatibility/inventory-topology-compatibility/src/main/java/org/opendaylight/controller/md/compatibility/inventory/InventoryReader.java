/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.compatibility.inventory;

import java.util.ArrayList;
import java.util.Set;

import org.opendaylight.controller.sal.binding.api.data.RuntimeDataProvider;
import org.opendaylight.controller.sal.compatibility.InventoryMapping;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryReader implements RuntimeDataProvider {
    private static final Logger LOG = LoggerFactory.getLogger(InventoryReader.class);
    private ISwitchManager switchManager;

    public ISwitchManager getSwitchManager() {
        return switchManager;
    }

    public void setSwitchManager(final ISwitchManager switchManager) {
        this.switchManager = switchManager;
    }

    @Override
    public DataObject readConfigurationData(final InstanceIdentifier<? extends DataObject> path) {
        // Topology and Inventory are operational only
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public DataObject readOperationalData(final InstanceIdentifier<? extends DataObject> path) {
        final Class<? extends DataObject> type = path.getTargetType();
        if (Nodes.class.equals(type)) {
            return readNodes(((InstanceIdentifier<Nodes>) path));
        }
        if (Node.class.equals(type)) {
            return readNode(((InstanceIdentifier<Node>) path));
        }
        if (NodeConnector.class.equals(type)) {
            return readNodeConnector(((InstanceIdentifier<NodeConnector>) path));
        }

        LOG.debug("Unsupported type {}", type);
        return null;
    }

    private NodeConnector readNodeConnector(final InstanceIdentifier<NodeConnector> identifier) {
        return constructNodeConnector(InventoryMapping.toAdNodeConnector(identifier));
    }

    private Node readNode(final InstanceIdentifier<Node> identifier) {
        return constructNode(InventoryMapping.toAdNode(identifier));
    }

    private Node constructNode(final org.opendaylight.controller.sal.core.Node node) {
        final Set<org.opendaylight.controller.sal.core.NodeConnector> connectors = getSwitchManager().getNodeConnectors(node);
        final ArrayList<NodeConnector> tpList = new ArrayList<NodeConnector>(connectors.size());
        for (final org.opendaylight.controller.sal.core.NodeConnector connector : connectors) {
            tpList.add(constructNodeConnector(connector));
        }

        return new NodeBuilder()
        .setKey(InventoryMapping.toNodeKey(node))
        .setNodeConnector(tpList)
        .build();
    }

    private Nodes readNodes(final InstanceIdentifier<Nodes> identifier) {
        final Set<org.opendaylight.controller.sal.core.Node> nodes = getSwitchManager().getNodes();
        final ArrayList<Node> nodeList = new ArrayList<Node>(nodes.size());
        for (final org.opendaylight.controller.sal.core.Node node : nodes) {
            nodeList.add(constructNode(node));
        }

        return new NodesBuilder().setNode(nodeList).build();
    }

    private static NodeConnector constructNodeConnector(final org.opendaylight.controller.sal.core.NodeConnector connector) {
        return new NodeConnectorBuilder().setKey(InventoryMapping.toNodeConnectorKey(connector)).build();
    }
}
