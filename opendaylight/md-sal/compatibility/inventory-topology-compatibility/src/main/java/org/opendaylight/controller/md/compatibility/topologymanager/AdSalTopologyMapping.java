/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.compatibility.topologymanager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AdSalTopologyMapping {
    private final InstanceIdentifier<Topology> topologyPath;

    public AdSalTopologyMapping(final TopologyKey topology) {
        this.topologyPath = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topology).toInstance();
    }

    public InstanceIdentifier<Topology> getTopologyPath() {
        return topologyPath;
    }

    public InstanceIdentifier<TerminationPoint> toTerminationPoint(final NodeConnector connector) {
        return getTopologyPath().builder()
                .child(Node.class)
                .child(TerminationPoint.class, toTerminationPointKey(connector))
                .toInstance();
    }

    public Map<Edge,Set<Property>> toEdgePropertiesMap(final Iterable<Link> links) {
        final HashMap<Edge,Set<Property>> ret = new HashMap<>();
        for (final Link link : links) {
            try {
                ret.put(toEdge(link), toProperties(link));
            } catch (ConstructionException e) {
                throw new IllegalStateException(String.format("Failed to create edge properties for {}", link), e);
            }
        }
        return ret;
    }

    public static Set<Edge> toEdges(final Iterable<Link> links) throws ConstructionException {
        final HashSet<Edge> ret = new HashSet<Edge>();
        for (final Link link : links) {
            ret.add(toEdge(link));
        }
        return ret;
    }

    public static Edge toEdge(final Link link) throws ConstructionException {
        final NodeConnector tail = toNodeConnector(link.getSource());
        final NodeConnector head = toNodeConnector(link.getDestination());
        return new Edge(tail, head);
    }

    public static org.opendaylight.controller.sal.core.Node toAdNode(final Node node) throws ConstructionException {
        return toAdNode(node.getNodeId());
    }

    public static org.opendaylight.controller.sal.core.Node toAdNode(final NodeId node) throws ConstructionException {
        final NodeKey key = new NodeKey(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(node));
        return new org.opendaylight.controller.sal.core.Node(NodeMapping.MD_SAL_TYPE, key);
    }

    public static NodeConnector toNodeConnector(final Source ref) throws ConstructionException {
        final org.opendaylight.controller.sal.core.Node adNode = toAdNode(ref.getSourceNode());
        final NodeConnectorKey key = new NodeConnectorKey(new NodeConnectorId(ref.getSourceTp()));
        return new NodeConnector(NodeMapping.MD_SAL_TYPE, key, adNode);
    }

    public static NodeConnector toNodeConnector(final Destination ref) throws ConstructionException {
        final org.opendaylight.controller.sal.core.Node adNode = toAdNode(ref.getDestNode());
        final NodeConnectorKey key = new NodeConnectorKey(new NodeConnectorId(ref.getDestTp()));
        return new NodeConnector(NodeMapping.MD_SAL_TYPE, key, adNode);
    }

    public TerminationPointKey toTerminationPointKey(final NodeConnector connector) {
        return null;
    }

    public Set<Property> toProperties(final Link link) {
        return null;
    }
}
