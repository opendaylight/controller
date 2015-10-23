/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology.util;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedActorExtension;
import akka.actor.TypedProps;
import akka.dispatch.OnComplete;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.netconf.topology.NodeManager;
import org.opendaylight.controller.netconf.topology.RoleChangeStrategy;
import org.opendaylight.controller.netconf.topology.StateAggregator;
import org.opendaylight.controller.netconf.topology.TopologyManager;
import org.opendaylight.controller.netconf.topology.TopologyManagerCallback;
import org.opendaylight.controller.netconf.topology.TopologyManagerCallback.TopologyManagerCallbackFactory;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.concurrent.impl.Promise.DefaultPromise;

public class BaseTopologyManager implements TopologyManager {

    private static final Logger LOG = LoggerFactory.getLogger(BaseTopologyManager.class);

    private final ActorSystem system;
    private final List<String> remotePaths;

    private final DataBroker dataBroker;
    private final RoleChangeStrategy roleChangeStrategy;
    private final StateAggregator aggregator;

    private final NodeWriter naSalNodeWriter;
    private final String topologyId;
    private final TopologyManagerCallback delegateTopologyHandler;

    private final Map<NodeId, NodeManager> nodes = new HashMap<>();
    private final List<TopologyManager> peers = new ArrayList<>();
    private final int id = new Random().nextInt();

    private boolean isMaster;

    public BaseTopologyManager(final ActorSystem system, final List<String> remotePaths, final DataBroker dataBroker,
            final String topologyId, final TopologyManagerCallbackFactory topologyManagerCallbackFactory,
            final StateAggregator aggregator, final NodeWriter naSalNodeWriter,
            final RoleChangeStrategy roleChangeStrategy) {
        this(system, remotePaths, dataBroker, topologyId, topologyManagerCallbackFactory, aggregator, naSalNodeWriter,
                roleChangeStrategy, false);
    }

    public BaseTopologyManager(final ActorSystem system, final List<String> remotePaths, final DataBroker dataBroker,
            final String topologyId, final TopologyManagerCallbackFactory topologyManagerCallbackFactory,
            final StateAggregator aggregator, final NodeWriter naSalNodeWriter,
            final RoleChangeStrategy roleChangeStrategy, final boolean isMaster) {

        this.system = system;
        this.remotePaths = remotePaths;
        this.dataBroker = dataBroker;
        this.topologyId = topologyId;
        this.delegateTopologyHandler = topologyManagerCallbackFactory.create(system, dataBroker, topologyId,
                remotePaths);
        this.aggregator = aggregator;
        this.naSalNodeWriter = naSalNodeWriter;
        this.roleChangeStrategy = roleChangeStrategy;

        // election has not yet happened
        this.isMaster = isMaster;

        // TODO change to enum, master/slave active/standby
        //        roleChangeStrategy.registerRoleCandidate(this);
        createPeers(system, remotePaths);
        LOG.warn("Base manager started ", +id);
    }

    @Override
    public ListenableFuture<Node> onNodeCreated(final NodeId nodeId, final Node node) {
        LOG.warn("TopologyManager({}) nodeCreated received, nodeid: {}", id, nodeId.getValue());

        final ArrayList<ListenableFuture<Node>> futures = new ArrayList<>();

        if (isMaster) {

            futures.add(delegateTopologyHandler.onNodeCreated(nodeId, node));
            // only master should call connect on peers and aggregate futures
            for (final TopologyManager topologyManager : peers) {
                // add a future into our futures that gets its completion status from the converted scala future
                final SettableFuture<Node> settableFuture = SettableFuture.create();
                futures.add(settableFuture);
                final Future<Node> scalaFuture = topologyManager.remoteNodeCreated(nodeId, node);
                scalaFuture.onComplete(new OnComplete<Node>() {
                    @Override
                    public void onComplete(final Throwable failure, final Node success) throws Throwable {
                        if (failure != null) {
                            settableFuture.setException(failure);
                            return;
                        }

                        settableFuture.set(success);
                    }
                }, TypedActor.context().dispatcher());
            }

            // TODO handle resyncs

            final ListenableFuture<Node> aggregatedFuture = aggregator.combineCreateAttempts(futures);
            Futures.addCallback(aggregatedFuture, new FutureCallback<Node>() {
                @Override
                public void onSuccess(final Node result) {
                    // FIXME make this (writing state data for nodes) optional and customizable
                    // this should be possible with providing your own NodeWriter implementation, maybe rename this interface?
                    LOG.debug("Futures aggregated succesfully");
                    naSalNodeWriter.update(nodeId, result);
                }

                @Override
                public void onFailure(final Throwable t) {
                    // If the combined connection attempt failed, set the node to connection failed
                    LOG.debug("Futures aggregation failed");
                    naSalNodeWriter.update(nodeId, nodes.get(nodeId).getFailedState(nodeId, node));
                    // FIXME disconnect those which succeeded
                    // just issue a delete on delegateTopologyHandler that gets handled on lower level
                }
            });

            //combine peer futures
            return aggregatedFuture;
        }

        // trigger create on this slave
        return delegateTopologyHandler.onNodeCreated(nodeId, node);
    }

