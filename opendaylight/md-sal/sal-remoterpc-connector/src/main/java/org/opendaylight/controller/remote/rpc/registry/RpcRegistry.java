/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.japi.Creator;
import akka.japi.Option;
import akka.japi.Pair;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.AddOrUpdateRoutes;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.FindRouters;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.RemoveRoutes;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.SetLocalRouter;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStore;
import org.opendaylight.controller.remote.rpc.registry.mbeans.RemoteRpcRegistryMXBean;
import org.opendaylight.controller.remote.rpc.registry.mbeans.RemoteRpcRegistryMXBeanImpl;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.api.RpcRouter.RouteIdentifier;
import scala.concurrent.duration.FiniteDuration;

/**
 * Registry to look up cluster nodes that have registered for a given rpc.
 * <p/>
 * It uses {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStore} to maintain this
 * cluster wide information.
 */
public class RpcRegistry extends BucketStore<RoutingTable> {
    private final Set<Runnable> routesUpdatedCallbacks = new HashSet<>();
    private final FiniteDuration findRouterTimeout;

    public RpcRegistry(RemoteRpcProviderConfig config) {
        super(config);
        getLocalBucket().setData(new RoutingTable());
        findRouterTimeout = getConfig().getGossipTickInterval().$times(10);
    }

    public static Props props(RemoteRpcProviderConfig config) {
        return Props.create(new RpcRegistryCreator(config));
    }

    @Override
    protected void handleReceive(Object message) throws Exception {
        //TODO: if sender is remote, reject message

        if (message instanceof SetLocalRouter) {
            receiveSetLocalRouter((SetLocalRouter) message);
        } else if (message instanceof AddOrUpdateRoutes) {
            receiveAddRoutes((AddOrUpdateRoutes) message);
        } else if (message instanceof RemoveRoutes) {
            receiveRemoveRoutes((RemoveRoutes) message);
        } else if (message instanceof Messages.FindRouters) {
            receiveGetRouter((FindRouters) message);
        } else if (message instanceof Runnable) {
            ((Runnable)message).run();
        } else {
            super.handleReceive(message);
        }
    }

    /**
     * Register's rpc broker
     *
     * @param message contains {@link akka.actor.ActorRef} for rpc broker
     */
    private void receiveSetLocalRouter(SetLocalRouter message) {
        getLocalBucket().getData().setRouter(message.getRouter());
    }

    /**
     * @param msg
     */
    private void receiveAddRoutes(AddOrUpdateRoutes msg) {

        log.debug("AddOrUpdateRoutes: {}", msg.getRouteIdentifiers());

        RoutingTable table = getLocalBucket().getData().copy();
        for(RpcRouter.RouteIdentifier<?, ?, ?> routeId : msg.getRouteIdentifiers()) {
            table.addRoute(routeId);
        }

        updateLocalBucket(table);

        onBucketsUpdated();
    }

    /**
     * @param msg contains list of route ids to remove
     */
    private void receiveRemoveRoutes(RemoveRoutes msg) {

        RoutingTable table = getLocalBucket().getData().copy();
        for (RpcRouter.RouteIdentifier<?, ?, ?> routeId : msg.getRouteIdentifiers()) {
            table.removeRoute(routeId);
        }

        updateLocalBucket(table);
    }

    /**
     * Finds routers for the given rpc.
     *
     * @param findRouters
     */
    private void receiveGetRouter(final FindRouters findRouters) {
        log.debug("receiveGetRouter for {}", findRouters.getRouteIdentifier());

        final ActorRef sender = getSender();
        if(!findRouters(findRouters, sender)) {
            log.debug("No routers found for {} - scheduling {} ms timer", findRouters.getRouteIdentifier(),
                    findRouterTimeout.toMillis());

            final AtomicReference<Cancellable> timer = new AtomicReference<>();
            final Runnable routesUpdatedRunnable = new Runnable() {
                @Override
                public void run() {
                    if(findRouters(findRouters, sender)) {
                        routesUpdatedCallbacks.remove(this);
                        timer.get().cancel();
                    }
                }
            };

            routesUpdatedCallbacks.add(routesUpdatedRunnable);

            Runnable timerRunnable = new Runnable() {
                @Override
                public void run() {
                    log.warn("Timed out finding routers for {}", findRouters.getRouteIdentifier());

                    routesUpdatedCallbacks.remove(routesUpdatedRunnable);
                    sender.tell(new Messages.FindRoutersReply(
                            Collections.<Pair<ActorRef, Long>>emptyList()), self());
                }
            };

            timer.set(getContext().system().scheduler().scheduleOnce(findRouterTimeout, self(), timerRunnable,
                    getContext().dispatcher(), self()));
        }
    }

