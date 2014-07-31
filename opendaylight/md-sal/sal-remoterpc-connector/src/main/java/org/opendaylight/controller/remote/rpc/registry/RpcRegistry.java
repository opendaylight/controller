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
import akka.actor.UntypedActor;
import akka.dispatch.Mapper;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Option;
import akka.japi.Pair;
import akka.pattern.Patterns;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStore;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import scala.concurrent.Future;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBuckets;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetAllBucketsReply;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetLocalBucket;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetLocalBucketReply;
import static org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.AddOrUpdateRoute;
import static org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.RemoveRoute;
import static org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.SetLocalRouter;
import static org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.UpdateBucket;

/**
 *
 */
public class RpcRegistry extends UntypedActor {

    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private ActorRef bucketStore;

    private ActorRef localRouter;

    public RpcRegistry() {
        bucketStore = getContext().actorOf(Props.create(BucketStore.class), "store");
    }

    public RpcRegistry(ActorRef bucketStore) {
        this.bucketStore = bucketStore;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        log.info("Received message: [{}]", message);
        //TODO: if sender is remote, reject message

        if (message instanceof SetLocalRouter)
            receiveSetLocalRouter((SetLocalRouter) message);

        if (message instanceof AddOrUpdateRoute)
            receiveAddRoute((AddOrUpdateRoute) message);

        else if (message instanceof RemoveRoute)
            receiveRemoveRoute((RemoveRoute) message);

        else if (message instanceof Messages.FindRouters)
            receiveGetRouter((Messages.FindRouters) message);

        else
            unhandled(message);
    }

    private void receiveSetLocalRouter(SetLocalRouter message) {
        if (message == null || message.getRouter() == null)
            return;//ignore

        localRouter = message.getRouter();
    }

    private void receiveAddRoute(AddOrUpdateRoute msg) {
        if (msg == null || msg.getRouteIdentifier() == null)
            return;//ignore

        Preconditions.checkState(localRouter != null, "Router must be set first");

        Future<Object> futureReply = Patterns.ask(bucketStore, new GetLocalBucket(), 1000);
        futureReply.map(getMapperToAddRoute(msg.getRouteIdentifier()), getContext().dispatcher());
    }

    private void receiveRemoveRoute(RemoveRoute msg) {
        if (msg == null || msg.getRouteIdentifier() == null)
            return;//ignore

        Future<Object> futureReply = Patterns.ask(bucketStore, new GetLocalBucket(), 1000);
        futureReply.map(getMapperToRemoveRoute(msg.getRouteIdentifier()), getContext().dispatcher());

    }

    private void receiveGetRouter(Messages.FindRouters msg) {
        final ActorRef sender = getSender();

        //if empty message, return empty list
        if (msg == null || msg.getRouteIdentifier() == null) {
            sender.tell(createEmptyReply(), getSelf());
            return;
        }

        log.info("Ask store to get all buckets");
        Future<Object> futureReply = Patterns.ask(bucketStore, new GetAllBuckets(), 1000);
        futureReply.map(getMapperToGetRouter(msg.getRouteIdentifier(), sender), getContext().dispatcher());

    }

    private Messages.FindRoutersReply createEmptyReply() {
        List<Pair<ActorRef, Long>> routerWithUpdateTime = Collections.emptyList();
        return new Messages.FindRoutersReply(routerWithUpdateTime);
    }

    private Messages.FindRoutersReply createReplyWithRouters(Map<Address, Bucket> buckets, RpcRouter.RouteIdentifier<?, ?, ?> routeId) {

        List<Pair<ActorRef, Long>> routers = new ArrayList<>();

        Option<Pair<ActorRef, Long>> routerWithUpdateTime = null;

        for (Bucket bucket : buckets.values()) {
            log.info("Creating reply with routers and updated time");
            RoutingTable table = (RoutingTable) bucket.getData();

            log.info("Routing table is-[{}]", table);
            if (table == null)
                continue;

            routerWithUpdateTime = table.getRouterFor(routeId);

            log.info("Found router:[{}]", routerWithUpdateTime);

            if (routerWithUpdateTime.isEmpty())
                continue;

            routers.add(routerWithUpdateTime.get());
        }

        return new Messages.FindRoutersReply(routers);
    }


