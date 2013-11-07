package org.opendaylight.controller.md.compatibility.topology

import org.opendaylight.controller.switchmanager.ISwitchManager
import org.opendaylight.controller.topologymanager.ITopologyManager
import org.opendaylight.controller.md.sal.common.api.data.DataReader
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.sal.binding.api.data.RuntimeDataProvider
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.Topology
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.Node
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.node.TerminationPoint
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.Link
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.TopologyKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.NetworkTopology
import org.opendaylight.controller.md.compatibility.topology.TopologyMapping
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.LinkBuilder

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.TopologyBuilder
import java.util.ArrayList
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.NodeBuilder
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.NodeKey
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.TopologyId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.node.TerminationPointBuilder
import org.opendaylight.controller.sal.core.Edge
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.link.attributes.SourceBuilder
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.link.attributes.DestinationBuilder

class TopologyReader implements RuntimeDataProvider {

    @Property
    var ISwitchManager switchManager;

    @Property
    var ITopologyManager topologyManager;

    @Property
    val TopologyKey topologyKey;

    @Property
    val InstanceIdentifier<Topology> topologyPath;

    @Property
    val extension TopologyMapping mapping;

    new() {
        _topologyKey = new TopologyKey(new TopologyId("compatibility:ad-sal"));
        _topologyPath = InstanceIdentifier.builder().node(NetworkTopology).child(Topology, topologyKey).toInstance;
        _mapping = new TopologyMapping(topologyKey, topologyPath);
    }

    override readConfigurationData(InstanceIdentifier<? extends DataObject> path) {

        // Topology and Inventory are operational only
        return null;
    }

    override readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        val type = path.targetType;
        var DataObject data = null;
        if (false == topologyPath.contains(path)) {
            return null;
        }
        switch (type) {
            case Topology:
                data = readTopology(path as InstanceIdentifier<Topology>)
            case Node:
                data = readNode(path as InstanceIdentifier<Node>)
            case TerminationPoint:
                data = readTerminationPoint(path as InstanceIdentifier<TerminationPoint>)
            case Link:
                data = readLink(path as InstanceIdentifier<Link>)
        }
        return data;
    }

    def DataObject readLink(InstanceIdentifier<Link> identifier) {
        val edge = identifier.toAdTopologyEdge();
        val properties = topologyManager?.edges?.get(edge);

        return constructLink(edge);
    }

    def DataObject readTerminationPoint(InstanceIdentifier<TerminationPoint> identifier) {
        val nodeConnector = identifier.toAdTopologyNodeConnector();
        return constructTerminationPoint(nodeConnector)
    }

    def DataObject readNode(InstanceIdentifier<Node> identifier) {
        val node = identifier.toAdTopologyNode();
        return constructNode(node);
    }

    def DataObject readTopology(InstanceIdentifier<Topology> identifier) {

        //val nodeConnectors = switchManager.
        val nodes = switchManager.nodes
        val edges = topologyManager.edges

        val nodeList = new ArrayList<Node>(nodes.size)
        for (node : nodes) {
            nodeList.add(constructNode(node))
        }

        val linkList = new ArrayList<Link>(edges.size)
        for (edge : edges.keySet) {
            linkList.add(constructLink(edge))
        }

        val it = new TopologyBuilder();
        key = topologyKey
        node = nodeList
        link = linkList
        return build()
    }

    def constructLink(Edge edge) {
        val sourceNc = edge.tailNodeConnector
        val destNc = edge.headNodeConnector

        val it = new LinkBuilder()
        key = edge.toTopologyLinkKey();
        source = new SourceBuilder().setSourceNode(sourceNc.node.toTopologyNodeKey.nodeId).setSourceTp(
            sourceNc.toTopologyTerminationPointKey.tpId).build()
        destination = new DestinationBuilder().setDestNode(destNc.node.toTopologyNodeKey.nodeId).setDestTp(
            destNc.toTopologyTerminationPointKey.tpId).build
        return build()
    }

    def Node constructNode(org.opendaylight.controller.sal.core.Node node) {
        val connectors = switchManager.getNodeConnectors(node)

        val tpList = new ArrayList<TerminationPoint>(connectors.size)
        for (connector : connectors) {
            tpList.add(constructTerminationPoint(connector));
        }

        val it = new NodeBuilder()
        key = node.toTopologyNodeKey();
        terminationPoint = tpList
        return build();
    }

    def TerminationPoint constructTerminationPoint(NodeConnector connector) {
        val it = new TerminationPointBuilder()
        key = connector.toTopologyTerminationPointKey
        return build();
    }

}