    private boolean findRouters(FindRouters findRouters, ActorRef sender) {
        List<Pair<ActorRef, Long>> routers = new ArrayList<>();

        RouteIdentifier<?, ?, ?> routeId = findRouters.getRouteIdentifier();
        findRoutes(getLocalBucket().getData(), routeId, routers);

        for(Bucket<RoutingTable> bucket : getRemoteBuckets().values()) {
            findRoutes(bucket.getData(), routeId, routers);
        }

        log.debug("Found {} routers for {}", routers.size(), findRouters.getRouteIdentifier());

        boolean foundRouters = !routers.isEmpty();
        if(foundRouters) {
            sender.tell(new Messages.FindRoutersReply(routers), getSelf());
        }

        return foundRouters;
    }

    private void findRoutes(RoutingTable table, RpcRouter.RouteIdentifier<?, ?, ?> routeId,
            List<Pair<ActorRef, Long>> routers) {
        if (table == null) {
            return;
        }

        Option<Pair<ActorRef, Long>> routerWithUpdateTime = table.getRouterFor(routeId);
        if(!routerWithUpdateTime.isEmpty()) {
            routers.add(routerWithUpdateTime.get());
        }
    }

    @Override
    protected void onBucketsUpdated() {
        if(routesUpdatedCallbacks.isEmpty()) {
            return;
        }

        for(Runnable callBack: routesUpdatedCallbacks.toArray(new Runnable[routesUpdatedCallbacks.size()])) {
            callBack.run();
        }
    }

    /**
     * All messages used by the RpcRegistry
     */
    public static class Messages {


        public static class ContainsRoute {
            final List<RpcRouter.RouteIdentifier<?, ?, ?>> routeIdentifiers;

            public ContainsRoute(List<RpcRouter.RouteIdentifier<?, ?, ?>> routeIdentifiers) {
                Preconditions.checkArgument(routeIdentifiers != null &&
                                            !routeIdentifiers.isEmpty(),
                                            "Route Identifiers must be supplied");
                this.routeIdentifiers = routeIdentifiers;
            }

            public List<RpcRouter.RouteIdentifier<?, ?, ?>> getRouteIdentifiers() {
                return this.routeIdentifiers;
            }

            @Override
            public String toString() {
                return "ContainsRoute{" +
                        "routeIdentifiers=" + routeIdentifiers +
                        '}';
            }
        }

        public static class AddOrUpdateRoutes extends ContainsRoute {

            public AddOrUpdateRoutes(List<RpcRouter.RouteIdentifier<?, ?, ?>> routeIdentifiers) {
                super(routeIdentifiers);
            }
        }

        public static class RemoveRoutes extends ContainsRoute {

            public RemoveRoutes(List<RpcRouter.RouteIdentifier<?, ?, ?>> routeIdentifiers) {
                super(routeIdentifiers);
            }
        }

        public static class SetLocalRouter {
            private final ActorRef router;

            public SetLocalRouter(ActorRef router) {
                Preconditions.checkArgument(router != null, "Router must not be null");
                this.router = router;
            }

            public ActorRef getRouter() {
                return this.router;
            }

            @Override
            public String toString() {
                return "SetLocalRouter{" +
                        "router=" + router +
                        '}';
            }
        }

        public static class FindRouters {
            private final RpcRouter.RouteIdentifier<?, ?, ?> routeIdentifier;

            public FindRouters(RpcRouter.RouteIdentifier<?, ?, ?> routeIdentifier) {
                Preconditions.checkArgument(routeIdentifier != null, "Route must not be null");
                this.routeIdentifier = routeIdentifier;
            }

            public RpcRouter.RouteIdentifier<?, ?, ?> getRouteIdentifier() {
                return routeIdentifier;
            }

            @Override
            public String toString() {
                return "FindRouters{" +
                        "routeIdentifier=" + routeIdentifier +
                        '}';
            }
        }

        public static class FindRoutersReply {
            final List<Pair<ActorRef, Long>> routerWithUpdateTime;

            public FindRoutersReply(List<Pair<ActorRef, Long>> routerWithUpdateTime) {
                Preconditions.checkArgument(routerWithUpdateTime != null, "List of routers found must not be null");
                this.routerWithUpdateTime = routerWithUpdateTime;
            }

            public List<Pair<ActorRef, Long>> getRouterWithUpdateTime() {
                return routerWithUpdateTime;
            }

            @Override
            public String toString() {
                return "FindRoutersReply{" +
                        "routerWithUpdateTime=" + routerWithUpdateTime +
                        '}';
            }
        }
    }

    private static class RpcRegistryCreator implements Creator<RpcRegistry> {
        private static final long serialVersionUID = 1L;
        private final RemoteRpcProviderConfig config;

        private RpcRegistryCreator(RemoteRpcProviderConfig config) {
            this.config = config;
        }

        @Override
        public RpcRegistry create() throws Exception {
            RpcRegistry registry =  new RpcRegistry(config);
            RemoteRpcRegistryMXBean mxBean = new RemoteRpcRegistryMXBeanImpl(registry);
            return registry;
        }
    }
}
