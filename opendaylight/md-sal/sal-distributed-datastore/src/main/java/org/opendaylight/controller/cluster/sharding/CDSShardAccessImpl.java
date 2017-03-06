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
import akka.dispatch.Mapper;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.datastore.messages.MakeLeaderLocal;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.dom.api.CDSShardAccess;
import org.opendaylight.controller.cluster.dom.api.LeaderLocation;
import org.opendaylight.controller.cluster.dom.api.LeaderLocationListener;
import org.opendaylight.controller.cluster.dom.api.LeaderLocationListenerRegistration;
import org.opendaylight.controller.cluster.raft.messages.RequestLeadershipReply;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;

final class CDSShardAccessImpl implements CDSShardAccess, LeaderLocationListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CDSShardAccessImpl.class);

    private final DOMDataTreeIdentifier prefix;
    private final ActorContext actorContext;
    private final Collection<LeaderLocationListener> listeners = Sets.newHashSet();
    private final Timeout makeLeaderLocalTimeout;
    private ActorRef notifyActor;

    private LeaderLocation currentLeader = LeaderLocation.UNKNOWN;
    private boolean closed = false;

    CDSShardAccessImpl(final DOMDataTreeIdentifier prefix, final ActorContext actorContext) {
        this.prefix = Preconditions.checkNotNull(prefix);
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.makeLeaderLocalTimeout =
                new Timeout(actorContext.getDatastoreContext().getShardLeaderElectionTimeout().duration().$times(2));
        init();
    }

    private void init() {
        final Optional<ActorRef> localShardReply =
                actorContext.findLocalShard(ClusterUtils.getCleanShardName(prefix.getRootIdentifier()));
        Preconditions.checkState(localShardReply.isPresent());
        notifyActor = actorContext.getActorSystem().actorOf(NotifySubscriberActor.props(localShardReply.get(), this));
    }

    private void checkNotClosed() {
        Preconditions.checkState(!closed);
    }

    public DOMDataTreeIdentifier getShardIdentifier() {
        checkNotClosed();
        return prefix;
    }

    public LeaderLocation getLeaderLocation() {
        checkNotClosed();
        return currentLeader;
    }

    public CompletionStage<Void> makeLeaderLocal() {
        checkNotClosed();
        final Optional<ActorRef> localShardReply =
                actorContext.findLocalShard(ClusterUtils.getCleanShardName(prefix.getRootIdentifier()));
        if (!localShardReply.isPresent()) {
            final CompletableFuture result = new CompletableFuture();
            result.completeExceptionally(new LeadershipTransferException(
                    "Cannot transfer leadership to local shard. Local shard is not present"));
            return result;
        }

        final ActorRef localShard = localShardReply.get();
        Future<Object> ask =
                actorContext.executeOperationAsync(localShard, new MakeLeaderLocal(), makeLeaderLocalTimeout);

        Future<Void> makeLeadeLocalFuture = ask.transform(new Mapper<Object, Void>() {
            @Override
            public Void checkedApply(final Object parameter) throws LeadershipTransferException {
                if (parameter instanceof RequestLeadershipReply) {
                    final boolean success = ((RequestLeadershipReply) parameter).isSuccess();
                    if (success) {
                        return null;
                    }
                }
                throw new LeadershipTransferException("Leadership transfer failed");
            }
        }, new Mapper<Throwable, Throwable>() {
            @Override
            public Throwable apply(final Throwable parameter) {
                return new LeadershipTransferException("Leadership transfer failed", parameter);
            }
        }, actorContext.getClientDispatcher());

        return FutureConverters.toJava(makeLeadeLocalFuture);
    }

    public <L extends LeaderLocationListener> LeaderLocationListenerRegistration<L>
            registerLeaderLocationListener(final L listener) {
        checkNotClosed();

        Preconditions.checkNotNull(listener);
        Preconditions.checkArgument(listeners.contains(listener));

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

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void onLeaderLocationChanged(final LeaderLocation location) {
        if (closed) {
            return;
        }

        currentLeader = location;
        listeners.forEach(listener -> {
            try {
                listener.onLeaderLocationChanged(location);
            } catch (Exception e) {
                LOG.warn("LeaderLocationListener {} threw an exception {}", listener, e);
            }
        });
    }

    @Override
    public void close() throws Exception {
        closed = true;
        notifyActor.tell(PoisonPill.getInstance(), noSender());
    }
}
