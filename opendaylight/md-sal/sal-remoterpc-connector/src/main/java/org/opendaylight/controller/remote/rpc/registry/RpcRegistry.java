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
import akka.actor.Props;
import akka.dispatch.Mapper;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Option;
import akka.japi.Pair;
import akka.pattern.Patterns;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.AddOrUpdateRoutes;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.FindRouters;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.RemoveRoutes;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.SetLocalRouter;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStore;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.AddToLocalBucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBuckets;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBucketsReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.RemoveFromLocalBucket;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import scala.concurrent.Future;

/**
 * Registry to look up cluster nodes that have registered for a given rpc.
 * <p/>
 * It uses {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStore} to maintain this
 * cluster wide information.
 */
public class RpcRegistry extends AbstractUntypedActorWithMetering {

    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    /**
     * Store to keep the registry. Bucket store sync's it across nodes in the cluster
     */
    private final ActorRef bucketStore;

    /**
     * Rpc broker that would use the registry to route requests.
     */
    private ActorRef localRouter;

    private RemoteRpcProviderConfig config;

    public RpcRegistry() {
        bucketStore = getContext().actorOf(Props.create(BucketStore.class), "store");
        this.config = new RemoteRpcProviderConfig(getContext().system().settings().config());
        log.info("Bucket store path = {}", bucketStore.path().toString());
    }

    public RpcRegistry(ActorRef bucketStore) {
        this.bucketStore = bucketStore;
    }


    @Override
    protected void handleReceive(Object message) throws Exception {
        //TODO: if sender is remote, reject message

        if (message instanceof SetLocalRouter) {
            receiveSetLocalRouter((SetLocalRouter) message);
        }

        if (message instanceof AddOrUpdateRoutes) {
            receiveAddRoutes((AddOrUpdateRoutes) message);
        } else if (message instanceof RemoveRoutes) {
            receiveRemoveRoutes((RemoveRoutes) message);
        } else if (message instanceof Messages.FindRouters) {
            receiveGetRouter((FindRouters) message);
        } else {
            unhandled(message);
        }
    }

    /**
     * Register's rpc broker
     *
     * @param message contains {@link akka.actor.ActorRef} for rpc broker
     */
    private void receiveSetLocalRouter(SetLocalRouter message) {
        localRouter = message.getRouter();
    }

    /**
     * @param msg
     */
    private void receiveAddRoutes(AddOrUpdateRoutes msg) {

        log.debug("AddOrUpdateRoutes: {}", msg.getRouteIdentifiers());

        Preconditions.checkState(localRouter != null, "Router must be set first");

        bucketStore.tell(new AddToLocalBucket(localRouter, msg.getRouteIdentifiers()),
                ActorRef.noSender());
    }

    /**
     * @param msg contains list of route ids to remove
     */
    private void receiveRemoveRoutes(RemoveRoutes msg) {

        bucketStore.tell(new RemoveFromLocalBucket(msg.getRouteIdentifiers()), ActorRef.noSender());

    }

    /**
     * Finds routers for the given rpc.
     *
     * @param msg
     */
    private void receiveGetRouter(FindRouters msg) {
        final ActorRef sender = getSender();

        Future<Object> futureReply = Patterns.ask(bucketStore, new GetAllBuckets(), config.getAskDuration());
        futureReply.map(getMapperToGetRouter(msg.getRouteIdentifier(), sender), getContext().dispatcher());
    }

    /**
     * Helper to create empty reply when no routers are found
     *
     * @return
     */
    private Messages.FindRoutersReply createEmptyReply() {
        List<Pair<ActorRef, Long>> routerWithUpdateTime = Collections.emptyList();
        return new Messages.FindRoutersReply(routerWithUpdateTime);
    }

    /**
     * Helper to create a reply when routers are found for the given rpc
     *
     * @param buckets
     * @param routeId
     * @return
     */
    private Messages.FindRoutersReply createReplyWithRouters(
            Map<Address, Bucket> buckets, RpcRouter.RouteIdentifier<?, ?, ?> routeId) {

        List<Pair<ActorRef, Long>> routers = new ArrayList<>();
        Option<Pair<ActorRef, Long>> routerWithUpdateTime = null;

        for (Bucket bucket : buckets.values()) {

            RoutingTable table = (RoutingTable) bucket.getData();
            if (table == null) {
                continue;
            }

            routerWithUpdateTime = table.getRouterFor(routeId);
            if (routerWithUpdateTime.isEmpty()) {
                continue;
            }

            routers.add(routerWithUpdateTime.get());
        }

        return new Messages.FindRoutersReply(routers);
    }


    ///
    ///private factories to create Mapper
    ///

    /**
     * Receives all buckets returned from bucket store and finds routers for the buckets where given rpc(routeId) is found
     *
     * @param routeId the rpc
     * @param sender  client who asked to find the routers.
     * @return
     */
    private Mapper<Object, Void> getMapperToGetRouter(
            final RpcRouter.RouteIdentifier<?, ?, ?> routeId, final ActorRef sender) {
        return new Mapper<Object, Void>() {
            @Override
            public Void apply(Object replyMessage) {

                if (replyMessage instanceof GetAllBucketsReply) {

                    GetAllBucketsReply reply = (GetAllBucketsReply) replyMessage;
                    Map<Address, Bucket> buckets = reply.getBuckets();

                    if (buckets == null || buckets.isEmpty()) {
                        sender.tell(createEmptyReply(), getSelf());
                        return null;
                    }

                    sender.tell(createReplyWithRouters(buckets, routeId), getSelf());
                }
                return null;
            }
        };
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
}
