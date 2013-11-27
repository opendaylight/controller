package org.opendaylight.md.controller.topology.manager

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscovered
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkOverutilized
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkUtilizationNormal
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.Topology
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.node.TerminationPoint
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.TopologyKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.NetworkTopology
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.Node

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.NodeKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.NodeId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.node.TerminationPointKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.TpId
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import static extension org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.*
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction
import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader
import com.google.common.collect.FluentIterable
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.Link

class FlowCapableTopologyExporter implements //
FlowTopologyDiscoveryListener, //
OpendaylightInventoryListener //
{

    var TopologyKey topology;

    @Property
    var DataProviderService dataService;

    override onNodeRemoved(NodeRemoved notification) {
        val invNodeKey = notification.nodeRef.nodeKey
        val tpNodeId = invNodeKey.id.toToplogyNodeId()
        val tpNodeInstance = notification.nodeRef.toNodeIdentifier()

        val it = dataService.beginTransaction
        removeRuntimeData(tpNodeInstance);
        removeAffectedLinks(tpNodeId)
        commit()

    }

    override onNodeUpdated(NodeUpdated notification) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override onNodeConnectorRemoved(NodeConnectorRemoved notification) {
        val tpRef = notification.nodeConnectorRef.toTerminationPointIdentifier();
        val it = dataService.beginTransaction
        removeRuntimeData(tpRef);
        commit()

    }

    override onNodeConnectorUpdated(NodeConnectorUpdated notification) {
        val nodeId = notification.nodeConnectorRef.nodeKey.id.toToplogyNodeId();
        val TerminationPoint point = notification.toTerminationPoint();
        val path = tpPath(nodeId, point.key.tpId);

        val it = dataService.beginTransaction
        putRuntimeData(path, point);
        commit()
    }

    override onLinkDiscovered(LinkDiscovered notification) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override onLinkOverutilized(LinkOverutilized notification) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override onLinkRemoved(LinkRemoved notification) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")

    }

    override onLinkUtilizationNormal(LinkUtilizationNormal notification) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    def InstanceIdentifier<Node> toNodeIdentifier(NodeRef ref) {
        val invNodeKey = ref.nodeKey

        val nodeKey = new NodeKey(invNodeKey.id.toToplogyNodeId);
        return InstanceIdentifier.builder.node(NetworkTopology).child(Topology, topology).child(Node, nodeKey).
            toInstance;
    }

    def InstanceIdentifier<TerminationPoint> toTerminationPointIdentifier(NodeConnectorRef ref) {
        val invNodeKey = ref.nodeKey
        val invNodeConnectorKey = ref.nodeConnectorKey
        return tpPath(invNodeKey.id.toToplogyNodeId(), invNodeConnectorKey.id.toTerminationPointId())
    }

    private def void removeAffectedLinks(DataModificationTransaction transaction, NodeId id) {
        val reader = TypeSafeDataReader.forReader(transaction)
        val topologyPath = InstanceIdentifier.builder().node(NetworkTopology).child(Topology, topology).toInstance;
        val topologyData = reader.readOperationalData(topologyPath);
        if (topologyData === null) {
            return;
        }
        val affectedLinkInstances = FluentIterable.from(topologyData.link).filter[
            source.sourceNode == id || destination.destNode == id].transform [
            //
            InstanceIdentifier.builder().node(NetworkTopology).child(Topology, topology).child(Link, key).toInstance
        //
        ]
        for(affectedLink : affectedLinkInstances) {
            transaction.removeRuntimeData(affectedLink);
        }
    }
    
    private def InstanceIdentifier<TerminationPoint> tpPath(NodeId nodeId, TpId tpId) {
        val nodeKey = new NodeKey(nodeId);
        val tpKey = new TerminationPointKey(tpId)
        return InstanceIdentifier.builder.node(NetworkTopology).child(Topology, topology).child(Node, nodeKey).
            child(TerminationPoint, tpKey).toInstance;
    }
}
