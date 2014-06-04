/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.compatibility.topology;

import java.util.Iterator;

import org.opendaylight.controller.sal.compatibility.InventoryMapping;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

import com.google.common.base.Splitter;

public class TopologyMapping {
    private static final String HEAD_TAIL_STRING = "::::";
    private static final Splitter HEAD_TAIL_SPLITTER = Splitter.on(HEAD_TAIL_STRING);

    public TopologyMapping(final TopologyKey path, final InstanceIdentifier<Topology> key) {
        // No-op for now. Multi-instance will require fixing InventoryMapping first.
    }

    public Edge toAdTopologyEdge(final InstanceIdentifier<Link> identifier) throws ConstructionException {
        @SuppressWarnings("unchecked")
        final LinkKey linkKey = ((KeyedInstanceIdentifier<Link, LinkKey>)identifier).getKey();

        final Iterator<String> it = HEAD_TAIL_SPLITTER.split(linkKey.getLinkId().getValue()).iterator();
        final NodeConnector tail = InventoryMapping.nodeConnectorFromId(it.next());
        final NodeConnector head = InventoryMapping.nodeConnectorFromId(it.next());
        return new Edge(tail, head);
    }

    public NodeConnector toAdTopologyNodeConnector(final InstanceIdentifier<TerminationPoint> identifier) {
        @SuppressWarnings("unchecked")
        final TerminationPointKey tpKey = ((KeyedInstanceIdentifier<TerminationPoint, TerminationPointKey>)identifier).getKey();

        return InventoryMapping.nodeConnectorFromId(tpKey.getTpId().getValue());
    }

    public org.opendaylight.controller.sal.core.Node toAdTopologyNode(final InstanceIdentifier<Node> identifier) {
        @SuppressWarnings("unchecked")
        final NodeKey nodeKey = ((KeyedInstanceIdentifier<Node, NodeKey>)identifier).getKey();

        return InventoryMapping.nodeFromNodeId(nodeKey.getNodeId().getValue());
    }

    public NodeKey toTopologyNodeKey(final org.opendaylight.controller.sal.core.Node node) {
        return new NodeKey(new NodeId(InventoryMapping.toNodeId(node)));
    }

    public TerminationPointKey toTopologyTerminationPointKey(final NodeConnector nc) {
        return new TerminationPointKey(new TpId(InventoryMapping.toNodeConnectorId(nc)));
    }

    public LinkKey toTopologyLinkKey(final Edge edge) {
        final TerminationPointKey sourceTp = toTopologyTerminationPointKey(edge.getTailNodeConnector());
        final TerminationPointKey destTp = toTopologyTerminationPointKey(edge.getHeadNodeConnector());

        final StringBuilder sb = new StringBuilder();
        sb.append(sourceTp.getTpId().toString());
        sb.append(HEAD_TAIL_STRING);
        sb.append(destTp.getTpId().toString());
        return new LinkKey(new LinkId(sb.toString()));
    }
}
