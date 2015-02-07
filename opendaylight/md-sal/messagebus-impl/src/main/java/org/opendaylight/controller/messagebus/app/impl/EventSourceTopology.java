/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.ioc.Callback;
import org.opendaylight.controller.mdsal.ioc.DataStore;
import org.opendaylight.controller.mdsal.ioc.Inject;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EventSourceTopology {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSourceTopology.class);

    private static final String TOPOLOGY_ID = "EVENT-SOURCE-TOPOLOGY" ;
    private static final TopologyKey TOPOLOGY_KEY = new TopologyKey(new TopologyId(TOPOLOGY_ID));
    private static final LogicalDatastoreType DATASTORE_TYPE = LogicalDatastoreType.OPERATIONAL;

    private static final InstanceIdentifier<Topology> TOPOLOGY_INSTANCE_IDENTIFIER =
            InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, TOPOLOGY_KEY);

    private static final InstanceIdentifier<TopologyTypes1> TOPOLOGY_TYPE_INSTANCE_IDENTIFIER =
            TOPOLOGY_INSTANCE_IDENTIFIER
                    .child(TopologyTypes.class)
                    .augmentation(TopologyTypes1.class);

    private static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                                            .network.topology.rev131021.network.topology.topology.Node> EVENT_SOURCE_TOPOLOGY_PATH =
            InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class)
                    .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                            .network.topology.rev131021.network.topology.topology.Node.class);

    @Inject
    private DataStore dataStore;

    @Callback(Callback.EVENT.MDSAL_READY)
    public void mdsalReady() {
        // TODO: Enforce one time initialization

        TopologyEventSource topologySource = new TopologyEventSourceBuilder().build();
        TopologyTypes1 topologyTypeAugment = new TopologyTypes1Builder().setTopologyEventSource(topologySource).build();

        dataStore.asyncPUT(DATASTORE_TYPE, TOPOLOGY_TYPE_INSTANCE_IDENTIFIER, topologyTypeAugment);
    }

    public void insert(Node node) {
        String nodeId = node.getKey().getId().getValue();
        NodeKey nodeKey = new NodeKey(new NodeId(nodeId));
        InstanceIdentifier<Node1> topologyNodeAugment
                = TOPOLOGY_INSTANCE_IDENTIFIER
                .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                        .network.topology.rev131021.network.topology.topology.Node.class, nodeKey)
                .augmentation(Node1.class);

        Node1 nodeAgument = new Node1Builder().setEventSourceNode(node.getId()).build();
        dataStore.asyncPUT(DATASTORE_TYPE, topologyNodeAugment, nodeAgument);
    }

    // TODO: Should we expose this functioanlity over RPC?
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                .network.topology.rev131021.network.topology.topology.Node> snapshot() {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
             .network.topology.rev131021.network.topology.topology.Node> nodes = new ArrayList<>();

        Topology topology = dataStore.read(DATASTORE_TYPE, TOPOLOGY_INSTANCE_IDENTIFIER);

        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
             .network.topology.rev131021.network.topology.topology.Node node : topology.getNode()) {
            nodes.add(node);
        }

        LOGGER.info("Topology snapshot created. Number of nodes in snapshot {}", nodes.size());
        LOGGER.trace("Topology snapshot {}", nodes);

        return nodes;
    }

    public void registerDataChangeListener(DataChangeListener listener) {
        dataStore.registerDataChangeListener(DATASTORE_TYPE,
                                             EVENT_SOURCE_TOPOLOGY_PATH,
                                             listener,
                                             DataBroker.DataChangeScope.SUBTREE);
    }
}
