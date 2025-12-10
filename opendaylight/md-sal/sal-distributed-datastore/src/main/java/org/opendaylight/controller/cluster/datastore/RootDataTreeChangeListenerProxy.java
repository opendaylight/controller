/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.dispatch.OnComplete;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeNotificationListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RootDataTreeChangeListenerProxy<L extends DOMDataTreeChangeListener> extends AbstractObjectRegistration<L> {
    private abstract static sealed class State {
        // Marker
    }

    private static final class ResolveShards extends State {
        final HashMap<String, Object> localShards = new HashMap<>();
        final int shardCount;

        ResolveShards(final int shardCount) {
            this.shardCount = shardCount;
        }
    }

    private static final class Subscribed extends State {
        final @NonNull ArrayList<ActorSelection> subscriptions;
        final @NonNull ActorRef dtclActor;

        Subscribed(final ActorRef dtclActor, final int shardCount) {
            this.dtclActor = requireNonNull(dtclActor);
            subscriptions = new ArrayList<>(shardCount);
        }
    }

    @NonNullByDefault
    private static final class Terminated extends State {
        static final Terminated INSTANCE = new Terminated();

        private Terminated() {
            // Hidden on purpose
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(RootDataTreeChangeListenerProxy.class);

    private final ActorUtils actorUtils;

    @GuardedBy("this")
    private State state;

    RootDataTreeChangeListenerProxy(final ActorUtils actorUtils, final @NonNull L listener,
            final Set<String> shardNames) {
        super(listener);
        this.actorUtils = requireNonNull(actorUtils);
        state = new ResolveShards(shardNames.size());

        for (var shardName : shardNames) {
            actorUtils.findLocalShardAsync(shardName).onComplete(new OnComplete<ActorRef>() {
                @Override
                public void onComplete(final Throwable failure, final ActorRef success) {
                    onFindLocalShardComplete(shardName, failure, success);
                }
            }, actorUtils.getClientDispatcher());
        }
    }

    @Override
    protected synchronized void removeRegistration() {
        switch (state) {
            case Terminated terminated -> {
                // Trivial case: we have already terminated on a failure, so this is a no-op
            }
            case ResolveShards resolvePaths -> {
                // Simple case: just mark the fact we were closed, terminating when resolution finishes
                state = Terminated.INSTANCE;
            }
            case Subscribed subscribed -> terminate(subscribed);
        }
    }

    private synchronized void onFindLocalShardComplete(final String shardName, final Throwable failure,
            final ActorRef shard) {
        if (state instanceof ResolveShards resolveShards) {
            localShardsResolved(resolveShards, shardName, failure, shard);
        } else {
            LOG.debug("{}: lookup for shard {} turned into a noop on state {}", logContext(), shardName, state);
        }
    }

    @Holding("this")
    private void localShardsResolved(final ResolveShards current, final String shardName, final Throwable failure,
            final ActorRef shard) {
        final Object result = failure != null ? failure : verifyNotNull(shard);
        LOG.debug("{}: lookup for shard {} resulted in {}", logContext(), shardName, result);
        current.localShards.put(shardName, result);

        if (current.localShards.size() == current.shardCount) {
            // We have all the responses we need
            if (current.localShards.values().stream().anyMatch(Throwable.class::isInstance)) {
                reportFailure(current.localShards);
            } else {
                subscribeToShards(current.localShards);
            }
        }
    }

    @Holding("this")
    private void reportFailure(final Map<String, Object> localShards) {
        for (var entry : Maps.filterValues(localShards, Throwable.class::isInstance).entrySet()) {
            final var cause = (Throwable) entry.getValue();
            LOG.error("{}: Failed to find local shard {}, cannot register {} at root", logContext(), entry.getKey(),
                getInstance(), cause);
        }
        state = Terminated.INSTANCE;
    }

    @Holding("this")
    private void subscribeToShards(final Map<String, Object> localShards) {
        // Safety check before we start doing anything
        for (var entry : localShards.entrySet()) {
            final var obj = entry.getValue();
            verify(obj instanceof ActorRef, "Unhandled response %s for shard %s", obj, entry.getKey());
        }

        // Instantiate the DTCL actor and update state
        final var dtclActor = actorUtils.getActorSystem().actorOf(
            RootDataTreeChangeListenerActor.props(getInstance(), localShards.size())
              .withDispatcher(actorUtils.getNotificationDispatcherPath()));
        state = new Subscribed(dtclActor, localShards.size());

        // Subscribe to all shards
        final var regMessage = new RegisterDataTreeChangeListener(YangInstanceIdentifier.of(), dtclActor, true);
        for (var entry : localShards.entrySet()) {
            // Do not retain references to localShards
            final var shardName = entry.getKey();
            final var shard = (ActorRef) entry.getValue();

            actorUtils.executeOperationAsync(shard, regMessage,
                actorUtils.getDatastoreContext().getShardInitializationTimeout()).onComplete(new OnComplete<>() {
                    @Override
                    public void onComplete(final Throwable failure, final Object result) {
                        onShardSubscribed(shardName, failure, result);
                    }
                }, actorUtils.getClientDispatcher());
        }
    }

    private synchronized void onShardSubscribed(final String shardName, final Throwable failure, final Object result) {
        if (state instanceof Subscribed current) {
            if (failure != null) {
                LOG.error("{}: Shard {} failed to subscribe, terminating listener {}", logContext(),
                    shardName,getInstance(), failure);
                terminate(current);
            } else {
                onSuccessfulSubscription(current, shardName, (RegisterDataTreeNotificationListenerReply) result);
            }
        } else {
            terminateSubscription(shardName, failure, result);
        }
    }

    @Holding("this")
    private void onSuccessfulSubscription(final Subscribed current, final String shardName,
            final RegisterDataTreeNotificationListenerReply reply) {
        final var regActor = actorUtils.actorSelection(reply.getListenerRegistrationPath());
        LOG.debug("{}: Shard {} subscribed at {}", logContext(), shardName, regActor);
        current.subscriptions.add(regActor);
    }

    @Holding("this")
    private void terminate(final Subscribed current) {
        // Terminate the listener
        current.dtclActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        // Terminate all subscriptions
        for (var regActor : current.subscriptions) {
            regActor.tell(CloseDataTreeNotificationListenerRegistration.getInstance(), ActorRef.noSender());
        }
        state = Terminated.INSTANCE;
    }

    // This method should not modify internal state
    private void terminateSubscription(final String shardName, final Throwable failure, final Object result) {
        if (failure != null) {
            LOG.debug("{}: Shard {} reported late failure", logContext(), shardName, failure);
            return;
        }

        final var regActor = actorUtils.actorSelection(
            ((RegisterDataTreeNotificationListenerReply) result).getListenerRegistrationPath());
        LOG.debug("{}: Shard {} registered late, terminating subscription at {}", logContext(), shardName, regActor);
        regActor.tell(CloseDataTreeNotificationListenerRegistration.getInstance(), ActorRef.noSender());
    }

    private String logContext() {
        return actorUtils.getDatastoreContext().getLogicalStoreType().toString();
    }
}
