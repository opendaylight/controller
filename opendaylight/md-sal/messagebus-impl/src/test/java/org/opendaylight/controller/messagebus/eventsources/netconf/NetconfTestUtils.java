/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.eventsources.netconf;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.Streams;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.StreamsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.Stream;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.StreamBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.DomainName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class NetconfTestUtils {

    public static final String notification_capability_prefix = "(urn:ietf:params:xml:ns:netconf:notification";

    private NetconfTestUtils() {
    }

    public static Node getNetconfNode(final String nodeIdent,final String hostName,final ConnectionStatus cs, final String notificationCapabilityPrefix){

        final DomainName dn = new DomainName(hostName);
        final Host host = new Host(dn);

        final List<String> avCapList = new ArrayList<>();
        avCapList.add(notificationCapabilityPrefix +"_availableCapabilityString1");
        final AvailableCapabilities avCaps = new AvailableCapabilitiesBuilder().setAvailableCapability(avCapList).build();
        final NetconfNode nn = new NetconfNodeBuilder()
                .setConnectionStatus(cs)
                .setHost(host)
                .setAvailableCapabilities(avCaps)
                .build();

        final NodeId nodeId = new NodeId(nodeIdent);
        final NodeKey nk = new NodeKey(nodeId);
        final NodeBuilder nb = new NodeBuilder();
        nb.setKey(nk);

        nb.addAugmentation(NetconfNode.class, nn);
        return nb.build();
    }

    public static Node getNode(final String nodeIdent){
         final NodeId nodeId = new NodeId(nodeIdent);
         final NodeKey nk = new NodeKey(nodeId);
         final NodeBuilder nb = new NodeBuilder();
         nb.setKey(nk);
         return nb.build();
    }

    public static InstanceIdentifier<Node> getInstanceIdentifier(final Node node){
        final TopologyKey NETCONF_TOPOLOGY_KEY = new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName()));
        final InstanceIdentifier<Node> nodeII = InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, NETCONF_TOPOLOGY_KEY)
                    .child(Node.class, node.getKey());
        return nodeII;
    }

    public static Optional<Streams> getAvailableStream(final String Name, final boolean replaySupport){
        final Stream stream = new StreamBuilder()
                .setName(new StreamNameType(Name))
                .setReplaySupport(replaySupport)
                .build();
        final List<Stream> streamList = new ArrayList<>();
        streamList.add(stream);
        final Streams streams = new StreamsBuilder().setStream(streamList).build();
        return Optional.of(streams);
    }

}
