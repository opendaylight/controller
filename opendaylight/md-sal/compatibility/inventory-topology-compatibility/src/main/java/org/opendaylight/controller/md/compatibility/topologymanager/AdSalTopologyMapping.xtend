package org.opendaylight.controller.md.compatibility.topologymanager

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.TopologyKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.node.TerminationPoint
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.Topology
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.NetworkTopology
import java.util.Map
import org.opendaylight.controller.sal.core.Edge
import java.util.Set
import java.util.List
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.Node
import java.util.Collections
import com.google.common.collect.FluentIterable
import java.util.HashSet
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId
import org.opendaylight.controller.sal.compatibility.NodeMapping
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.Link
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.link.attributes.Source
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.link.attributes.Destination
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.TpId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.node.TerminationPointKey
import java.util.HashMap

class AdSalTopologyMapping {

    val TopologyKey topologyMapping;
    @Property
    val InstanceIdentifier<Topology> topologyPath;

    new(TopologyKey topology) {
        topologyMapping = topology;
        _topologyPath = InstanceIdentifier.builder.node(NetworkTopology).child(Topology, topology).toInstance;
    }

    def InstanceIdentifier<TerminationPoint> toTerminationPoint(NodeConnector connector) {
        InstanceIdentifier.builder(topologyPath).node(Node).child(TerminationPoint, connector.toTerminationPointKey()).toInstance;
    }

    def Map<Edge, Set<org.opendaylight.controller.sal.core.Property>> toEdgePropertiesMap(Iterable<Link> links) {
        val ret = new HashMap<Edge, Set<org.opendaylight.controller.sal.core.Property>>
        for (link : links) {
            ret.put(link.toEdge(), link.toProperties())
        }
        return ret;
    }

    def Set<Edge> toEdges(Iterable<Link> links) {
        val ret = new HashSet<Edge>
        for (link : links) {
            ret.add(link.toEdge)
        }
        return ret;
    }

    def Edge toEdge(Link link) {
        val tail = link.source.toNodeConnector();
        val head = link.destination.toNodeConnector();
        return new Edge(tail, head);
    }

    def org.opendaylight.controller.sal.core.Node toAdNode(Node node) {
        return node.nodeId.toAdNode;
    }

    def org.opendaylight.controller.sal.core.Node toAdNode(
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.NodeId node) {
        val key = new NodeKey(new NodeId(node))
        return new org.opendaylight.controller.sal.core.Node(NodeMapping.MD_SAL_TYPE, key);
    }

    def NodeConnector toNodeConnector(Source ref) {
        val adNode = ref.sourceNode.toAdNode();
        val key = new NodeConnectorKey(new NodeConnectorId(ref.sourceTp))
        return new NodeConnector(NodeMapping.MD_SAL_TYPE, key, adNode);
    }

    def NodeConnector toNodeConnector(Destination ref) {
        val adNode = ref.destNode.toAdNode();
        val key = new NodeConnectorKey(new NodeConnectorId(ref.destTp))
        return new NodeConnector(NodeMapping.MD_SAL_TYPE, key, adNode);
    }

    def TerminationPointKey toTerminationPointKey(NodeConnector connector) {
    }

    def Set<org.opendaylight.controller.sal.core.Property> toProperties(Link link) {
    }
}
