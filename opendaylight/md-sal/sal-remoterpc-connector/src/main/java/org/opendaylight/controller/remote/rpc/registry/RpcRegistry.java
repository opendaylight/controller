/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.Props;
import org.opendaylight.controller.remote.rpc.RemoteOpsProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor;
import org.opendaylight.controller.remote.rpc.registry.mbeans.RemoteRpcRegistryMXBeanImpl;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry to look up cluster nodes that have registered for a given RPC.
 *
 * <p>It uses {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor} to maintain this
 * cluster-wide information.
 */
public class RpcRegistry extends BucketStoreActor<RoutingTable> {
    private static final Logger LOG = LoggerFactory.getLogger(RpcRegistry.class);

    private final ActorRef rpcRegistrar;
    private RemoteRpcRegistryMXBeanImpl mxBean;

    public RpcRegistry(final RemoteOpsProviderConfig config, final Path directory, final ActorRef rpcInvoker,
            final ActorRef rpcRegistrar) {
        super(config, directory, config.getRpcRegistryPersistenceId(), new RoutingTable(rpcInvoker, ImmutableSet.of()));
        this.rpcRegistrar = requireNonNull(rpcRegistrar);
    }

    /**
     * Create a new props instance for instantiating an RpcRegistry actor.
     *
     * @param config Provider configuration
     * @param directory Persistence directory
     * @param rpcRegistrar Local RPC provider interface, used to register routers to remote nodes
     * @param rpcInvoker Actor handling RPC invocation requests from remote nodes
     * @return A new {@link Props} instance
     */
    public static Props props(final RemoteOpsProviderConfig config, final Path directory, final ActorRef rpcInvoker,
                              final ActorRef rpcRegistrar) {
        return Props.create(RpcRegistry.class, config, directory, rpcInvoker, rpcRegistrar);
    }

    @Override
    protected Logger log() {
        return LOG;
    }

    @Override
    public void preStart() throws IOException {
        super.preStart();
        mxBean = new RemoteRpcRegistryMXBeanImpl(new BucketStoreAccess(self(), getContext().dispatcher(),
            getConfig().getAskDuration()), getConfig().getAskDuration());
    }

    @Override
    public void postStop() throws Exception {
        if (mxBean != null) {
            mxBean.unregister();
            mxBean = null;
        }
        super.postStop();
    }

    @Override
    protected void handleReceive(final Object message) {
        switch (message) {
            case AddOrUpdateRoutes addRoutes -> receiveAddRoutes(addRoutes);
            case RemoveRoutes removeRoutes -> receiveRemoveRoutes(removeRoutes);
            default -> super.handleReceive(message);
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
        final var endpoints = HashMap.<Address, Optional<RemoteRpcEndpoint>>newHashMap(buckets.size());

        for (var eentry : buckets.entrySet()) {
            final var table = eentry.getValue().getData();
            final var rpcs = table.getItems();
            endpoints.put(eentry.getKey(), rpcs.isEmpty() ? Optional.empty()
                    : Optional.of(new RemoteRpcEndpoint(table.getInvoker(), rpcs)));
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
            this.router = requireNonNull(router);
            this.rpcs = ImmutableSet.copyOf(rpcs);
        }

        public ActorRef getRouter() {
            return router;
        }

        public Set<DOMRpcIdentifier> getRpcs() {
            return rpcs;
        }
    }

    abstract static class AbstractRouteMessage {
        final List<DOMRpcIdentifier> rpcRouteIdentifiers;

        AbstractRouteMessage(final Collection<DOMRpcIdentifier> rpcRouteIdentifiers) {
            checkArgument(rpcRouteIdentifiers != null && !rpcRouteIdentifiers.isEmpty(),
                "Route Identifiers must be supplied");
            this.rpcRouteIdentifiers = ImmutableList.copyOf(rpcRouteIdentifiers);
        }

        List<DOMRpcIdentifier> getRouteIdentifiers() {
            return rpcRouteIdentifiers;
        }

        @Override
        public String toString() {
            return "ContainsRoute{" + "routeIdentifiers=" + rpcRouteIdentifiers + '}';
        }
    }

    public static final class AddOrUpdateRoutes extends AbstractRouteMessage {
        public AddOrUpdateRoutes(final Collection<DOMRpcIdentifier> rpcRouteIdentifiers) {
            super(rpcRouteIdentifiers);
        }

    }

    public static final class RemoveRoutes extends AbstractRouteMessage {
        public RemoveRoutes(final Collection<DOMRpcIdentifier> rpcRouteIdentifiers) {
            super(rpcRouteIdentifiers);
        }
    }

    public static final class UpdateRemoteEndpoints {
        private final ImmutableMap<Address, Optional<RemoteRpcEndpoint>> rpcEndpoints;

        @VisibleForTesting
        public UpdateRemoteEndpoints(final Map<Address, Optional<RemoteRpcEndpoint>> rpcEndpoints) {
            this.rpcEndpoints = ImmutableMap.copyOf(rpcEndpoints);
        }

        public Map<Address, Optional<RemoteRpcEndpoint>> getRpcEndpoints() {
            return rpcEndpoints;
        }
    }
}