    private Mapper<Object, Void> getMapperToGetRouter(final RpcRouter.RouteIdentifier<?, ?, ?> routeId, final ActorRef sender) {
        return new Mapper<Object, Void>() {
            @Override
            public Void apply(Object replyMessage) {
                log.info("Received message: [{}]", replyMessage);
                log.info("sender is [{}]", sender);
                if (replyMessage instanceof GetAllBucketsReply) {
                    log.info("Going to cast");
                    GetAllBucketsReply reply = (GetAllBucketsReply) replyMessage;
                    log.info("Casted message is [{}]", reply);
                    Map<Address, Bucket> buckets = reply.getBuckets();
                    log.info("Received buckets [{}]", buckets);
                    if (buckets == null || buckets.isEmpty()) {
                        log.info("Local bucket is null");
                        sender.tell(createEmptyReply(), getSelf());
                        return null;
                    }
                    log.info("Sending routers for route[{}]:[{}]", routeId, buckets);
                    sender.tell(createReplyWithRouters(buckets, routeId), getSelf());

                } else{
                    log.info("Received message not instance of GetAllBucketsReply");
                }
                return null;
            }
        };
    }


    private Mapper<Object, Void> getMapperToRemoveRoute(final RpcRouter.RouteIdentifier<?, ?, ?> routeId) {
        return new Mapper<Object, Void>() {
            @Override
            public Void apply(Object replyMessage) {
                if (replyMessage instanceof GetLocalBucketReply) {

                    GetLocalBucketReply reply = (GetLocalBucketReply) replyMessage;
                    Bucket<RoutingTable> bucket = reply.getBucket();

                    if (bucket == null) {
                        log.debug("Local bucket is null");
                        return null;
                    }

                    RoutingTable table = bucket.getData();
                    if (table == null)
                        table = new RoutingTable();

                    table.setRouter(localRouter);
                    table.removeRoute(routeId);

                    bucket.setData(table);

                    UpdateBucket updateBucketMessage = new UpdateBucket(bucket);
                    bucketStore.tell(updateBucketMessage, getSelf());
                }
                return null;
            }
        };
    }

    private Mapper<Object, Void> getMapperToAddRoute(final RpcRouter.RouteIdentifier<?, ?, ?> routeId) {

        return new Mapper<Object, Void>() {
            @Override
            public Void apply(Object replyMessage) {
                if (replyMessage instanceof GetLocalBucketReply) {

                    GetLocalBucketReply reply = (GetLocalBucketReply) replyMessage;
                    Bucket<RoutingTable> bucket = reply.getBucket();

                    if (bucket == null) {
                        log.debug("Local bucket is null");
                        return null;
                    }

                    RoutingTable table = bucket.getData();
                    if (table == null)
                        table = new RoutingTable();

                    table.setRouter(localRouter);
                    table.addRoute(routeId);

                    bucket.setData(table);

                    UpdateBucket updateBucketMessage = new UpdateBucket(bucket);
                    bucketStore.tell(updateBucketMessage, getSelf());
                }

                return null;
            }
        };
    }


    public static class Messages {


        public static class ContainsRoute {
            final RpcRouter.RouteIdentifier<?,?,?> routeIdentifier;

            public ContainsRoute(RpcRouter.RouteIdentifier<?, ?, ?> routeIdentifier) {
                Preconditions.checkArgument(routeIdentifier != null);
                this.routeIdentifier = routeIdentifier;
            }

            public RpcRouter.RouteIdentifier<?,?,?> getRouteIdentifier(){
                return this.routeIdentifier;
            }
        }

        public static class AddOrUpdateRoute extends ContainsRoute{

            public AddOrUpdateRoute(RpcRouter.RouteIdentifier<?, ?, ?> routeIdentifier) {
                super(routeIdentifier);
            }
        }

        public static class RemoveRoute extends ContainsRoute {

            public RemoveRoute(RpcRouter.RouteIdentifier<?, ?, ?> routeIdentifier) {
                super(routeIdentifier);
            }
        }

        public static class SetLocalRouter{
            private final ActorRef router;

            public SetLocalRouter(ActorRef router) {
                this.router = router;
            }

            public ActorRef getRouter(){
                return this.router;
            }
        }

        public static class FindRouters extends ContainsRoute {
            public FindRouters(RpcRouter.RouteIdentifier<?, ?, ?> routeIdentifier) {
                super(routeIdentifier);
            }
        }

        public static class FindRoutersReply {
            final List<Pair<ActorRef, Long>> routerWithUpdateTime;

            public FindRoutersReply(List<Pair<ActorRef, Long>> routerWithUpdateTime) {
                this.routerWithUpdateTime = routerWithUpdateTime;
            }

            public List<Pair<ActorRef, Long>> getRouterWithUpdateTime(){
                return routerWithUpdateTime;
            }
        }
    }
}
