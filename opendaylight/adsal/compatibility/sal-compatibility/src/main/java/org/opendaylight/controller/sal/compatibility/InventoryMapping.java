/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import java.util.Iterator;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

import com.google.common.base.Splitter;

public final class InventoryMapping {
    private static final String NODE_TYPE_STRING = "::";
    private static final Splitter NODE_TYPE_SPLITTER = Splitter.on(NODE_TYPE_STRING);

    private InventoryMapping() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static org.opendaylight.controller.sal.core.NodeConnector toAdNodeConnector(final InstanceIdentifier<NodeConnector> identifier) {
        @SuppressWarnings("unchecked")
        final NodeConnectorKey tpKey = ((KeyedInstanceIdentifier<NodeConnector, NodeConnectorKey>) identifier).getKey();
        return InventoryMapping.nodeConnectorFromId(tpKey.getId().getValue());
    }

    public static org.opendaylight.controller.sal.core.Node toAdNode(final InstanceIdentifier<Node> identifier) {
        @SuppressWarnings("unchecked")
        final NodeKey tpKey = ((KeyedInstanceIdentifier<Node,NodeKey>)identifier).getKey();
        return InventoryMapping.nodeFromNodeId(tpKey.getId().getValue());
    }

    public static NodeRef toNodeRef(final org.opendaylight.controller.sal.core.Node node) {
        final NodeKey nodeKey = new NodeKey(new NodeId(InventoryMapping.toNodeId(node)));
        final InstanceIdentifier<Node> path = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeKey).toInstance();
        return new NodeRef(path);
    }

    public static NodeKey toNodeKey(final org.opendaylight.controller.sal.core.Node node) {
        final NodeId nodeId = new NodeId(InventoryMapping.toNodeId(node));
        return new NodeKey(nodeId);
    }

    public static NodeConnectorKey toNodeConnectorKey(final org.opendaylight.controller.sal.core.NodeConnector nc) {
        final NodeConnectorId nodeConnectorId = new NodeConnectorId(InventoryMapping.toNodeConnectorId(nc));
        return new NodeConnectorKey(nodeConnectorId);
    }

    private static StringBuilder nodeIdBulder(final org.opendaylight.controller.sal.core.Node node) {
        final StringBuilder sb = new StringBuilder();
        sb.append("ad-sal:");
        sb.append(node.getType());
        sb.append(NODE_TYPE_STRING);
        sb.append(node.getNodeIDString());
        return sb;
    }

    public static String toNodeId(final org.opendaylight.controller.sal.core.Node node) {
        return nodeIdBulder(node).toString();
    }

    public static String toNodeConnectorId(final org.opendaylight.controller.sal.core.NodeConnector nc) {
        final StringBuilder sb = nodeIdBulder(nc.getNode());
        sb.append(NODE_TYPE_STRING);
        sb.append(nc.getNodeConnectorIDString());
        return sb.toString();
    }

    public static org.opendaylight.controller.sal.core.Node nodeFromNodeId(final String nodeId) {
        return InventoryMapping.nodeFromStrings(NODE_TYPE_SPLITTER.split(nodeId).iterator());
    }

    public static org.opendaylight.controller.sal.core.NodeConnector nodeConnectorFromId(final String invId) {
        return InventoryMapping.nodeConnectorFromString(NODE_TYPE_SPLITTER.split(invId).iterator());
    }

    private static org.opendaylight.controller.sal.core.NodeConnector nodeConnectorFromString(final Iterator<String> it) {
        final org.opendaylight.controller.sal.core.Node node = InventoryMapping.nodeFromStrings(it);
        return org.opendaylight.controller.sal.core.NodeConnector.fromStringNoNode(it.next(), node);
    }

    private static org.opendaylight.controller.sal.core.Node nodeFromStrings(final Iterator<String> it) {
        final String type = it.next().substring(6);
        return org.opendaylight.controller.sal.core.Node.fromString(type, it.next());
    }
}
