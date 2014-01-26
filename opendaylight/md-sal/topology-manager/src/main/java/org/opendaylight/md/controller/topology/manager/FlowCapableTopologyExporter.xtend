/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.manager

import com.google.common.collect.FluentIterable
import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryListener
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscovered
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkOverutilized
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkUtilizationNormal
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

import static extension org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.*

class FlowCapableTopologyExporter implements //
FlowTopologyDiscoveryListener, //
OpendaylightInventoryListener //
{

    var TopologyKey topology = new TopologyKey(new TopologyId("flow:1"));

    @Property
    var DataProviderService dataService;
    
    def start() {
        val tb = new TopologyBuilder();
        tb.setKey(topology);
        val path = InstanceIdentifier.builder(NetworkTopology).child(Topology,topology).toInstance;
        val top = tb.build();
        val it = dataService.beginTransaction
        putOperationalData(path,top);
        commit()       
    }

    override onNodeRemoved(NodeRemoved notification) {
        val nodeId = notification.nodeRef.nodeKey.id.toToplogyNodeId()
        val nodeInstance = notification.nodeRef.toNodeIdentifier()

        val it = dataService.beginTransaction
        removeOperationalData(nodeInstance);
        removeAffectedLinks(it,nodeId)
        commit()

    }

    override onNodeUpdated(NodeUpdated notification) {
        val fcnu = notification.getAugmentation(FlowCapableNodeUpdated)
        if(fcnu != null) {
            val node = notification.id.toToplogyNodeId.toTopologyNode(notification.nodeRef)
            val path = notification.id.toToplogyNodeId.nodePath;
            val it = dataService.beginTransaction
            putOperationalData(path, node);
            commit()
        }
    }

    override onNodeConnectorRemoved(NodeConnectorRemoved notification) {
        val tpInstance = notification.nodeConnectorRef.toTerminationPointIdentifier;
        val tpId = notification.nodeConnectorRef.nodeConnectorKey.id.toTerminationPointId;
        val it = dataService.beginTransaction
        removeOperationalData(tpInstance);
        removeAffectedLinks(it,tpId)
        commit()

    }

    override onNodeConnectorUpdated(NodeConnectorUpdated notification) {
        val fcncu = notification.getAugmentation(FlowCapableNodeConnectorUpdated)
        if(fcncu != null) {
            val nodeId = notification.nodeConnectorRef.nodeKey.id.toToplogyNodeId;
            val TerminationPoint point = notification.id.toTerminationPointId.toTerminationPoint(notification.nodeConnectorRef);
            val path = tpPath(nodeId, point.key.tpId);
    
            val it = dataService.beginTransaction
            putOperationalData(path, point);
            if((fcncu.state != null && fcncu.state.linkDown) || (fcncu.configuration != null && fcncu.configuration.PORTDOWN)) {
                removeAffectedLinks(it,point.tpId)
            }
            commit()     
       }
    }

    override onLinkDiscovered(LinkDiscovered notification) {
        val link = notification.toTopologyLink;
        val path = link.linkPath;
        val it = dataService.beginTransaction
        putOperationalData(path, link);
        commit()
    }

    override onLinkOverutilized(LinkOverutilized notification) {
        // NOOP
    }

    override onLinkRemoved(LinkRemoved notification) {
        val path = notification.toTopologyLink.linkPath
        val it = dataService.beginTransaction
        removeOperationalData(path);
        commit()
    }

    override onLinkUtilizationNormal(LinkUtilizationNormal notification) {
        // NOOP
    }

    def InstanceIdentifier<Node> toNodeIdentifier(NodeRef ref) {
        val invNodeKey = ref.nodeKey

        val nodeKey = new NodeKey(invNodeKey.id.toToplogyNodeId);
        return InstanceIdentifier.builder(NetworkTopology).child(Topology, topology).child(Node, nodeKey).
            toInstance;
    }

    def InstanceIdentifier<TerminationPoint> toTerminationPointIdentifier(NodeConnectorRef ref) {
        val invNodeKey = ref.nodeKey
        val invNodeConnectorKey = ref.nodeConnectorKey
        return tpPath(invNodeKey.id.toToplogyNodeId(), invNodeConnectorKey.id.toTerminationPointId())
    }

    private def void removeAffectedLinks(DataModificationTransaction transaction, NodeId id) {
        val reader = TypeSafeDataReader.forReader(transaction)
        val topologyPath = InstanceIdentifier.builder(NetworkTopology).child(Topology, topology).toInstance;
        val topologyData = reader.readOperationalData(topologyPath);
        if (topologyData === null) {
            return;
        }
        val affectedLinkInstances = FluentIterable.from(topologyData.link).filter[
            source.sourceNode == id || destination.destNode == id].transform [
            //
            InstanceIdentifier.builder(NetworkTopology).child(Topology, topology).child(Link, key).toInstance
        //
        ]
        for(affectedLink : affectedLinkInstances) {
            transaction.removeOperationalData(affectedLink);
        }
    }
    
    private def void removeAffectedLinks(DataModificationTransaction transaction, TpId id) {
        val reader = TypeSafeDataReader.forReader(transaction)
        val topologyPath = InstanceIdentifier.builder(NetworkTopology).child(Topology, topology).toInstance;
        val topologyData = reader.readOperationalData(topologyPath);
        if (topologyData === null) {
            return;
        }
        val affectedLinkInstances = FluentIterable.from(topologyData.link).filter[
            source.sourceTp == id || destination.destTp == id].transform [
            //
            InstanceIdentifier.builder(NetworkTopology).child(Topology, topology).child(Link, key).toInstance
        //
        ]
        for(affectedLink : affectedLinkInstances) {
            transaction.removeOperationalData(affectedLink);
        }
    }
    
    private def InstanceIdentifier<Node> nodePath(NodeId nodeId) {
        val nodeKey = new NodeKey(nodeId);
        return InstanceIdentifier.builder(NetworkTopology)
            .child(Topology, topology)
            .child(Node, nodeKey)
            .toInstance;
    }
    
    private def InstanceIdentifier<TerminationPoint> tpPath(NodeId nodeId, TpId tpId) {
        val nodeKey = new NodeKey(nodeId);
        val tpKey = new TerminationPointKey(tpId)
        return InstanceIdentifier.builder(NetworkTopology).child(Topology, topology).child(Node, nodeKey).
            child(TerminationPoint, tpKey).toInstance;
    }
    
    private def InstanceIdentifier<Link> linkPath(Link link) {
        val linkInstanceId = InstanceIdentifier.builder(NetworkTopology)
            .child(Topology, topology)
            .child(Link, link.key)
            .toInstance;
        return linkInstanceId;
    }    
}
