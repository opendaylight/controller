/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.topology;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader;
import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public final class TopologyMapping {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyMapping.class);
    private final static Pattern NUMBERS_ONLY = Pattern.compile("[0-9]+");

    private TopologyMapping() {
        throw new UnsupportedOperationException("Utility class. Instantiation is not allowed.");
    }

    public static List<TopoEdgeUpdate> toADEdgeUpdates(final Topology topology,final TypeSafeDataReader reader) {
        final List<TopoEdgeUpdate> result = new CopyOnWriteArrayList<>();
        return FluentIterable.from(topology.getLink()).transform(
                new Function<Link, TopoEdgeUpdate>() {
                    @Override
                    public TopoEdgeUpdate apply(final Link input) {
                        try {
                            return toTopoEdgeUpdate(toAdEdge(input, topology), reader);
                        } catch (ConstructionException e) {
                            throw new IllegalArgumentException(String.format("Failed to construct edge update for {}", input), e);
                        }
                    }}
                ).copyInto(result);
    }

    public static Edge toAdEdge(final Link link, final Topology topology) throws ConstructionException {
        final NodeConnector adSrc = toADNodeConnector(link.getSource().getSourceTp(), link.getSource().getSourceNode());
        final NodeConnector adDst = toADNodeConnector(link.getDestination().getDestTp(), link.getDestination().getDestNode());
        return new Edge(adSrc, adDst);
    }

    public static TopoEdgeUpdate toTopoEdgeUpdate(final Edge e, final TypeSafeDataReader reader) {
        return toTopoEdgeUpdate(e, UpdateType.ADDED, reader);
    }

    public static TopoEdgeUpdate toTopoEdgeUpdate(final Edge e,final UpdateType type,final TypeSafeDataReader reader) {
        return new TopoEdgeUpdate(e, toAdEdgeProperties(e, reader), type);
    }

    public static Set<Property> toAdEdgeProperties(final Edge e,final TypeSafeDataReader reader) {
        final NodeConnectorRef ncref = NodeMapping.toNodeConnectorRef(e.getTailNodeConnector());
        if(ncref == null) {
            LOG.debug("Edge {} ncref {}",e,ncref);
            return null;
        }

        @SuppressWarnings("unchecked")
        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector> ncInstanceId =
        (InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector>) ncref.getValue();
        if(ncInstanceId == null) {
            LOG.debug("Edge {} ncref {}",e,ncref);
            return null;
        }

        final  org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector nc = reader.readOperationalData(ncInstanceId);
        if(nc == null) {
            return null;
        }
        return NodeMapping.toADNodeConnectorProperties(nc);
    }

    public static String toADNodeId(final NodeId nodeId) {
        return nodeId.getValue().replaceFirst("^.*:", "");
    }

    public static NodeConnector toADNodeConnector(final TpId source, final NodeId nodeId) throws ConstructionException {
        checkNotNull(source);
        String nodeConnectorIdStripped = toADNodeConnectorId(source);
        if (NUMBERS_ONLY.matcher(nodeConnectorIdStripped).matches()) {
            return new NodeConnector(NodeConnectorIDType.OPENFLOW, Short.valueOf(nodeConnectorIdStripped), toADNode(nodeId));
        }
        LOG.debug("NodeConnectorId does not match openflow id type, using " + NodeMapping.MD_SAL_TYPE +  "instead");
        NodeConnectorIDType.registerIDType(NodeMapping.MD_SAL_TYPE, String.class, NodeMapping.MD_SAL_TYPE);
        return new NodeConnector(NodeMapping.MD_SAL_TYPE, nodeConnectorIdStripped, toADNode(nodeId));
    }

    public static String toADNodeConnectorId(final TpId nodeConnectorId) {
        return nodeConnectorId.getValue().replaceFirst("^.*:", "");
    }

    public static Node toADNode(final NodeId nodeId) throws ConstructionException {
        checkNotNull(nodeId);
        String nodeIdStripped = toADNodeId(nodeId);
        if (NUMBERS_ONLY.matcher(nodeIdStripped).matches()) {
            return new Node(NodeIDType.OPENFLOW, Long.valueOf(nodeIdStripped));
        }
        LOG.debug("NodeId does not match openflow id type, using " + NodeMapping.MD_SAL_TYPE +  "instead");
        NodeIDType.registerIDType(NodeMapping.MD_SAL_TYPE, String.class);
        return new Node(NodeMapping.MD_SAL_TYPE, nodeId.getValue());
    }
}
