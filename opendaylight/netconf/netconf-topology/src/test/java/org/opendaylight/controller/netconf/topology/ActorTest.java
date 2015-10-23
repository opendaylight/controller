/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology;

import static com.jayway.awaitility.Awaitility.await;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedActorExtension;
import akka.actor.TypedProps;
import akka.japi.Creator;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.netconf.topology.NodeManagerCallback.NodeManagerCallbackFactory;
import org.opendaylight.controller.netconf.topology.TopologyManagerCallback.TopologyManagerCallbackFactory;
import org.opendaylight.controller.netconf.topology.impl.NodeManagerCallbackImpl;
import org.opendaylight.controller.netconf.topology.impl.TopologyManagerCallbackImpl;
import org.opendaylight.controller.netconf.topology.util.BaseTopologyManager;
import org.opendaylight.controller.netconf.topology.util.LoggingSalNodeWriter;
import org.opendaylight.controller.netconf.topology.util.NodeRoleChangeStrategy;
import org.opendaylight.controller.netconf.topology.util.NoopRoleChangeStrategy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActorTest {

    private static final Logger LOG = LoggerFactory.getLogger(ActorTest.class);

    private static final String TOPOLOGY_NETCONF = "TopologyNetconf";

    @Mock
    private NetconfTopology topologyDispatcher;

    @Mock
    private EntityOwnershipService entityOwnershipService;

    @Mock
    private DataBroker dataBroker;

    private static final String PATH_MASTER = "akka.tcp://NetconfNode@127.0.0.1:2552/user/TopologyNetconf";
    private static final String PATH_SLAVE1 = "akka.tcp://NetconfNode@127.0.0.1:2553/user/TopologyNetconf";
    private static final String PATH_SLAVE2 = "akka.tcp://NetconfNode@127.0.0.1:2554/user/TopologyNetconf";

    private static final List<String> PATHS_MASTER = Lists.newArrayList(PATH_SLAVE1, PATH_SLAVE2);
    private static final List<String> PATHS_SLAVE1 = Lists.newArrayList(PATH_MASTER, PATH_SLAVE2);
    private static final List<String> PATHS_SLAVE2 = Lists.newArrayList(PATH_MASTER, PATH_SLAVE1);

    private static final ActorSystem ACTOR_SYSTEM = ActorSystem.create("NetconfNode", ConfigFactory.load("netconf-node1"));
    private static final ActorSystem ACTOR_SYSTEM_SLAVE1 = ActorSystem.create("NetconfNode", ConfigFactory.load("netconf-node2"));
    private static final ActorSystem ACTOR_SYSTEM_SLAVE2 = ActorSystem.create("NetconfNode", ConfigFactory.load("netconf-node3"));

    @Test
    public void testRealActors() throws Exception {
        // load from config
        final TopologyManager master = createNode(ACTOR_SYSTEM, TOPOLOGY_NETCONF, PATHS_MASTER, true, createRealTopoTestingNodeCallbackFactory());
        final TopologyManager slave1 = createNode(ACTOR_SYSTEM_SLAVE1, TOPOLOGY_NETCONF, PATHS_SLAVE1, false, createRealTopoTestingNodeCallbackFactory());
        final TopologyManager slave2 = createNode(ACTOR_SYSTEM_SLAVE2, TOPOLOGY_NETCONF, PATHS_SLAVE2, false, createRealTopoTestingNodeCallbackFactory());

        await().atMost(10L, TimeUnit.SECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return master.hasAllPeersUp();
            }
        });
        final List<ListenableFuture<Node>> futures = new ArrayList<>();
        for (int i = 0; i <= 1; i++) {
            final String nodeid = "testing-node" + i;
            final Node testingNode = new NodeBuilder()
                    .setNodeId(new NodeId(nodeid))
                    .addAugmentation(NetconfNode.class,
                            new NetconfNodeBuilder()
                                    .setHost(new Host(new IpAddress(new Ipv4Address("127.0.0.1"))))
                                    .setPort(new PortNumber(10000 + i))
                                    .build())
                    .build();
            final ListenableFuture<Node> nodeListenableFuture = master.nodeCreated(new NodeId(nodeid), testingNode);
            futures.add(nodeListenableFuture);
            Futures.addCallback(nodeListenableFuture, new FutureCallback<Node>() {
                @Override
                public void onSuccess(final Node result) {
                    LOG.warn("Node {} created succesfully on all nodes", result.getNodeId().getValue());
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.warn("Node creation failed. ", t);
                }
            });
        }

        for (int i = 0; i <= 1; i++) {
            final String nodeid = "testing-node" + i;
            final Node testingNode = new NodeBuilder()
                    .setNodeId(new NodeId(nodeid))
                    .addAugmentation(NetconfNode.class,
                            new NetconfNodeBuilder()
                                    .setHost(new Host(new IpAddress(new Ipv4Address("127.0.0.1"))))
                                    .setPort(new PortNumber(10000 + i))
                                    .build())
                    .build();
            final ListenableFuture<Node> nodeListenableFuture = master.nodeUpdated(new NodeId(nodeid), testingNode);
            futures.add(nodeListenableFuture);
            Futures.addCallback(nodeListenableFuture, new FutureCallback<Node>() {
                @Override
                public void onSuccess(final Node result) {
                    LOG.warn("Node {} updated succesfully on all nodes", result.getNodeId().getValue());
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.warn("Node update failed. ", t);
                }
            });
        }


        final List<ListenableFuture<Void>> deleteFutures = new ArrayList<>();
        for (int i = 0; i <= 1; i++) {
            final String nodeid = "testing-node" + i;
            final ListenableFuture<Void> nodeListenableFuture = master.nodeDeleted(new NodeId(nodeid));
            deleteFutures.add(nodeListenableFuture);
            Futures.addCallback(nodeListenableFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    LOG.warn("Node {} succesfully deleted on all nodes", nodeid);
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.warn("Node delete failed. ", t);
                }
            });

        }
        LOG.warn("All tasks submitted");
        Futures.allAsList(futures).get();
        Futures.allAsList(deleteFutures).get();

    }

    private TopologyManager createNode(final ActorSystem actorSystem, final String topologyId, final List<String> remotePaths, final boolean isMaster,
                                      final TopologyManagerCallbackFactory topologyManagerCallbackFactory) {

        final TypedActorExtension typedActorExtension = TypedActor.get(actorSystem);
        return typedActorExtension.typedActorOf(new TypedProps<>(TopologyManager.class, new Creator<BaseTopologyManager>() {
            @Override
            public BaseTopologyManager create() throws Exception {
                return new BaseTopologyManager(actorSystem,
                        remotePaths,
                        dataBroker,
                        topologyId,
                        topologyManagerCallbackFactory,
                        new OnlySuccessStateAggregator(),
                        new LoggingSalNodeWriter(),
                        new NoopRoleChangeStrategy(),
                        isMaster);
            }
        }), TOPOLOGY_NETCONF);
    }

    private TopologyManagerCallbackFactory createRealTopoTestingNodeCallbackFactory() {
        final NodeManagerCallbackFactory nodeManagerCallbackFactory = new NodeManagerCallbackFactory() {
            @Override
            public NodeManagerCallback create(final String nodeId, final String topologyId, final ActorSystem actorSystem) {
                return new LoggingNodeManagerCallback();
            }
        };

        return new TopologyManagerCallbackFactory() {
            @Override
            public TopologyManagerCallback create(final ActorSystem actorSystem, final DataBroker dataBroker, final String topologyId, final List<String> remotePaths) {
                return new TopologyManagerCallbackImpl(actorSystem, dataBroker, topologyId, remotePaths, nodeManagerCallbackFactory, new LoggingSalNodeWriter());
            }
        };
    }

    private TopologyManagerCallbackFactory createRealTopoCallbackFactory() {
        final NodeManagerCallbackFactory nodeManagerCallbackFactory = new NodeManagerCallbackFactory() {
            @Override
            public NodeManagerCallback create(final String nodeId, final String topologyId, final ActorSystem actorSystem) {
                return new NodeManagerCallbackImpl(
                        nodeId, topologyId, actorSystem, topologyDispatcher,  new NodeRoleChangeStrategy(entityOwnershipService, "netconf", nodeId));
            }
        };

        return new TopologyManagerCallbackFactory() {
            @Override
            public TopologyManagerCallback create(final ActorSystem actorSystem, final DataBroker dataBroker, final String topologyId, final List<String> remotePaths) {
                return new TopologyManagerCallbackImpl(actorSystem, dataBroker, topologyId, remotePaths, nodeManagerCallbackFactory);
            }
        };
    }

    private TopologyManagerCallbackFactory createTestingTopoCallbackFactory() {
        return new TopologyManagerCallbackFactory() {
            @Override
            public TopologyManagerCallback create(final ActorSystem actorSystem, final DataBroker dataBroker, final String topologyId, final List<String> remotePaths) {
                return new TestingTopologyManagerCallback();
            }
        };
    }

    public static class LoggingNodeManagerCallback implements NodeManagerCallback {

        @Nonnull
        @Override
        public Node getInitialState(@Nonnull final NodeId nodeId, @Nonnull final Node configNode) {
            final NetconfNode augmentation = configNode.getAugmentation(NetconfNode.class);
            return new NodeBuilder()
                    .setNodeId(nodeId)
                    .addAugmentation(NetconfNode.class,
                            new NetconfNodeBuilder()
                                    .setConnectionStatus(ConnectionStatus.Connecting)
                                    .setHost(augmentation.getHost())
                                    .setPort(augmentation.getPort())
                                    .build())
                    .build();
        }

        @Nonnull
        @Override
        public Node getFailedState(@Nonnull final NodeId nodeId, @Nonnull final Node configNode) {
            return new NodeBuilder()
                    .setNodeId(nodeId)
                    .addAugmentation(NetconfNode.class,
                            new NetconfNodeBuilder()
                                    .setConnectionStatus(ConnectionStatus.UnableToConnect)
                                    .build())
                    .build();
        }

        @Nonnull
        @Override
        public ListenableFuture<Node> nodeCreated(@Nonnull final NodeId nodeId, @Nonnull final Node configNode) {
            LOG.debug("Creating node {} with config {}", nodeId, configNode);
            final NetconfNode augmentation = configNode.getAugmentation(NetconfNode.class);
            return Futures.immediateFuture(new NodeBuilder()
                    .setNodeId(nodeId)
                    .addAugmentation(NetconfNode.class,
                            new NetconfNodeBuilder()
                                    .setConnectionStatus(ConnectionStatus.Connected)
                                    .setHost(augmentation.getHost())
                                    .setPort(augmentation.getPort())
                                    .build())
                    .build());
        }

        @Nonnull
        @Override
        public ListenableFuture<Node> nodeUpdated(@Nonnull final NodeId nodeId, @Nonnull final Node configNode) {
            LOG.debug("Updating node {} with config {}", nodeId, configNode);
            final NetconfNode augmentation = configNode.getAugmentation(NetconfNode.class);
            return Futures.immediateFuture(new NodeBuilder()
                    .setNodeId(nodeId)
                    .addAugmentation(NetconfNode.class,
                            new NetconfNodeBuilder()
                                    .setConnectionStatus(ConnectionStatus.Connected)
                                    .setHost(augmentation.getHost())
                                    .setPort(augmentation.getPort())
                                    .build())
                    .build());
        }

        @Nonnull
        @Override
        public ListenableFuture<Void> nodeDeleted(@Nonnull final NodeId nodeId) {
            LOG.debug("Deleting node {}", nodeId);
            return Futures.immediateFuture(null);
        }

        @Nonnull
        @Override
        public ListenableFuture<Node> getCurrentStatusForNode(@Nonnull final NodeId nodeId) {
            return null;
        }

        @Override
        public void onRoleChanged(final RoleChangeDTO roleChangeDTO) {

        }

        @Override
        public void onReceive(final Object o, final ActorRef actorRef) {

        }
    }

    public static class TestingTopologyManagerCallback implements TopologyManagerCallback{

        public TestingTopologyManagerCallback() {

        }

        @Override
        public ListenableFuture<Node> nodeCreated(final NodeId nodeId, final Node node) {
            LOG.warn("Actor system that called this: {}", TypedActor.context().system().settings().toString());
            return Futures.immediateFuture(new NodeBuilder()
                    .setNodeId(nodeId)
                    .addAugmentation(NetconfNode.class,
                            new NetconfNodeBuilder()
                                    .setConnectionStatus(ConnectionStatus.Connected)
                                    .setHost(new Host(new IpAddress(new Ipv4Address("127.0.0.1"))))
                                    .setPort(new PortNumber(2555))
                                    .build())
                    .build());
        }

        @Override
        public ListenableFuture<Node> nodeUpdated(final NodeId nodeId, final Node node) {
            LOG.warn("Actor system that called this: {}", TypedActor.context().system().settings().toString());
            LOG.debug("Update called on node {}, with config {}", nodeId.getValue(), node);
            return Futures.immediateFuture(new NodeBuilder()
                    .setNodeId(nodeId)
                    .addAugmentation(NetconfNode.class,
                            new NetconfNodeBuilder()
                                    .setConnectionStatus(ConnectionStatus.Connected)
                                    .setHost(new Host(new IpAddress(new Ipv4Address("127.0.0.1"))))
                                    .setPort(new PortNumber(65535))
                                    .build())
                    .build());
        }

        @Override
        public ListenableFuture<Void> nodeDeleted(final NodeId nodeId) {
            LOG.debug("Delete called on node {}", nodeId.getValue());
            return Futures.immediateFuture(null);
        }

        @Nonnull
        @Override
        public ListenableFuture<Node> getCurrentStatusForNode(@Nonnull final NodeId nodeId) {
            return null;
        }

        @Override
        public void onRoleChanged(final RoleChangeDTO roleChangeDTO) {

        }

        @Override
        public void onReceive(final Object o, final ActorRef actorRef) {

        }
    }

    public class OnlySuccessStateAggregator implements StateAggregator {

        @Override
        public ListenableFuture<Node> combineCreateAttempts(final List<ListenableFuture<Node>> stateFutures) {
            final SettableFuture<Node> future = SettableFuture.create();
            final ListenableFuture<List<Node>> allAsList = Futures.allAsList(stateFutures);
            Futures.addCallback(allAsList, new FutureCallback<List<Node>>() {
                @Override
                public void onSuccess(final List<Node> result) {
                    for (int i = 0; i < result.size() - 1; i++) {
                        if (!result.get(i).equals(result.get(i + 1))) {
                            future.setException(new IllegalStateException("Create futures have different result"));
                        }
                    }
                    future.set(result.get(0));
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("One of the combined create attempts failed {}", t);
                    future.setException(t);
                }
            }, TypedActor.context().dispatcher());

            return future;
        }

        @Override
        public ListenableFuture<Node> combineUpdateAttempts(final List<ListenableFuture<Node>> stateFutures) {
            final SettableFuture<Node> future = SettableFuture.create();
            final ListenableFuture<List<Node>> allAsList = Futures.allAsList(stateFutures);
            Futures.addCallback(allAsList, new FutureCallback<List<Node>>() {
                @Override
                public void onSuccess(final List<Node> result) {
                    for (int i = 0; i < result.size() - 1; i++) {
                        if (!result.get(i).equals(result.get(i + 1))) {
                            future.setException(new IllegalStateException("Update futures have different result"));
                        }
                    }
                    future.set(result.get(0));
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("One of the combined update attempts failed {}", t);
                    future.setException(t);
                }
            });
            return future;
        }

        @Override
        public ListenableFuture<Void> combineDeleteAttempts(final List<ListenableFuture<Void>> stateFutures) {
            final SettableFuture<Void> future = SettableFuture.create();
            final ListenableFuture<List<Void>> allAsList = Futures.allAsList(stateFutures);
            Futures.addCallback(allAsList, new FutureCallback<List<Void>>() {
                @Override
                public void onSuccess(final List<Void> result) {
                    future.set(null);
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("One of the combined delete attempts failed {}", t);
                    future.setException(t);
                }
            });
            return future;
        }
    }
}
