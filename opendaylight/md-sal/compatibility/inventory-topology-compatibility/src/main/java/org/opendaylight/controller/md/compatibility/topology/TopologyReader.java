/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.compatibility.topology;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.sal.binding.api.data.RuntimeDataProvider;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyReader implements RuntimeDataProvider {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyReader.class);
    private final InstanceIdentifier<Topology> topologyPath;
    private final TopologyKey topologyKey;
    private final TopologyMapping mapping;
    private ITopologyManager topologyManager;
    private ISwitchManager switchManager;

    public ISwitchManager getSwitchManager() {
        return this.switchManager;
    }

    public void setSwitchManager(final ISwitchManager switchManager) {
        this.switchManager = switchManager;
    }

    public ITopologyManager getTopologyManager() {
        return this.topologyManager;
    }

    public void setTopologyManager(final ITopologyManager topologyManager) {
        this.topologyManager = topologyManager;
    }

    public TopologyKey getTopologyKey() {
        return this.topologyKey;
    }

    public TopologyMapping getMapping() {
        return this.mapping;
    }

    public TopologyReader() {
        this.topologyKey = new TopologyKey(new TopologyId("compatibility:ad-sal"));
        this.topologyPath = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topologyKey)
                .toInstance();
        this.mapping = new TopologyMapping(topologyKey, topologyPath);
    }

    @Override
    public DataObject readConfigurationData(final InstanceIdentifier<? extends DataObject> path) {
        // Topology and Inventory are operational only
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public DataObject readOperationalData(final InstanceIdentifier<? extends DataObject> path) {
        if (!topologyPath.contains(path)) {
            return null;
        }

        final Class<? extends DataObject> type = path.getTargetType();
        if (Link.class.equals(type)) {
            return readLink((InstanceIdentifier<Link>) path);
        }
        if (Node.class.equals(type)) {
            return readNode((InstanceIdentifier<Node>) path);
        }
        if (TerminationPoint.class.equals(type)) {
            return readTerminationPoint((InstanceIdentifier<TerminationPoint>) path);

        }
        if (Topology.class.equals(type)) {
            return readTopology((InstanceIdentifier<Topology>) path);
        }

        LOG.debug("Unsupported type {}", type);
        return null;
    }

    private Link readLink(final InstanceIdentifier<Link> identifier) {
        final Edge edge;
        try {
            edge = this.mapping.toAdTopologyEdge(identifier);
        } catch (ConstructionException e) {
            throw new IllegalStateException(String.format("Failed to construct edge for link %s", identifier), e);
        }

        final Map<Edge,Set<Property>> edges;
        if (topologyManager != null) {
            edges = topologyManager.getEdges();
        } else {
            edges = null;
        }

        final Set<Property> properties;
        if (edges != null) {
            properties = edges.get(edge);
        } else {
            properties = null;
        }

        return constructLink(edge);
    }

    private TerminationPoint readTerminationPoint(final InstanceIdentifier<TerminationPoint> identifier) {
        return constructTerminationPoint(mapping.toAdTopologyNodeConnector(identifier));
    }

    private Node readNode(final InstanceIdentifier<Node> identifier) {
        return constructNode(mapping.toAdTopologyNode(identifier));
    }

    private Topology readTopology(final InstanceIdentifier<Topology> identifier) {
        final Set<org.opendaylight.controller.sal.core.Node> nodes = getSwitchManager().getNodes();
        final ArrayList<Node> nodeList = new ArrayList<Node>(nodes.size());
        for (final org.opendaylight.controller.sal.core.Node node : nodes) {
            nodeList.add(constructNode(node));
        }

        final Map<Edge,Set<Property>> edges = getTopologyManager().getEdges();
        final ArrayList<Link> linkList = new ArrayList<Link>(edges.size());
        for (final Edge edge : edges.keySet()) {
            linkList.add(constructLink(edge));
        }

        return new TopologyBuilder()
        .setKey(topologyKey)
        .setNode(nodeList)
        .setLink(linkList)
        .build();
    }

    private Link constructLink(final Edge edge) {
        final NodeConnector sourceNc = edge.getTailNodeConnector();
        final NodeConnector destNc = edge.getHeadNodeConnector();

        final LinkBuilder it = new LinkBuilder().setKey(mapping.toTopologyLinkKey(edge));

        it.setSource(new SourceBuilder()
        .setSourceNode(mapping.toTopologyNodeKey(sourceNc.getNode()).getNodeId())
        .setSourceTp(mapping.toTopologyTerminationPointKey(sourceNc).getTpId())
        .build());

        it.setDestination(new DestinationBuilder()
        .setDestNode(mapping.toTopologyNodeKey(destNc.getNode()).getNodeId())
        .setDestTp(mapping.toTopologyTerminationPointKey(destNc).getTpId())
        .build());

        return it.build();
    }

    private Node constructNode(final org.opendaylight.controller.sal.core.Node node) {
        final Set<NodeConnector> connectors = getSwitchManager().getNodeConnectors(node);
        final ArrayList<TerminationPoint> tpList = new ArrayList<TerminationPoint>(connectors.size());
        for (final NodeConnector connector : connectors) {
            tpList.add(constructTerminationPoint(connector));
        }

        return new NodeBuilder()
        .setKey(mapping.toTopologyNodeKey(node))
        .setTerminationPoint(tpList)
        .build();
    }

    private TerminationPoint constructTerminationPoint(final NodeConnector connector) {
        return new TerminationPointBuilder().setKey(mapping.toTopologyTerminationPointKey(connector)).build();
    }
}
