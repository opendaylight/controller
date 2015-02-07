/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.DataStore;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.Node1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.topology.event.source.type.TopologyEventSource;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.topology.event.source.type.TopologyEventSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EventSourceTopology {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSourceTopology.class);

    private static final String topologyId = "EVENT-SOURCE-TOPOLOGY" ;
    private static final TopologyKey topologyKey = new TopologyKey(new TopologyId(topologyId));
    private static final LogicalDatastoreType datastoreType = LogicalDatastoreType.OPERATIONAL;

    private static final InstanceIdentifier<Topology> topologyInstanceIdentifier =
            InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, topologyKey);

    private static final InstanceIdentifier<TopologyTypes1> topologyTypeInstanceIdentifier =
            topologyInstanceIdentifier
                    .child(TopologyTypes.class)
                    .augmentation(TopologyTypes1.class);

    private static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                                            .network.topology.rev131021.network.topology.topology.Node> eventSourceTopologyPath =
            InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class)
                    .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                            .network.topology.rev131021.network.topology.topology.Node.class);

    private final Map<DataChangeListener, ListenerRegistration<DataChangeListener>> registrations =
            new ConcurrentHashMap<>();

    private final DataStore dataStore;

    public EventSourceTopology(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void mdsalReady() {
        TopologyEventSource topologySource = new TopologyEventSourceBuilder().build();
        TopologyTypes1 topologyTypeAugment = new TopologyTypes1Builder().setTopologyEventSource(topologySource).build();

        dataStore.asyncPUT(datastoreType, topologyTypeInstanceIdentifier, topologyTypeAugment);
    }

    public void insert(Node node) {
        String nodeId = node.getKey().getId().getValue();
        NodeKey nodeKey = new NodeKey(new NodeId(nodeId));
        InstanceIdentifier<Node1> topologyNodeAugment
                = topologyInstanceIdentifier
                .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                        .network.topology.rev131021.network.topology.topology.Node.class, nodeKey)
                .augmentation(Node1.class);

        Node1 nodeAgument = new Node1Builder().setEventSourceNode(node.getId()).build();
        dataStore.asyncPUT(datastoreType, topologyNodeAugment, nodeAgument);
    }

    // TODO: Should we expose this functioanlity over RPC?
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                .network.topology.rev131021.network.topology.topology.Node> snapshot() {
        Topology topology = dataStore.read(datastoreType, topologyInstanceIdentifier);
        return topology.getNode();
    }

    public void registerDataChangeListener(DataChangeListener listener) {
        ListenerRegistration<DataChangeListener> listenerRegistration = dataStore.registerDataChangeListener(datastoreType,
                eventSourceTopologyPath,
                listener,
                DataBroker.DataChangeScope.SUBTREE);

        registrations.put(listener, listenerRegistration);
    }
}
