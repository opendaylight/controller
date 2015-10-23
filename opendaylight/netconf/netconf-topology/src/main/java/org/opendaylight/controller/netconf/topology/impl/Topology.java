/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology.impl;

import akka.actor.ActorSystem;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.netconf.topology.NetconfTopology;
import org.opendaylight.controller.netconf.topology.NodeManagerCallback;
import org.opendaylight.controller.netconf.topology.NodeManagerCallback.NodeManagerCallbackFactory;
import org.opendaylight.controller.netconf.topology.TopologyManagerCallback;
import org.opendaylight.controller.netconf.topology.TopologyManagerCallback.TopologyManagerCallbackFactory;
import org.opendaylight.controller.netconf.topology.util.BaseTopologyManager;
import org.opendaylight.controller.netconf.topology.util.NodeRoleChangeStrategy;

public class Topology {

    private static final String TOPOLOGY_NETCONF = "topology-netconf";
    private BaseTopologyManager netconfNodeBaseTopologyManager;
    private final DataBroker dataBroker;

    public Topology(final EntityOwnershipService entityOwnershipService, final NetconfTopology topologyDispatcher) {
        dataBroker = topologyDispatcher.getDataBroker();
        final ActorSystem actorSystem = ActorSystem.create("netconf-cluster");

        final NodeManagerCallbackFactory nodeManagerCallbackFactory = new NodeManagerCallbackFactory() {
            @Override
            public NodeManagerCallback create(final String nodeId, final String topologyId,
                    final ActorSystem actorSystem) {
                return new NodeManagerCallbackImpl(nodeId, topologyId, actorSystem, topologyDispatcher,
                        new NodeRoleChangeStrategy(entityOwnershipService, "netconf", nodeId));
            }
        };

        final TopologyManagerCallbackFactory topologyManagerCallbackFactory = new TopologyManagerCallbackFactory() {
            @Override
            public TopologyManagerCallback create(final ActorSystem actorSystem, final DataBroker dataBroker,
                    final String topologyId, final List<String> remotePaths) {
                return new TopologyManagerCallbackImpl(actorSystem, dataBroker, topologyId, remotePaths,
                        nodeManagerCallbackFactory);
            }
        };

//        netconfNodeBaseTopologyManager = new BaseTopologyManager<>(dataBroker, TOPOLOGY_NETCONF,
//                topologyManagerCallbackFactory, new SingleStateAggregatorImpl(), new SalNodeWriter(dataBroker,
//                        TOPOLOGY_NETCONF), new TopologyRoleChangeStrategy(dataBroker, entityOwnershipService,
//                        "netconf", TOPOLOGY_NETCONF));

    }
}
