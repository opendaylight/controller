/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
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
import org.opendaylight.controller.remote.rpc.registry.mbeans.RemoteActionRegistryMXBeanImpl;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry to look up cluster nodes that have registered for a given Action.
 *
 * <p>It uses {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor} to maintain this
 * cluster-wide information.
 */
public class ActionRegistry extends BucketStoreActor<ActionRoutingTable> {
    private static final Logger LOG = LoggerFactory.getLogger(ActionRegistry.class);

    private final ActorRef rpcRegistrar;

    private RemoteActionRegistryMXBeanImpl mxBean;

    public ActionRegistry(final RemoteOpsProviderConfig config, final Path directory, final ActorRef rpcInvoker,
                          final ActorRef rpcRegistrar) {
        super(config, directory, config.getActionRegistryPersistenceId(),
            new ActionRoutingTable(rpcInvoker, ImmutableSet.of()));
        this.rpcRegistrar = requireNonNull(rpcRegistrar);
    }

    /**
     * Create a new props instance for instantiating an ActionRegistry actor.
     *
     * @param config Provider configuration
     * @param directory Persistence directory
     * @param opsRegistrar Local RPC provider interface, used to register routers to remote nodes
     * @param opsInvoker Actor handling RPC invocation requests from remote nodes
     * @return A new {@link Props} instance
     */
    public static Props props(final RemoteOpsProviderConfig config, final Path directory, final ActorRef opsInvoker,
                              final ActorRef opsRegistrar) {
        return Props.create(ActionRegistry.class, config, directory, opsInvoker, opsRegistrar);
    }

    @Override
    protected Logger log() {
        return LOG;
    }

    @Override
    public void preStart() throws IOException {
        super.preStart();
        mxBean = new RemoteActionRegistryMXBeanImpl(new BucketStoreAccess(self(), getContext().dispatcher(),
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
        if (message instanceof UpdateActions msg) {
            LOG.debug("handling updatesActionRoutes message");
            LOG.debug("addedActions: {}", msg.getAddedActions());
            LOG.debug("removedActions: {}", msg.getRemovedActions());
            updateLocalBucket(getLocalData().updateActions(msg.getAddedActions(), msg.getRemovedActions()));
        } else {
            super.handleReceive(message);
        }
    }

    @Override
    protected void onBucketRemoved(final Address address, final Bucket<ActionRoutingTable> bucket) {
        rpcRegistrar.tell(new UpdateRemoteActionEndpoints(ImmutableMap.of(address, Optional.empty())),
            ActorRef.noSender());
    }

    @Override
    protected void onBucketsUpdated(final Map<Address, Bucket<ActionRoutingTable>> buckets) {
        LOG.debug("Updating buckets for action registry");
        final var endpoints = HashMap.<Address, Optional<RemoteActionEndpoint>>newHashMap(buckets.size());

        for (var entry : buckets.entrySet()) {
            final var table = entry.getValue().getData();
            final var actions = table.getItems();
            endpoints.put(entry.getKey(), actions.isEmpty() ? Optional.empty()
                : Optional.of(new RemoteActionEndpoint(table.getInvoker(), actions)));
        }

        if (!endpoints.isEmpty()) {
            rpcRegistrar.tell(new UpdateRemoteActionEndpoints(endpoints), ActorRef.noSender());
        }
    }

    public static final class RemoteActionEndpoint {
        private final Set<DOMActionInstance> actions;
        private final ActorRef router;

        @VisibleForTesting
        public RemoteActionEndpoint(final ActorRef router, final Collection<DOMActionInstance> actions) {
            this.router = requireNonNull(router);
            this.actions = ImmutableSet.copyOf(actions);
        }

        public ActorRef getRouter() {
            return router;
        }

        public Set<DOMActionInstance> getActions() {
            return actions;
        }
    }

    abstract static class AbstractActionRouteMessage {
        final ImmutableList<DOMActionInstance> addedActions;
        final ImmutableList<DOMActionInstance> removedActions;

        AbstractActionRouteMessage(final Collection<DOMActionInstance> addedActions,
                final Collection<DOMActionInstance> removedActions) {
            this.addedActions = ImmutableList.copyOf(addedActions);
            this.removedActions = ImmutableList.copyOf(removedActions);
        }

        ImmutableList<DOMActionInstance> getAddedActions() {
            return addedActions;
        }

        ImmutableList<DOMActionInstance> getRemovedActions() {
            return removedActions;
        }

        @Override
        public String toString() {
            return "ContainsRoute{" + "addedActions=" + addedActions + " removedActions=" + removedActions + '}';
        }
    }

    public static final class UpdateActions extends AbstractActionRouteMessage {
        public UpdateActions(final Collection<DOMActionInstance> addedActions,
                final Collection<DOMActionInstance> removedActions) {
            super(addedActions, removedActions);
        }
    }

    public static final class UpdateRemoteActionEndpoints {
        private final Map<Address, Optional<RemoteActionEndpoint>> actionEndpoints;

        @VisibleForTesting
        public UpdateRemoteActionEndpoints(final Map<Address, Optional<RemoteActionEndpoint>> actionEndpoints) {
            this.actionEndpoints = ImmutableMap.copyOf(actionEndpoints);
        }

        public Map<Address, Optional<RemoteActionEndpoint>> getActionEndpoints() {
            return actionEndpoints;
        }
    }
}
