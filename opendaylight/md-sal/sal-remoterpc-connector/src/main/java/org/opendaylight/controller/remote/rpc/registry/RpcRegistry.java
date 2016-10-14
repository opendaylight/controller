/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.japi.Creator;
import akka.japi.Option;
import akka.japi.Pair;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.RpcManager;
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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
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

    private Map<String, DOMRpcIdentifier> globalRpcIdentifiers;

    public RpcRegistry(final RemoteRpcProviderConfig config) {
        super(config);
        getLocalBucket().setData(new RoutingTable());
        findRouterTimeout = getConfig().getGossipTickInterval().$times(10);
    }

    public static Props props(final RemoteRpcProviderConfig config) {
        return Props.create(new RpcRegistryCreator(config));
    }

    @Override
    protected void handleReceive(final Object message) throws Exception {
        //TODO: if sender is remote, reject message

        if (message instanceof SetLocalRouter) {
            receiveSetLocalRouter((SetLocalRouter) message);
        } else if (message instanceof AddOrUpdateRoutes) {
            receiveAddRoutes((AddOrUpdateRoutes) message);
        } else if (message instanceof RemoveRoutes) {
            receiveRemoveRoutes((RemoveRoutes) message);
        } else if (message instanceof Messages.FindRouters) {
            receiveGetRouter((FindRouters) message);
        } else if (message instanceof Messages.StoreGlobalRpcsFound) {
            receiveStoreGlobalRpcsFound((Messages.StoreGlobalRpcsFound) message);
        } else if (message instanceof org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.UpdateRemoteBuckets) {
            super.handleReceive(message);
            registerRoutedRpcDelegate();
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
    private void receiveSetLocalRouter(final SetLocalRouter message) {
        getLocalBucket().getData().setRouter(message.getRouter());
    }

    /**
     * @param msg
     */
    private void receiveAddRoutes(final AddOrUpdateRoutes msg) {

        log.debug("AddOrUpdateRoutes: {}", msg.getRouteIdentifiers());

        final RoutingTable table = getLocalBucket().getData().copy();
        for(final RpcRouter.RouteIdentifier<?, ?, ?> routeId : msg.getRouteIdentifiers()) {
            table.addRoute(routeId);
        }

        updateLocalBucket(table);

        onBucketsUpdated();
    }

    /**
     * @param msg contains list of route ids to remove
     */
    private void receiveRemoveRoutes(final RemoveRoutes msg) {

        final RoutingTable table = getLocalBucket().getData().copy();
        for (final RpcRouter.RouteIdentifier<?, ?, ?> routeId : msg.getRouteIdentifiers()) {
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
            final Runnable routesUpdatedRunnable = () -> {
                    if(findRouters(findRouters, sender)) {
                        routesUpdatedCallbacks.remove(this);
                        timer.get().cancel();
                    }
            };

            routesUpdatedCallbacks.add(routesUpdatedRunnable);

            final Runnable timerRunnable = () -> {
                log.warn("Timed out finding routers for {}", findRouters.getRouteIdentifier());

                routesUpdatedCallbacks.remove(routesUpdatedRunnable);
                sender.tell(new Messages.FindRoutersReply(
                        Collections.<Pair<ActorRef, Long>>emptyList()), self());
            };

            timer.set(getContext().system().scheduler().scheduleOnce(findRouterTimeout, self(), timerRunnable,
                    getContext().dispatcher(), self()));
        }
    }

    private boolean findRouters(final FindRouters findRouters, final ActorRef sender) {
        final List<Pair<ActorRef, Long>> routers = new ArrayList<>();

        final RouteIdentifier<?, ?, ?> routeId = findRouters.getRouteIdentifier();
        findRoutes(getLocalBucket().getData(), routeId, routers);

        for(final Bucket<RoutingTable> bucket : getRemoteBuckets().values()) {
            findRoutes(bucket.getData(), routeId, routers);
        }

        log.debug("Found {} routers for {}", routers.size(), findRouters.getRouteIdentifier());

        final boolean foundRouters = !routers.isEmpty();
        if(foundRouters) {
            sender.tell(new Messages.FindRoutersReply(routers), getSelf());
        }

        return foundRouters;
    }

    private static void findRoutes(final RoutingTable table, final RpcRouter.RouteIdentifier<?, ?, ?> routeId,
            final List<Pair<ActorRef, Long>> routers) {
        if (table == null) {
            return;
        }

        final Option<Pair<ActorRef, Long>> routerWithUpdateTime = table.getRouterFor(routeId);
        if(!routerWithUpdateTime.isEmpty()) {
            routers.add(routerWithUpdateTime.get());
        }
    }

    private void receiveStoreGlobalRpcsFound(final Messages.StoreGlobalRpcsFound storeGlobalRpcsFound) {
        if (this.globalRpcIdentifiers == null) {
            this.globalRpcIdentifiers = new HashMap<>();
        }
        this.globalRpcIdentifiers.putAll(storeGlobalRpcsFound.getGlobalRpcIdentifiers());
    }

    /**
     * Registering delegates for remote rpcs present in the remote bucket, both global and routed rpcs.
     *
     * We do not register for local rpcs as md-sal rpcregistry would handle the local rpcs
     */
    private void registerRoutedRpcDelegate() {
        final Set<DOMRpcIdentifier> domRpcIdentifierSet = new HashSet<>();
        for (final Map.Entry<Address, Bucket<RoutingTable>> entry : getRemoteBuckets().entrySet()) {
            final RoutingTable table = entry.getValue().getData();
            for (final RpcRouter.RouteIdentifier<?, ?, ?> route : table.getRoutes()) {
                if (getLocalBucket().getData().contains(route) || isRpcGlobal(route)) {
                    // on a remote node, we don't want to register ourselves as delegate/provider, if the rpc is present in local-bucket or if the rpc is global
                    // it will get serviced by md-sal core rpc registry. This can happen if a non-cluster-aware app registers a global rpc in every node.
                    // this should handle routed-rpc as well, as route-identifier contains type and route
                    LOG.debug("Found remote in local bucket, so ignoring for rpc registration, route:{}", route);
                    continue;
                }

                if (route.getType() != null) {
                    // we register based on only the type of the rpc and not the route.
                    // Any routed rpcs  matching the type but not the route, will get routed to remote-rpc-connector
                    final DOMRpcIdentifier domRpcIdentifier = DOMRpcIdentifier
                            .create(SchemaPath.create(true, (QName) route.getType()));
                    domRpcIdentifierSet.add(domRpcIdentifier);
                }
            }
        }

        if (!domRpcIdentifierSet.isEmpty()) {
            final ActorRef registryParent = getContext().parent();
            final Address selfAddress = getContext().provider().getDefaultAddress();
            // send a message to rpcmanager, since its a light-weight actor, these messages should get processed faster
            LOG.info("{} Registering remote rpcs with broker:{}", selfAddress, domRpcIdentifierSet);
            registryParent.tell(new RpcManager.Messages.RegisterRpcImplementation(domRpcIdentifierSet), self());
        }
    }

    private boolean isRpcGlobal(final RpcRouter.RouteIdentifier<?, ?, ?> route) {
        if (globalRpcIdentifiers != null) {
            // the remote rpc can be a locally registered global rpc, if the type of the rpc matches
            for (final Map.Entry<String, DOMRpcIdentifier> entry : globalRpcIdentifiers.entrySet()) {
                if (route.getType().equals(entry.getValue().getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Map<String, DOMRpcIdentifier> getGlobalRpcsFromModules() {
        return this.globalRpcIdentifiers;
    }

    @Override
    protected void onBucketsUpdated() {
        if(routesUpdatedCallbacks.isEmpty()) {
            return;
        }

        for(final Runnable callBack: routesUpdatedCallbacks.toArray(new Runnable[routesUpdatedCallbacks.size()])) {
            callBack.run();
        }
    }

    /**
     * All messages used by the RpcRegistry
     */
    public static class Messages {


        public static class ContainsRoute {
            final List<RpcRouter.RouteIdentifier<?, ?, ?>> routeIdentifiers;

            public ContainsRoute(final List<RpcRouter.RouteIdentifier<?, ?, ?>> routeIdentifiers) {
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

            public AddOrUpdateRoutes(final List<RpcRouter.RouteIdentifier<?, ?, ?>> routeIdentifiers) {
                super(routeIdentifiers);
            }
        }

        public static class RemoveRoutes extends ContainsRoute {

            public RemoveRoutes(final List<RpcRouter.RouteIdentifier<?, ?, ?>> routeIdentifiers) {
                super(routeIdentifiers);
            }
        }

        public static class SetLocalRouter {
            private final ActorRef router;

            public SetLocalRouter(final ActorRef router) {
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

            public FindRouters(final RpcRouter.RouteIdentifier<?, ?, ?> routeIdentifier) {
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

            public FindRoutersReply(final List<Pair<ActorRef, Long>> routerWithUpdateTime) {
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

        public static class StoreGlobalRpcsFound {
            private final Map<String, DOMRpcIdentifier> globalRpcIdentifiers;

            public StoreGlobalRpcsFound(final Map<String, DOMRpcIdentifier> globalRpcIdentifiers) {
                this.globalRpcIdentifiers =  globalRpcIdentifiers;
            }

            public Map<String, DOMRpcIdentifier> getGlobalRpcIdentifiers() {
                return globalRpcIdentifiers;
            }

            @Override
            public String toString() {
                return "StoreGlobalRpcsFound{" +
                        "globalRpcIdentifiers=" + globalRpcIdentifiers +
                        '}';
            }
        }
    }

    private static class RpcRegistryCreator implements Creator<RpcRegistry> {
        private static final long serialVersionUID = 1L;
        private final RemoteRpcProviderConfig config;

        private RpcRegistryCreator(final RemoteRpcProviderConfig config) {
            this.config = config;
        }

        @Override
        public RpcRegistry create() throws Exception {
            final RpcRegistry registry =  new RpcRegistry(config);
            final RemoteRpcRegistryMXBean mxBean = new RemoteRpcRegistryMXBeanImpl(registry);
            return registry;
        }
    }
}