    @Override
    public ListenableFuture<Node> onNodeUpdated(final NodeId nodeId, final Node node) {

        final ArrayList<ListenableFuture<Node>> futures = new ArrayList<>();

        // Master needs to trigger nodeUpdated on peers and combine results
        if (isMaster) {
            futures.add(delegateTopologyHandler.onNodeUpdated(nodeId, node));
            for (final TopologyManager topologyManager : peers) {
                // add a future into our futures that gets its completion status from the converted scala future
                final SettableFuture<Node> settableFuture = SettableFuture.create();
                futures.add(settableFuture);
                final Future<Node> scalaFuture = topologyManager.remoteNodeUpdated(nodeId, node);
                scalaFuture.onComplete(new OnComplete<Node>() {
                    @Override
                    public void onComplete(final Throwable failure, final Node success) throws Throwable {
                        if (failure != null) {
                            settableFuture.setException(failure);
                            return;
                        }

                        settableFuture.set(success);
                    }
                }, TypedActor.context().dispatcher());
            }

            final ListenableFuture<Node> aggregatedFuture = aggregator.combineUpdateAttempts(futures);
            Futures.addCallback(aggregatedFuture, new FutureCallback<Node>() {
                @Override
                public void onSuccess(final Node result) {
                    // FIXME make this (writing state data for nodes) optional and customizable
                    // this should be possible with providing your own NodeWriter implementation, maybe rename this interface?
                    naSalNodeWriter.update(nodeId, result);
                }

                @Override
                public void onFailure(final Throwable t) {
                    // If the combined connection attempt failed, set the node to connection failed
                    naSalNodeWriter.update(nodeId, nodes.get(nodeId).getFailedState(nodeId, node));
                    // FIXME disconnect those which succeeded
                    // just issue a delete on delegateTopologyHandler that gets handled on lower level
                }
            });

            //combine peer futures
            return aggregatedFuture;
        }

        // Trigger update on this slave
        return delegateTopologyHandler.onNodeUpdated(nodeId, node);
    }

    @Override
    public ListenableFuture<Void> onNodeDeleted(final NodeId nodeId) {
        final ArrayList<ListenableFuture<Void>> futures = new ArrayList<>();

        // Master needs to trigger delete on peers and combine results
        if (isMaster) {
            futures.add(delegateTopologyHandler.onNodeDeleted(nodeId));
            for (final TopologyManager topologyManager : peers) {
                // add a future into our futures that gets its completion status from the converted scala future
                final SettableFuture<Void> settableFuture = SettableFuture.create();
                futures.add(settableFuture);
                final Future<Void> scalaFuture = topologyManager.remoteNodeDeleted(nodeId);
                scalaFuture.onComplete(new OnComplete<Void>() {
                    @Override
                    public void onComplete(final Throwable failure, final Void success) throws Throwable {
                        if (failure != null) {
                            settableFuture.setException(failure);
                            return;
                        }

                        settableFuture.set(success);
                    }
                }, TypedActor.context().dispatcher());
            }

            final ListenableFuture<Void> aggregatedFuture = aggregator.combineDeleteAttempts(futures);
            Futures.addCallback(aggregatedFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    naSalNodeWriter.delete(nodeId);
                }

                @Override
                public void onFailure(final Throwable t) {
                    // FIXME unable to disconnect all the connections, what do we do now ?
                }
            });

