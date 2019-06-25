/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.controller.remote.rpc.RemoteOpsProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor;
import org.opendaylight.controller.remote.rpc.registry.mbeans.RemoteActionRegistryMXBeanImpl;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;

/**
 * Registry to look up cluster nodes that have registered for a given Action.
 *
 * <p>
 * It uses {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreActor} to maintain this
 * cluster wide information.
 */
public class ActionRegistry extends BucketStoreActor<ActionRoutingTable> {
    private final ActorRef rpcRegistrar;
    private final RemoteActionRegistryMXBeanImpl mxBean;

    public ActionRegistry(final RemoteOpsProviderConfig config, final ActorRef rpcInvoker,
                          final ActorRef rpcRegistrar) {
        super(config, config.getRpcRegistryPersistenceId(), new ActionRoutingTable(rpcInvoker, ImmutableSet.of()));
        this.rpcRegistrar = Preconditions.checkNotNull(rpcRegistrar);
        this.mxBean = new RemoteActionRegistryMXBeanImpl(new BucketStoreAccess(self(), getContext().dispatcher(),
                config.getAskDuration()), config.getAskDuration());
    }

    /**
     * Create a new props instance for instantiating an ActionRegistry actor.
     *
     * @param config Provider configuration
     * @param opsRegistrar Local RPC provider interface, used to register routers to remote nodes
     * @param opsInvoker Actor handling RPC invocation requests from remote nodes
     * @return A new {@link Props} instance
     */
    public static Props props(final RemoteOpsProviderConfig config, final ActorRef opsInvoker,
                              final ActorRef opsRegistrar) {
        return Props.create(ActionRegistry.class, config, opsInvoker, opsRegistrar);
    }

    @Override
    public void postStop() {
        super.postStop();
        this.mxBean.unregister();
    }

    @Override
    protected void handleCommand(final Object message) throws Exception {
        if (message instanceof ActionRegistry.Messages.UpdateActions) {
            LOG.debug("handling updatesActionRoutes message");
            updatesActionRoutes((Messages.UpdateActions) message);
        } else {
            super.handleCommand(message);
        }
    }

    private void updatesActionRoutes(final Messages.UpdateActions msg) {
        LOG.debug("addedActions: {}", msg.getAddedActions());
        LOG.debug("removedActions: {}", msg.getRemovedActions());
        updateLocalBucket(getLocalData().updateActions(msg.getAddedActions(), msg.getRemovedActions()));
    }

    @Override
    protected void onBucketRemoved(final Address address, final Bucket<ActionRoutingTable> bucket) {
        rpcRegistrar.tell(new Messages.UpdateRemoteActionEndpoints(ImmutableMap.of(address, Optional.empty())),
            ActorRef.noSender());
    }

    @Override
    protected void onBucketsUpdated(final Map<Address, Bucket<ActionRoutingTable>> buckets) {
        LOG.debug("Updating buckets for action registry");
        final Map<Address, Optional<RemoteActionEndpoint>> endpoints = new HashMap<>(buckets.size());

        for (Map.Entry<Address, Bucket<ActionRoutingTable>> e : buckets.entrySet()) {
            final ActionRoutingTable table = e.getValue().getData();

            final Collection<DOMActionInstance> actions = table.getActions();
            endpoints.put(e.getKey(), actions.isEmpty() ? Optional.empty()
                : Optional.of(new RemoteActionEndpoint(table.getRpcInvoker(), actions)));
        }

        if (!endpoints.isEmpty()) {
            rpcRegistrar.tell(new Messages.UpdateRemoteActionEndpoints(endpoints), ActorRef.noSender());
        }
    }

    public static final class RemoteActionEndpoint {
        private final Set<DOMActionInstance> actions;
        private final ActorRef router;

        @VisibleForTesting
        public RemoteActionEndpoint(final ActorRef router, final Collection<DOMActionInstance> actions) {
            this.router = Preconditions.checkNotNull(router);
            this.actions = ImmutableSet.copyOf(actions);
        }

        public ActorRef getRouter() {
            return router;
        }

        public Set<DOMActionInstance> getActions() {
            return actions;
        }
    }

        /**
         * All messages used by the ActionRegistry.
         */
    public static class Messages {
        abstract static class AbstractActionRouteMessage {
            final Collection<DOMActionInstance> addedActions;
            final Collection<DOMActionInstance> removedActions;

            AbstractActionRouteMessage(final Collection<DOMActionInstance> addedActions,
                                       final Collection<DOMActionInstance> removedActions) {
                this.addedActions = ImmutableList.copyOf(addedActions);
                this.removedActions = ImmutableList.copyOf(removedActions);
            }

            Collection<DOMActionInstance> getAddedActions() {
                return this.addedActions;
            }

            Collection<DOMActionInstance> getRemovedActions() {
                return this.removedActions;
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
            public UpdateRemoteActionEndpoints(final Map<Address, Optional<RemoteActionEndpoint>>
                                                                   actionEndpoints) {
                this.actionEndpoints = ImmutableMap.copyOf(actionEndpoints);
            }

            public Map<Address, Optional<RemoteActionEndpoint>> getActionEndpoints() {
                return actionEndpoints;
            }
        }
    }
}


