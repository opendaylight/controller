/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
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
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Function1;
import scala.compat.java8.FutureConverters;

/**
 * {@link BackendInfoResolver} implementation for static shard configuration based on ShardManager. Each string-named
 * shard is assigned a single cookie and this mapping is stored in a bidirectional map. Information about corresponding
 * shard leader is resolved via {@link ActorContext}. The product of resolution is {@link ShardBackendInfo}.
 *
 * @author Robert Varga
 */
@ThreadSafe
abstract class AbstractShardBackendResolver extends BackendInfoResolver<ShardBackendInfo> {
    static final class ShardState {
        private final CompletionStage<ShardBackendInfo> stage;
        @GuardedBy("this")
        private ShardBackendInfo result;

        ShardState(final CompletionStage<ShardBackendInfo> stage) {
            this.stage = Preconditions.checkNotNull(stage);
            stage.whenComplete(this::onStageResolved);
        }

        @Nonnull CompletionStage<ShardBackendInfo> getStage() {
            return stage;
        }

        @Nullable synchronized ShardBackendInfo getResult() {
            return result;
        }

        private synchronized void onStageResolved(final ShardBackendInfo result, final Throwable failure) {
            if (failure == null) {
                this.result = Preconditions.checkNotNull(result);
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
    private final ActorContext actorContext;

    // FIXME: we really need just ActorContext.findPrimaryShardAsync()
    AbstractShardBackendResolver(final ClientIdentifier clientId, final ActorContext actorContext) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.connectFunction = ExplicitAsk.toScala(t -> new ConnectClientRequest(clientId, t, ABIVersion.BORON,
            ABIVersion.current()));
    }

    protected final void flushCache(final String shardName) {
        actorContext.getPrimaryShardInfoCache().remove(shardName);
    }

    protected final ShardState resolveBackendInfo(final String shardName, final long cookie) {
        LOG.debug("Resolving cookie {} to shard {}", cookie, shardName);

        final CompletableFuture<ShardBackendInfo> future = new CompletableFuture<>();
        FutureConverters.toJava(actorContext.findPrimaryShardAsync(shardName)).whenComplete((info, failure) -> {
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
        ret.initCause(Preconditions.checkNotNull(cause));
        return ret;
    }

    private void connectShard(final String shardName, final long cookie, final PrimaryShardInfo info,
            final CompletableFuture<ShardBackendInfo> future) {
        LOG.debug("Shard {} resolved to {}, attempting to connect", shardName, info);

        FutureConverters.toJava(ExplicitAsk.ask(info.getPrimaryShardActor(), connectFunction, CONNECT_TIMEOUT))
            .whenComplete((response, failure) -> {
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
                Preconditions.checkArgument(response instanceof ConnectClientSuccess, "Unhandled response %s",
                    response);
                final ConnectClientSuccess success = (ConnectClientSuccess) response;
                future.complete(new ShardBackendInfo(success.getBackend(), nextSessionId.getAndIncrement(),
                    success.getVersion(), shardName, UnsignedLong.fromLongBits(cookie), success.getDataTree(),
                    success.getMaxMessages()));
            });
    }
}
