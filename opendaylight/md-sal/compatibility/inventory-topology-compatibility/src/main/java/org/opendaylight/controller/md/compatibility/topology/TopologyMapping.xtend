package org.opendaylight.controller.md.compatibility.topology

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.TopologyKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.Topology
import org.opendaylight.controller.sal.core.Edge
import java.util.Set
import org.opendaylight.controller.sal.core.Property
import org.opendaylight.controller.sal.core.NodeConnector

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.node.TerminationPoint
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.Link
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.node.TerminationPointKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.TpId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.NodeKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.NodeId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.LinkKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.LinkId
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.Node
import org.opendaylight.controller.sal.compatibility.InventoryMapping
class TopologyMapping {

    new(TopologyKey path, InstanceIdentifier<Topology> key) {
        // NOOP
    }

    def Edge toAdTopologyEdge(InstanceIdentifier<Link> identifier) {
        val linkKey = (identifier.path.last as IdentifiableItem<Link,LinkKey>).key;
        val components = linkKey.linkId.value.split("::::");
        val tail = InventoryMapping.nodeConnectorFromId(components.get(0));
        val head = InventoryMapping.nodeConnectorFromId(components.get(1));
        return new Edge(tail, head);
    }

    def NodeConnector toAdTopologyNodeConnector(InstanceIdentifier<TerminationPoint> identifier) {
        val tpKey = (identifier.path.last as IdentifiableItem<TerminationPoint,TerminationPointKey>).key;
        return InventoryMapping.nodeConnectorFromId(tpKey.tpId.value);
    }

    def org.opendaylight.controller.sal.core.Node toAdTopologyNode(
        InstanceIdentifier<Node> identifier) {
        val tpKey = (identifier.path.last as IdentifiableItem<Node,NodeKey>).key;
        return InventoryMapping.nodeFromNodeId(tpKey.nodeId.value);
    }
    


    def NodeKey toTopologyNodeKey(org.opendaylight.controller.sal.core.Node node) {
        val nodeId = new NodeId(InventoryMapping.toNodeId(node));
        return new NodeKey(nodeId);
    }

    def TerminationPointKey toTopologyTerminationPointKey(NodeConnector nc) {
        val node = nc.node;
        val nodeId = new TpId(InventoryMapping.toNodeConnectorId(nc))
        return new TerminationPointKey(nodeId);
    }

    def LinkKey toTopologyLinkKey(Edge edge) {
        val sourceTp = edge.tailNodeConnector.toTopologyTerminationPointKey;
        val destTp = edge.headNodeConnector.toTopologyTerminationPointKey;
        val linkId = new LinkId('''«sourceTp.tpId»::::«destTp.tpId»''')
        return new LinkKey(linkId);
    }
}
