/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.manager

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNodeBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNode
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNodeConnectorBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode

class FlowCapableNodeMapping {

    static def NodeKey getNodeKey(NodeRef ref) {
        (ref?.value?.path?.get(1) as IdentifiableItem<Node,NodeKey>).key
    }

    static def NodeKey getNodeKey(NodeConnectorRef ref) {
        (ref?.value?.path?.get(1) as IdentifiableItem<Node,NodeKey>).key
    }

    static def NodeConnectorKey getNodeConnectorKey(NodeConnectorRef ref) {
        (ref?.value?.path?.get(2) as IdentifiableItem<NodeConnector,NodeConnectorKey>).key
    }

    static def NodeId toToplogyNodeId(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId nodeId) {
        return new NodeId(nodeId);
    }

    static def toTerminationPointId(NodeConnectorId id) {
        return new TpId(id);
    }
    
    static def toTopologyNode(NodeId nodeId,NodeRef invNodeRef) {
        val nb = new NodeBuilder();
        nb.setNodeId(nodeId)
        val inb = new InventoryNodeBuilder
        inb.setInventoryNodeRef(invNodeRef)
        nb.addAugmentation(InventoryNode,inb.build)
        return nb.build();
    }
    
    static def toTerminationPoint(TpId id, NodeConnectorRef invNodeConnectorRef) {
        val tpb = new TerminationPointBuilder
        tpb.setTpId(id);
        val incb = new InventoryNodeConnectorBuilder
        incb.setInventoryNodeConnectorRef(invNodeConnectorRef)
        tpb.addAugmentation(InventoryNodeConnector,incb.build())
        return tpb.build();
    }
    
    static def toTopologyLink(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.Link link) {
        val sb = new SourceBuilder();
        sb.setSourceNode(link.source.nodeKey.id.toToplogyNodeId)
        sb.setSourceTp(link.source.nodeConnectorKey.id.toTerminationPointId)
        val db = new DestinationBuilder();
        db.setDestNode(link.destination.nodeKey.id.toToplogyNodeId)
        db.setDestTp(link.destination.nodeConnectorKey.id.toTerminationPointId)
        val lb = new LinkBuilder();
        lb.setSource(sb.build())
        lb.setDestination(db.build());
        lb.setLinkId(new LinkId(lb.source.sourceTp.value))
        return lb.build();
    } 
}
