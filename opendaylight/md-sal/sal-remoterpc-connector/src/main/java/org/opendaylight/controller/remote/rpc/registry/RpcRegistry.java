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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.AddOrUpdateRoutes;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.RemoveRoutes;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.UpdateRemoteEndpoints;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor;
import org.opendaylight.controller.remote.rpc.registry.mbeans.RemoteRpcRegistryMXBeanImpl;

/**
 * Registry to look up cluster nodes that have registered for a given RPC.
 *
 * <p>
 * It uses {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor} to maintain this
 * cluster wide information.
 */
public class RpcRegistry extends BucketStoreActor<RoutingTable> {
    private final ActorRef rpcRegistrar;
    private final RemoteRpcRegistryMXBeanImpl mxBean;

    public RpcRegistry(final RemoteRpcProviderConfig config, final ActorRef rpcInvoker, final ActorRef rpcRegistrar) {
        super(config, config.getRpcRegistryPersistenceId(), new RoutingTable(rpcInvoker, ImmutableSet.of()));
        this.rpcRegistrar = Preconditions.checkNotNull(rpcRegistrar);
        this.mxBean = new RemoteRpcRegistryMXBeanImpl(new BucketStoreAccess(self(), getContext().dispatcher(),
                config.getAskDuration()), config.getAskDuration());
    }

    /**
     * Create a new props instance for instantiating an RpcRegistry actor.
     *
     * @param config Provider configuration
     * @param rpcRegistrar Local RPC provider interface, used to register routers to remote nodes
     * @param rpcInvoker Actor handling RPC invocation requests from remote nodes
     * @return A new {@link Props} instance
     */
    public static Props props(final RemoteRpcProviderConfig config, final ActorRef rpcInvoker,
            final ActorRef rpcRegistrar) {
        return Props.create(RpcRegistry.class, config, rpcInvoker, rpcRegistrar);
    }

    @Override
    public void postStop() {
        super.postStop();
        this.mxBean.unregister();
    }

    @Override
    protected void handleCommand(final Object message) throws Exception {
        if (message instanceof AddOrUpdateRoutes) {
            receiveAddRoutes((AddOrUpdateRoutes) message);
        } else if (message instanceof RemoveRoutes) {
            receiveRemoveRoutes((RemoveRoutes) message);
        } else {
            super.handleCommand(message);
        }
    }

    private void receiveAddRoutes(final AddOrUpdateRoutes msg) {
        LOG.debug("AddOrUpdateRoutes: {}", msg.getRouteIdentifiers());
        updateLocalBucket(getLocalData().addRpcs(msg.getRouteIdentifiers()));
    }

    /**
     * Processes a RemoveRoutes message.
     *
     * @param msg contains list of route ids to remove
     */
    private void receiveRemoveRoutes(final RemoveRoutes msg) {
        LOG.debug("RemoveRoutes: {}", msg.getRouteIdentifiers());
        updateLocalBucket(getLocalData().removeRpcs(msg.getRouteIdentifiers()));
    }

    @Override
    protected void onBucketRemoved(final Address address, final Bucket<RoutingTable> bucket) {
        rpcRegistrar.tell(new UpdateRemoteEndpoints(ImmutableMap.of(address, Optional.empty())), ActorRef.noSender());
    }

    @Override
    protected void onBucketsUpdated(final Map<Address, Bucket<RoutingTable>> buckets) {
        final Map<Address, Optional<RemoteRpcEndpoint>> endpoints = new HashMap<>(buckets.size());

        for (Entry<Address, Bucket<RoutingTable>> e : buckets.entrySet()) {
            final RoutingTable table = e.getValue().getData();

            final Collection<DOMRpcIdentifier> rpcs = table.getRoutes();
            endpoints.put(e.getKey(), rpcs.isEmpty() ? Optional.empty()
                    : Optional.of(new RemoteRpcEndpoint(table.getRpcInvoker(), rpcs)));
        }

        if (!endpoints.isEmpty()) {
            rpcRegistrar.tell(new UpdateRemoteEndpoints(endpoints), ActorRef.noSender());
        }
    }

    public static final class RemoteRpcEndpoint {
        private final Set<DOMRpcIdentifier> rpcs;
        private final ActorRef router;

        @VisibleForTesting
        public RemoteRpcEndpoint(final ActorRef router, final Collection<DOMRpcIdentifier> rpcs) {
            this.router = Preconditions.checkNotNull(router);
            this.rpcs = ImmutableSet.copyOf(rpcs);
        }

        public ActorRef getRouter() {
            return router;
        }

        public Set<DOMRpcIdentifier> getRpcs() {
            return rpcs;
        }
    }

    /**
     * All messages used by the RpcRegistry.
     */
    public static class Messages {
        abstract static class AbstractRouteMessage {
            final List<DOMRpcIdentifier> routeIdentifiers;

            AbstractRouteMessage(final Collection<DOMRpcIdentifier> routeIdentifiers) {
                Preconditions.checkArgument(routeIdentifiers != null && !routeIdentifiers.isEmpty(),
                        "Route Identifiers must be supplied");
                this.routeIdentifiers = ImmutableList.copyOf(routeIdentifiers);
            }

            List<DOMRpcIdentifier> getRouteIdentifiers() {
                return this.routeIdentifiers;
            }

            @Override
            public String toString() {
                return "ContainsRoute{" + "routeIdentifiers=" + routeIdentifiers + '}';
            }
        }

        public static final class AddOrUpdateRoutes extends AbstractRouteMessage {
            public AddOrUpdateRoutes(final Collection<DOMRpcIdentifier> routeIdentifiers) {
                super(routeIdentifiers);
            }
        }

        public static final class RemoveRoutes extends AbstractRouteMessage {
            public RemoveRoutes(final Collection<DOMRpcIdentifier> routeIdentifiers) {
                super(routeIdentifiers);
            }
        }

        public static final class UpdateRemoteEndpoints {
            private final Map<Address, Optional<RemoteRpcEndpoint>> endpoints;

            @VisibleForTesting
            public UpdateRemoteEndpoints(final Map<Address, Optional<RemoteRpcEndpoint>> endpoints) {
                this.endpoints = ImmutableMap.copyOf(endpoints);
            }

            public Map<Address, Optional<RemoteRpcEndpoint>> getEndpoints() {
                return endpoints;
            }
        }
    }
}
