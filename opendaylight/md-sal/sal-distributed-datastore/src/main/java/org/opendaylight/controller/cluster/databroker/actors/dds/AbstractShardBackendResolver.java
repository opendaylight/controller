/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static java.util.Objects.requireNonNull;

import com.google.common.primitives.UnsignedLong;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.apache.pekko.dispatch.OnComplete;
import org.apache.pekko.pattern.Patterns;
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
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link BackendInfoResolver} implementation for static shard configuration based on ShardManager. Each string-named
 * shard is assigned a single cookie and this mapping is stored in a bidirectional map. Information about corresponding
 * shard leader is resolved via {@link ActorUtils}. The product of resolution is {@link ShardBackendInfo}.
 *
 * <p>This class is thread-safe.
 */
abstract sealed class AbstractShardBackendResolver extends BackendInfoResolver<ShardBackendInfo>
        permits ModuleShardBackendResolver, SimpleShardBackendResolver {
    /**
     * A future {@link ShardBackendInfo}, which can only resolve successfully.
     */
    static final class ResolvingBackendInfo {
        private final @NonNull CompletionStage<ShardBackendInfo> stage;

        private @GuardedBy("this") ShardBackendInfo result;

        private ResolvingBackendInfo(final CompletionStage<ShardBackendInfo> stage) {
            this.stage = requireNonNull(stage);
            stage.whenComplete(this::onStageResolved);
        }

        @NonNull CompletionStage<ShardBackendInfo> stage() {
            return stage;
        }

        synchronized @Nullable ShardBackendInfo result() {
            return result;
        }

        private synchronized void onStageResolved(final ShardBackendInfo info, final @Nullable Throwable failure) {
            if (failure == null) {
                result = requireNonNull(info);
            } else {
                LOG.warn("Failed to resolve shard", failure);
            }
        }
    }

    private final class StaleCallbackReg extends AbstractRegistration {
        // Note: not LongConsumer because we use it for map lookup
        final Consumer<Long> callback;

        StaleCallbackReg(final Consumer<Long> callback) {
            this.callback = requireNonNull(callback);
        }

        @Override
        protected void removeRegistration() {
            staleBackendInfoCallbacks.remove(this);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractShardBackendResolver.class);

    /**
     * Connect request timeout. If the shard does not respond within this interval, we retry the lookup and connection.
     */
    // TODO: maybe make this configurable somehow?
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final AtomicLong nextSessionId = new AtomicLong();
    private final ClientIdentifier clientId;
    private final ActorUtils actorUtils;
    private final Set<StaleCallbackReg> staleBackendInfoCallbacks = ConcurrentHashMap.newKeySet();

    // FIXME: we really need just ActorContext.findPrimaryShardAsync()
    AbstractShardBackendResolver(final ClientIdentifier clientId, final ActorUtils actorUtils) {
        this.actorUtils = requireNonNull(actorUtils);
        this.clientId = requireNonNull(clientId);
    }

    @Override
    public final Registration notifyWhenBackendInfoIsStale(final Consumer<Long> callback) {
        final var reg = new StaleCallbackReg(callback);
        staleBackendInfoCallbacks.add(reg);
        return reg;
    }

    final void notifyStaleBackendInfoCallbacks(final @NonNull Long cookie) {
        staleBackendInfoCallbacks.forEach(reg -> reg.callback.accept(cookie));
    }

    final ActorUtils actorUtils() {
        return actorUtils;
    }

    final void flushCache(final String shardName) {
        actorUtils.getPrimaryShardInfoCache().remove(shardName);
    }

    final @NonNull ResolvingBackendInfo resolveBackendInfo(final String shardName, final long cookie) {
        LOG.debug("Resolving cookie {} to shard {}", cookie, shardName);

        final var future = new CompletableFuture<ShardBackendInfo>();
        actorUtils.findPrimaryShardAsync(shardName).onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final PrimaryShardInfo success) {
                if (failure != null) {
                    LOG.debug("Shard {} failed to resolve", shardName, failure);
                    switch (failure) {
                        case NoShardLeaderException ex -> future.completeExceptionally(
                            wrap("Shard has no current leader", ex));
                        case NotInitializedException ex -> {
                            // FIXME: this actually is an exception we can retry on
                            LOG.info("Shard {} has not initialized yet", shardName);
                            future.completeExceptionally(ex);
                        }
                        case PrimaryNotFoundException ex -> {
                            LOG.info("Failed to find primary for shard {}", shardName);
                            future.completeExceptionally(ex);
                        }
                        default -> future.completeExceptionally(failure);
                    }
                } else {
                    connectShard(shardName, cookie, success, future);
                }
            }
        }, actorUtils.getClientDispatcher());

        return new ResolvingBackendInfo(future);
    }

    private static TimeoutException wrap(final String message, final Throwable cause) {
        final var ret = new TimeoutException(message);
        ret.initCause(requireNonNull(cause));
        return ret;
    }

    private void connectShard(final String shardName, final long cookie, final PrimaryShardInfo info,
            final CompletableFuture<ShardBackendInfo> future) {
        LOG.debug("Shard {} resolved to {}, attempting to connect", shardName, info);

        Patterns.askWithReplyTo(info.getPrimaryShardActor(),
            t -> new ConnectClientRequest(clientId, t, ABIVersion.POTASSIUM, ABIVersion.current()), CONNECT_TIMEOUT)
            .whenComplete((response, failure) -> onConnectResponse(shardName, cookie, future, response, failure));
    }

    private void onConnectResponse(final String shardName, final long cookie,
            final CompletableFuture<ShardBackendInfo> future, final Object response, final Throwable failure) {
        if (failure != null) {
            LOG.debug("Connect attempt to {} failed, will retry", shardName, failure);
            future.completeExceptionally(wrap("Connection attempt failed", failure));
            return;
        }
        if (response instanceof RequestFailure<?, ?> reqFailure) {
            final var cause = reqFailure.getCause().unwrap();
            LOG.debug("Connect attempt to {} failed to process", shardName, cause);
            final var result = cause instanceof NotLeaderException notLeader
                    ? wrap("Leader moved during establishment", notLeader) : cause;
            future.completeExceptionally(result);
            return;
        }

        LOG.debug("Resolved backend information to {}", response);
        if (response instanceof ConnectClientSuccess success) {
            future.complete(new ShardBackendInfo(success.getBackend(), nextSessionId.getAndIncrement(),
                success.getVersion(), shardName, UnsignedLong.fromLongBits(cookie), success.getDataTree(),
                success.getMaxMessages()));
        } else {
            throw new IllegalArgumentException("Unhandled response " + response);
        }
    }
}
