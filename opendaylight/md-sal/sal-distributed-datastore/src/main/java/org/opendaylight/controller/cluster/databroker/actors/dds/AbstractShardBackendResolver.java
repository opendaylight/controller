/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.primitives.UnsignedLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.util.Timeout;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.client.BackendInfoResolver;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.access.commands.NotLeaderException;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.common.actor.ExplicitAsk;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Function1;
import scala.compat.java8.FutureConverters;

/**
 * {@link BackendInfoResolver} implementation for static shard configuration based on ShardManager. Each string-named
 * shard is assigned a single cookie and this mapping is stored in a bidirectional map. Information about corresponding
 * shard leader is resolved via {@link ActorUtils}. The product of resolution is {@link ShardBackendInfo}.
 *
 * <p>This class is thread-safe.
 */
abstract class AbstractShardBackendResolver extends BackendInfoResolver<ShardBackendInfo> {
    static final class ShardState {
        private final CompletionStage<ShardBackendInfo> stage;
        @GuardedBy("this")
        private ShardBackendInfo result;

        ShardState(final CompletionStage<ShardBackendInfo> stage) {
            this.stage = requireNonNull(stage);
            stage.whenComplete(this::onStageResolved);
        }

        @NonNull CompletionStage<ShardBackendInfo> getStage() {
            return stage;
        }

        synchronized @Nullable ShardBackendInfo getResult() {
            return result;
        }

        private synchronized void onStageResolved(final ShardBackendInfo info, final Throwable failure) {
            if (failure == null) {
                result = requireNonNull(info);
            } else {
                LOG.warn("Failed to resolve shard", failure);
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractShardBackendResolver.class);

    /**
     * Connect request timeout. If the shard does not respond within this interval, we retry the lookup and connection.
     */
    // TODO: maybe make this configurable somehow?
    private static final Timeout CONNECT_TIMEOUT = Timeout.apply(5, TimeUnit.SECONDS);

    private final AtomicLong nextSessionId = new AtomicLong();
    private final Function1<ActorRef, ?> connectFunction;
    private final ActorUtils actorUtils;
    private final Set<Consumer<Long>> staleBackendInfoCallbacks = ConcurrentHashMap.newKeySet();

    // FIXME: we really need just ActorContext.findPrimaryShardAsync()
    AbstractShardBackendResolver(final ClientIdentifier clientId, final ActorUtils actorUtils) {
        this.actorUtils = requireNonNull(actorUtils);
        connectFunction = ExplicitAsk.toScala(t -> new ConnectClientRequest(clientId, t, ABIVersion.POTASSIUM,
            ABIVersion.current()));
    }

    @Override
    public Registration notifyWhenBackendInfoIsStale(final Consumer<Long> callback) {
        staleBackendInfoCallbacks.add(callback);
        return () -> staleBackendInfoCallbacks.remove(callback);
    }

    protected void notifyStaleBackendInfoCallbacks(final Long cookie) {
        staleBackendInfoCallbacks.forEach(callback -> callback.accept(cookie));
    }

    protected ActorUtils actorUtils() {
        return actorUtils;
    }

    protected final void flushCache(final String shardName) {
        actorUtils.getPrimaryShardInfoCache().remove(shardName);
    }

    protected final ShardState resolveBackendInfo(final String shardName, final long cookie) {
        LOG.debug("Resolving cookie {} to shard {}", cookie, shardName);

        final CompletableFuture<ShardBackendInfo> future = new CompletableFuture<>();
        FutureConverters.toJava(actorUtils.findPrimaryShardAsync(shardName)).whenComplete((info, failure) -> {
            if (failure == null) {
                connectShard(shardName, cookie, info, future);
                return;
            }

            LOG.debug("Shard {} failed to resolve", shardName, failure);
            if (failure instanceof NoShardLeaderException) {
                future.completeExceptionally(wrap("Shard has no current leader", failure));
            } else if (failure instanceof NotInitializedException) {
                // FIXME: this actually is an exception we can retry on
                LOG.info("Shard {} has not initialized yet", shardName);
                future.completeExceptionally(failure);
            } else if (failure instanceof PrimaryNotFoundException) {
                LOG.info("Failed to find primary for shard {}", shardName);
                future.completeExceptionally(failure);
            } else {
                future.completeExceptionally(failure);
            }
        });

        return new ShardState(future);
    }

    private static TimeoutException wrap(final String message, final Throwable cause) {
        final TimeoutException ret = new TimeoutException(message);
        ret.initCause(requireNonNull(cause));
        return ret;
    }

    private void connectShard(final String shardName, final long cookie, final PrimaryShardInfo info,
            final CompletableFuture<ShardBackendInfo> future) {
        LOG.debug("Shard {} resolved to {}, attempting to connect", shardName, info);

        FutureConverters.toJava(ExplicitAsk.ask(info.getPrimaryShardActor(), connectFunction, CONNECT_TIMEOUT))
            .whenComplete((response, failure) -> onConnectResponse(shardName, cookie, future, response, failure));
    }

    private void onConnectResponse(final String shardName, final long cookie,
            final CompletableFuture<ShardBackendInfo> future, final Object response, final Throwable failure) {
        if (failure != null) {
            LOG.debug("Connect attempt to {} failed, will retry", shardName, failure);
            future.completeExceptionally(wrap("Connection attempt failed", failure));
            return;
        }
        if (response instanceof RequestFailure) {
            final Throwable cause = ((RequestFailure<?, ?>) response).getCause().unwrap();
            LOG.debug("Connect attempt to {} failed to process", shardName, cause);
            final Throwable result = cause instanceof NotLeaderException
                    ? wrap("Leader moved during establishment", cause) : cause;
            future.completeExceptionally(result);
            return;
        }

        LOG.debug("Resolved backend information to {}", response);
        checkArgument(response instanceof ConnectClientSuccess, "Unhandled response %s", response);
        final ConnectClientSuccess success = (ConnectClientSuccess) response;
        future.complete(new ShardBackendInfo(success.getBackend(), nextSessionId.getAndIncrement(),
            success.getVersion(), shardName, UnsignedLong.fromLongBits(cookie), success.getDataTree(),
            success.getMaxMessages()));
    }
}
