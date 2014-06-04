/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.compatibility.topologymanager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

@SuppressWarnings("all")
public class CompatibleTopologyManager extends ConfigurableLinkManager implements ITopologyManager {
    private AdSalTopologyMapping topologyMapping;
    private TypeSafeDataReader dataReader;

    public TypeSafeDataReader getDataReader() {
        return dataReader;
    }

    public void setDataReader(final TypeSafeDataReader dataReader) {
        this.dataReader = dataReader;
    }

    public AdSalTopologyMapping getTopologyMapping() {
        return topologyMapping;
    }

    public void setTopologyMapping(final AdSalTopologyMapping topologyMapping) {
        this.topologyMapping = topologyMapping;
    }

    @Override
    public Map<Edge,Set<Property>> getEdges() {
        final Topology topology = getDataReader().readOperationalData(topologyMapping.getTopologyPath());
        return this.topologyMapping.toEdgePropertiesMap(topology.getLink());
    }

    @Override
    public Map<org.opendaylight.controller.sal.core.Node, Set<Edge>> getNodeEdges() {
        final Topology topology = getDataReader().readOperationalData(topologyMapping.getTopologyPath());
        final HashMap<org.opendaylight.controller.sal.core.Node, Set<Edge>> ret = new HashMap<>();
        for (final Node node : topology.getNode()) {
            final org.opendaylight.controller.sal.core.Node adNode = topologyMapping.toAdNode(node);
            ret.put(adNode, topologyMapping.toEdges(
                    FluentIterable.from(topology.getLink()).filter(new Predicate<Link>() {
                        @Override
                        public boolean apply(final Link input) {
                            final NodeId nodeId = node.getNodeId();
                            if (nodeId.equals(input.getSource().getSourceNode())) {
                                return true;
                            }
                            if (nodeId.equals(input.getDestination().getDestNode())) {
                                return true;
                            }

                            return false;
                        }
                    })));
        }
        return ret;
    }

    /**
     * Returns true if point is connected to link
     */
    public boolean isInternal(final TerminationPoint point) {
        final Topology topology = getDataReader().readConfigurationData(topologyMapping.getTopologyPath());
        final TpId tpId = point.getKey().getTpId();
        return FluentIterable.from(topology.getLink()).anyMatch(new Predicate<Link>() {
            @Override
            public boolean apply(final Link input) {
                if (tpId.equals(input.getSource().getSourceTp())) {
                    return true;
                }
                if (tpId.equals(input.getDestination().getDestTp())) {
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public Set<NodeConnector> getNodeConnectorWithHost() {
        return null;
    }

    @Override
    public Host getHostAttachedToNodeConnector(final NodeConnector p) {
        final InstanceIdentifier<TerminationPoint> tpPath = topologyMapping.toTerminationPoint(p);
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public List<Host> getHostsAttachedToNodeConnector(final NodeConnector p) {
        final Topology topology = getDataReader().readOperationalData(topologyMapping.getTopologyPath());
        throw new UnsupportedOperationException("Hosts not mapped yet");
    }

    @Override
    public Map<org.opendaylight.controller.sal.core.Node, Set<NodeConnector>> getNodesWithNodeConnectorHost() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public boolean isInternal(final NodeConnector p) {
        final TerminationPoint connector = getDataReader()
                .readConfigurationData(topologyMapping.toTerminationPoint(p));
        return this.isInternal(connector);
    }

    @Override
    public void updateHostLink(final NodeConnector p, final Host h, final UpdateType t, final Set<Property> props) {
        // Update app defined topology
    }

    @Override
    public Status saveConfig() {
        // FIXME: commit configuration
        return null;
    }
}
