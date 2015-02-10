/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.util;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.HostBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public final class RemoteDeviceId {

    private final String name;
    private final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier path;
    private final InstanceIdentifier<Node> bindingPath;
    private final NodeKey key;
    private final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier topologyPath;
    private final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> topologyBindingPath;
    private InetSocketAddress address;
    private Host host;

    @Deprecated
    public RemoteDeviceId(final ModuleIdentifier identifier) {
        this(Preconditions.checkNotNull(identifier).getInstanceName());
    }

    public RemoteDeviceId(final ModuleIdentifier identifier, Host host) {
        this(identifier);
        this.host = host;
    }

    public RemoteDeviceId(final ModuleIdentifier identifier, InetSocketAddress address) {
        this(identifier);
        this.address = address;
        this.host = buildHost();
    }

    @Deprecated
    public RemoteDeviceId(final String name) {
        Preconditions.checkNotNull(name);
        this.name = name;
        this.key = new NodeKey(new NodeId(name));
        this.path = createBIPath(name);
        this.bindingPath = createBindingPath(key);
        this.topologyPath = createBIPathForTopology(name);
        this.topologyBindingPath = createBindingPathForTopology(key);
    }

    public RemoteDeviceId(final String name, InetSocketAddress address) {
        this(name);
        this.address = address;
        this.host = buildHost();
    }

    private static InstanceIdentifier<Node> createBindingPath(final NodeKey key) {
        return InstanceIdentifier.builder(Nodes.class).child(Node.class, key).build();
    }

    private static org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier createBIPath(final String name) {
        final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder builder =
                org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder();
        builder.node(Nodes.QNAME).nodeWithKey(Node.QNAME, QName.create(Node.QNAME.getNamespace(), Node.QNAME.getRevision(), "id"), name);

        return builder.build();
    }

    private static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> createBindingPathForTopology(final NodeKey key) {
        final InstanceIdentifier<NetworkTopology> networkTopology = InstanceIdentifier.builder(NetworkTopology.class).build();
        final KeyedInstanceIdentifier<Topology, TopologyKey> topology = networkTopology.child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));
        return topology
                .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey
                                (new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(key.getId().getValue())));
    }

    private static org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier createBIPathForTopology(final String name) {
        final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder builder =
                org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder();
        builder
                .node(NetworkTopology.QNAME)
                .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "topology-id"), TopologyNetconf.QNAME.getLocalName())
                .nodeWithKey(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.QNAME,
                        QName.create(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.QNAME, "node-id"), name);
        return builder.build();
    }

    private Host buildHost() {
        return address.getAddress().getHostAddress() != null
                ? HostBuilder.getDefaultInstance(address.getAddress().getHostAddress())
                : HostBuilder.getDefaultInstance(address.getAddress().getHostName());
    }

    public String getName() {
        return name;
    }

    public InstanceIdentifier<Node> getBindingPath() {
        return bindingPath;
    }

    public org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier getPath() {
        return path;
    }

    public NodeKey getBindingKey() {
        return key;
    }

    public InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getTopologyBindingPath() {
        return topologyBindingPath;
    }

    public YangInstanceIdentifier getTopologyPath() {
        return topologyPath;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public Host getHost() {
        return host;
    }

    @Override
    public String toString() {
        return "RemoteDevice{" + name +'}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RemoteDeviceId)) {
            return false;
        }

        final RemoteDeviceId that = (RemoteDeviceId) o;

        if (!name.equals(that.name)) {
            return false;
        }
        if (!bindingPath.equals(that.bindingPath)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + bindingPath.hashCode();
        return result;
    }
}