            return aggregatedFuture;
        }

        // Trigger delete
        return delegateTopologyHandler.onNodeDeleted(nodeId);
    }

    @Nonnull
    @Override
    public ListenableFuture<Node> getCurrentStatusForNode(@Nonnull final NodeId nodeId) {
        return null;
    }

    @Override
    public void onRoleChanged(final RoleChangeDTO roleChangeDTO) {
        // TODO if we are master start watching peers and react to failure, implement akka Reciever
        isMaster = roleChangeDTO.isOwner();
        delegateTopologyHandler.onRoleChanged(roleChangeDTO);
    }

    @Override
    public void notifyNodeStatusChange(final NodeId nodeId) {
        if (isMaster) {
            // grab status from all peers and aggregate
        }
        for (final TopologyManager manager : peers) {
            manager.notifyNodeStatusChange(nodeId);
        }
    }

    @Override
    public boolean hasAllPeersUp() {
        LOG.debug("Peers needed: {} Peers up: {}", remotePaths.size(), peers.size());
        return peers.size() == remotePaths.size();
    }

    @Override
    public Future<Node> remoteNodeCreated(final NodeId nodeId, final Node node) {
        final ListenableFuture<Node> nodeListenableFuture = onNodeCreated(nodeId, node);
        final DefaultPromise<Node> promise = new DefaultPromise<>();
        Futures.addCallback(nodeListenableFuture, new FutureCallback<Node>() {
            @Override
            public void onSuccess(final Node result) {
                promise.success(result);
            }

            @Override
            public void onFailure(final Throwable t) {
                promise.failure(t);
            }
        });

        return promise.future();
    }

    @Override
    public Future<Node> remoteNodeUpdated(final NodeId nodeId, final Node node) {
        final ListenableFuture<Node> nodeListenableFuture = onNodeUpdated(nodeId, node);
        final DefaultPromise<Node> promise = new DefaultPromise<>();
        Futures.addCallback(nodeListenableFuture, new FutureCallback<Node>() {
            @Override
            public void onSuccess(final Node result) {
                promise.success(result);
            }

            @Override
            public void onFailure(final Throwable t) {
                promise.failure(t);
            }
        });
        return promise.future();
    }

    @Override
    public Future<Void> remoteNodeDeleted(final NodeId nodeId) {
        final ListenableFuture<Void> listenableFuture = onNodeDeleted(nodeId);
        final DefaultPromise<Void> promise = new DefaultPromise<>();
        Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                promise.success(null);
            }

            @Override
            public void onFailure(final Throwable t) {
                promise.failure(t);
            }
        });

        return promise.future();
    }

    @Override
    public Future<Node> remoteGetCurrentStatusForNode(final NodeId nodeId) {
        return null;
    }

    private List<TopologyManager> createPeers(final ActorSystem system, final List<String> remotePaths) {
        final TypedActorExtension extension = TypedActor.get(system);
        final List<TopologyManager> peers = new ArrayList<>();

        for (final String path : remotePaths) {
            // this needs to be async, otherwise it introduces deadlock when multiple managers
            // want to identify peers at the same time
            LOG.warn("Scheduling remote actor for path: {}", path);
            system.scheduler().scheduleOnce(Duration.create(1L, TimeUnit.SECONDS),
                    createSchedulerRunnable(system, extension, path), system.dispatcher());
        }
        return peers;
    }

    @Override
    public void onReceive(final Object o, final ActorRef actorRef) {

    }

    private Runnable createSchedulerRunnable(final ActorSystem actorSystem, final TypedActorExtension extension,
            final String path) {
        return new Runnable() {
            @Override
            public void run() {
                final Future<ActorRef> refFuture = actorSystem.actorSelection(path).resolveOne(
                        FiniteDuration.create(10L, TimeUnit.SECONDS));
                refFuture.onComplete(new OnComplete<ActorRef>() {
                    @Override
                    public void onComplete(final Throwable throwable, final ActorRef actorRef) throws Throwable {
                        if (throwable != null) {
                            LOG.warn("Unable to resolve actor for path: {} .Rescheduling..", path, throwable);
                            actorSystem.scheduler().scheduleOnce(Duration.create(5L, TimeUnit.SECONDS),
                                    createSchedulerRunnable(actorSystem, extension, path), system.dispatcher());
                            return;
                        }

                        LOG.debug("Actor ref for path {} resolved", path);
                        final TopologyManager peer = extension.typedActorOf(new TypedProps<>(TopologyManager.class,
                                BaseTopologyManager.class), actorRef);
                        // resync this actor
                        peers.add(peer);
                    }
                }, system.dispatcher());
            }
        };
    }
}
