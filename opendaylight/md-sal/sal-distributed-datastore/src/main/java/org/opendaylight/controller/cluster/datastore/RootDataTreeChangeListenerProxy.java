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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.dispatch.OnComplete;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
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
    private abstract static class State {

    }

    private static final class ResolveShards extends State {
        final Map<String, Object> localShards = new HashMap<>();
        final int shardCount;

        ResolveShards(final int shardCount) {
            this.shardCount = shardCount;
        }
    }

    private static final class Subscribed extends State {
        final List<ActorSelection> subscriptions;
        final ActorRef dtclActor;

        Subscribed(final ActorRef dtclActor, final int shardCount) {
            this.dtclActor = requireNonNull(dtclActor);
            subscriptions = new ArrayList<>(shardCount);
        }
    }

    private static final class Terminated extends State {

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

        for (String shardName : shardNames) {
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
        if (state instanceof Terminated) {
            // Trivial case: we have already terminated on a failure, so this is a no-op
        } else if (state instanceof ResolveShards) {
            // Simple case: just mark the fact we were closed, terminating when resolution finishes
            state = new Terminated();
        } else if (state instanceof Subscribed subscribed) {
            terminate(subscribed);
        } else {
            throw new IllegalStateException("Unhandled close in state " + state);
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
        for (Entry<String, Object> entry : Maps.filterValues(localShards, Throwable.class::isInstance).entrySet()) {
            final Throwable cause = (Throwable) entry.getValue();
            LOG.error("{}: Failed to find local shard {}, cannot register {} at root", logContext(), entry.getKey(),
                getInstance(), cause);
        }
        state = new Terminated();
    }

    @Holding("this")
    private void subscribeToShards(final Map<String, Object> localShards) {
        // Safety check before we start doing anything
        for (Entry<String, Object> entry : localShards.entrySet()) {
            final Object obj = entry.getValue();
            verify(obj instanceof ActorRef, "Unhandled response %s for shard %s", obj, entry.getKey());
        }

        // Instantiate the DTCL actor and update state
        final ActorRef dtclActor = actorUtils.getActorSystem().actorOf(
            RootDataTreeChangeListenerActor.props(getInstance(), localShards.size())
              .withDispatcher(actorUtils.getNotificationDispatcherPath()));
        state = new Subscribed(dtclActor, localShards.size());

        // Subscribe to all shards
        final RegisterDataTreeChangeListener regMessage = new RegisterDataTreeChangeListener(
            YangInstanceIdentifier.of(), dtclActor, true);
        for (Entry<String, Object> entry : localShards.entrySet()) {
            // Do not retain references to localShards
            final String shardName = entry.getKey();
            final ActorRef shard = (ActorRef) entry.getValue();

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
        final ActorSelection regActor = actorUtils.actorSelection(reply.getListenerRegistrationPath());
        LOG.debug("{}: Shard {} subscribed at {}", logContext(), shardName, regActor);
        current.subscriptions.add(regActor);
    }

    @Holding("this")
    private void terminate(final Subscribed current) {
        // Terminate the listener
        current.dtclActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        // Terminate all subscriptions
        for (ActorSelection regActor : current.subscriptions) {
            regActor.tell(CloseDataTreeNotificationListenerRegistration.INSTANCE, ActorRef.noSender());
        }
        state = new Terminated();
    }

    // This method should not modify internal state
    private void terminateSubscription(final String shardName, final Throwable failure, final Object result) {
        if (failure == null) {
            final ActorSelection regActor = actorUtils.actorSelection(
                ((RegisterDataTreeNotificationListenerReply) result).getListenerRegistrationPath());
            LOG.debug("{}: Shard {} registered late, terminating subscription at {}", logContext(), shardName,
                regActor);
            regActor.tell(CloseDataTreeNotificationListenerRegistration.INSTANCE, ActorRef.noSender());
        } else {
            LOG.debug("{}: Shard {} reported late failure", logContext(), shardName, failure);
        }
    }

    private String logContext() {
        return actorUtils.getDatastoreContext().getLogicalStoreType().toString();
    }
}
