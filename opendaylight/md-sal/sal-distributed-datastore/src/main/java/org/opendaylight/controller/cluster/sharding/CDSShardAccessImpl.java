/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static akka.actor.ActorRef.noSender;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.MakeLeaderLocal;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.dom.api.CDSShardAccess;
import org.opendaylight.controller.cluster.dom.api.LeaderLocation;
import org.opendaylight.controller.cluster.dom.api.LeaderLocationListener;
import org.opendaylight.controller.cluster.dom.api.LeaderLocationListenerRegistration;
import org.opendaylight.controller.cluster.raft.LeadershipTransferFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;


/**
 * Default {@link CDSShardAccess} implementation. Listens on leader location
 * change events and distributes them to registered listeners. Also updates
 * current information about leader location accordingly.
 *
 * <p>
 * Sends {@link MakeLeaderLocal} message to local shards and translates its result
 * on behalf users {@link #makeLeaderLocal()} calls.
 *
 * <p>
 * {@link org.opendaylight.controller.cluster.dom.api.CDSDataTreeProducer} that
 * creates instances of this class has to call {@link #close()} once it is no
 * longer valid.
 */
final class CDSShardAccessImpl implements CDSShardAccess, LeaderLocationListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CDSShardAccessImpl.class);

    private final Collection<LeaderLocationListener> listeners = ConcurrentHashMap.newKeySet();
    private final DOMDataTreeIdentifier prefix;
    private final ActorContext actorContext;
    private final Timeout makeLeaderLocalTimeout;

    private ActorRef roleChangeListenerActor;

    private volatile LeaderLocation currentLeader = LeaderLocation.UNKNOWN;
    private volatile boolean closed = false;

    CDSShardAccessImpl(final DOMDataTreeIdentifier prefix, final ActorContext actorContext) {
        this.prefix = Preconditions.checkNotNull(prefix);
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.makeLeaderLocalTimeout =
                new Timeout(actorContext.getDatastoreContext().getShardLeaderElectionTimeout().duration().$times(2));

        // register RoleChangeListenerActor
        // TODO Maybe we should do this in async
        final Optional<ActorRef> localShardReply =
                actorContext.findLocalShard(ClusterUtils.getCleanShardName(prefix.getRootIdentifier()));
        Preconditions.checkState(localShardReply.isPresent(),
                "Local shard for {} not present. Cannot register RoleChangeListenerActor", prefix);
        roleChangeListenerActor =
                actorContext.getActorSystem().actorOf(RoleChangeListenerActor.props(localShardReply.get(), this));
    }

    private void checkNotClosed() {
        Preconditions.checkState(!closed,
                "CDSDataTreeProducer, that this CDSShardAccess is associated with, is no longer valid");
    }

    @Override
    @Nonnull
    public DOMDataTreeIdentifier getShardIdentifier() {
        checkNotClosed();
        return prefix;
    }

    @Override
    @Nonnull
    public LeaderLocation getLeaderLocation() {
        checkNotClosed();
        // TODO before getting first notification from roleChangeListenerActor
        // we will always return UNKNOWN
        return currentLeader;
    }

    @Override
    @Nonnull
    public CompletionStage<Void> makeLeaderLocal() {
        // TODO when we have running make leader local operation
        // we should just return the same completion stage
        checkNotClosed();

        // TODO can we cache local shard actorRef?
        final Future<ActorRef> localShardReply =
                actorContext.findLocalShardAsync(ClusterUtils.getCleanShardName(prefix.getRootIdentifier()));

        // we have to tell local shard to make leader local
        final scala.concurrent.Promise<Object> makeLeaderLocalAsk = Futures.promise();
        localShardReply.onComplete(new OnComplete<ActorRef>() {
            @Override
            public void onComplete(final Throwable failure, final ActorRef actorRef) throws Throwable {
                if (failure instanceof LocalShardNotFoundException) {
                    LOG.debug("No local shard found for {} - Cannot request leadership transfer to local shard.",
                            getShardIdentifier(), failure);
                    makeLeaderLocalAsk.failure(failure);
                } else if (failure != null) {
                    // TODO should this be WARN?
                    LOG.debug("Failed to find local shard for {} - Cannot request leadership transfer to local shard.",
                            getShardIdentifier(), failure);
                    makeLeaderLocalAsk.failure(failure);
                } else {
                    makeLeaderLocalAsk
                            .completeWith(actorContext
                                    .executeOperationAsync(actorRef, MakeLeaderLocal.INSTANCE, makeLeaderLocalTimeout));
                }
            }
        }, actorContext.getClientDispatcher());

        // we have to transform make leader local request result
        Future<Void> makeLeaderLocalFuture = makeLeaderLocalAsk.future()
                .transform(new Mapper<Object, Void>() {
                    @Override
                    public Void apply(final Object parameter) {
                        return null;
                    }
                }, new Mapper<Throwable, Throwable>() {
                    @Override
                    public Throwable apply(final Throwable parameter) {
                        if (parameter instanceof LeadershipTransferFailedException) {
                            // do nothing with exception and just pass it as it is
                            return parameter;
                        }
                        // wrap exception in LeadershipTransferFailedEx
                        return new LeadershipTransferFailedException("Leadership transfer failed", parameter);
                    }
                }, actorContext.getClientDispatcher());

        return FutureConverters.toJava(makeLeaderLocalFuture);
    }

    @Override
    @Nonnull
    public <L extends LeaderLocationListener> LeaderLocationListenerRegistration<L>
            registerLeaderLocationListener(@Nonnull final L listener) {
        checkNotClosed();
        Preconditions.checkNotNull(listener);
        Preconditions.checkArgument(!listeners.contains(listener),
                "Listener {} is already registered with ShardAccess {}", listener, this);

        LOG.debug("Registering LeaderLocationListener {}", listener);

        listeners.add(listener);

        return new LeaderLocationListenerRegistration<L>() {
            @Override
            public L getInstance() {
                return listener;
            }

            @Override
            public void close() {
                listeners.remove(listener);
            }
        };
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void onLeaderLocationChanged(@Nonnull final LeaderLocation location) {
        if (closed) {
            // we are closed already. Do not dispatch any new leader location
            // change events.
            return;
        }

        LOG.debug("Received leader location change notification. New leader location: {}", location);
        currentLeader = location;
        listeners.forEach(listener -> {
            try {
                listener.onLeaderLocationChanged(location);
            } catch (Exception e) {
                LOG.warn("Ignoring uncaught exception thrown be LeaderLocationListener {} "
                        + "during processing leader location change {}", listener, location, e);
            }
        });
    }

    @Override
    public void close() {
        // TODO should we also remove all listeners?
        LOG.debug("Closing {} ShardAccess", prefix);
        closed = true;

        if (roleChangeListenerActor != null) {
            // stop RoleChangeListenerActor
            roleChangeListenerActor.tell(PoisonPill.getInstance(), noSender());
            roleChangeListenerActor = null;
        }
    }
}
