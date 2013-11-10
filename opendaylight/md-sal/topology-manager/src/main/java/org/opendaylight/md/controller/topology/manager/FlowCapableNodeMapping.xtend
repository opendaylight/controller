package org.opendaylight.md.controller.topology.manager

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnector
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.node.TerminationPoint
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.node.TerminationPointBuilder
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.node.TerminationPointKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.TpId
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.NodeId

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

    static def TerminationPoint toTerminationPoint(NodeConnectorUpdated updated) {
        val it = new TerminationPointBuilder
        key = new TerminationPointKey(new TpId(updated.id));
        return it.build()
    }

    static def NodeId toToplogyNodeId(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId nodeId) {
        return new NodeId(nodeId);
    }

    static def toTerminationPointId(NodeConnectorId id) {
        return new TpId(id);
    }
}
