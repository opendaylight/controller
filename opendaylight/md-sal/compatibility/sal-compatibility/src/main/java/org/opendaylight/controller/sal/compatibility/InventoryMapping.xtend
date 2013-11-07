/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef

class InventoryMapping {

    static def org.opendaylight.controller.sal.core.NodeConnector toAdNodeConnector(
        InstanceIdentifier<NodeConnector> identifier) {
        val tpKey = (identifier.path.last as IdentifiableItem<NodeConnector,NodeConnectorKey>).key;
        return nodeConnectorFromId(tpKey.id.value);
    }

    static def org.opendaylight.controller.sal.core.Node toAdNode(InstanceIdentifier<Node> identifier) {
        val tpKey = (identifier.path.last as IdentifiableItem<Node,NodeKey>).key;
        return nodeFromNodeId(tpKey.id.value);
    }
    
    
     static def NodeRef toNodeRef(org.opendaylight.controller.sal.core.Node node) {
        val nodeId = new NodeKey(new NodeId(node.toNodeId))
        val path = InstanceIdentifier.builder().node(Nodes).child(Node,nodeId).toInstance;
        return new NodeRef(path);
    }

    static def NodeKey toNodeKey(org.opendaylight.controller.sal.core.Node node) {
        val nodeId = new NodeId(node.toNodeId)
        return new NodeKey(nodeId);
    }

    static def NodeConnectorKey toNodeConnectorKey(org.opendaylight.controller.sal.core.NodeConnector nc) {
        val nodeId = new NodeConnectorId(nc.toNodeConnectorId)
        return new NodeConnectorKey(nodeId);
    }

    static def String toNodeId(org.opendaylight.controller.sal.core.Node node) {
        '''ad-sal:«node.type»::«node.nodeIDString»'''
    }

    static def String toNodeConnectorId(org.opendaylight.controller.sal.core.NodeConnector nc) {
        '''«nc.node.toNodeId»::«nc.nodeConnectorIDString»'''
    }

    static def org.opendaylight.controller.sal.core.Node nodeFromNodeId(String nodeId) {
        return nodeFromString(nodeId.split("::"))
    }

    static def nodeConnectorFromId(String invId) {
        return nodeConnectorFromString(invId.split("::"));
    }

    private static def org.opendaylight.controller.sal.core.NodeConnector nodeConnectorFromString(String[] string) {
        val node = nodeFromString(string.subList(0, 1));
        return org.opendaylight.controller.sal.core.NodeConnector.fromStringNoNode(string.get(2), node);
    }

    private static def org.opendaylight.controller.sal.core.Node nodeFromString(String[] strings) {
        val type = strings.get(0).substring(6);
        org.opendaylight.controller.sal.core.Node.fromString(type, strings.get(1))
    }

}
