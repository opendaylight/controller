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
import com.google.common.base.Throwables;
import com.google.common.primitives.UnsignedLong;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.client.BackendInfoResolver;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.common.actor.ExplicitAsk;
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
     * Fall-over-dead timeout. If we do not make progress in this long, just fall over and propagate the failure.
     * All users are expected to fail, possibly attempting to recover by restarting. It is fair to remain
     * non-operational.
     */
    // TODO: maybe make this configurable somehow?
    private static final Timeout DEAD_TIMEOUT = Timeout.apply(15, TimeUnit.MINUTES);

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

        return new ShardState(FutureConverters.toJava(actorContext.findPrimaryShardAsync(shardName)).thenCompose(i -> {
            LOG.debug("Looking up primary info for {} from {}", shardName, i);
            return FutureConverters.toJava(ExplicitAsk.ask(i.getPrimaryShardActor(), connectFunction, DEAD_TIMEOUT));
        }).thenApply(response -> {
            if (response instanceof RequestFailure) {
                final RequestFailure<?, ?> failure = (RequestFailure<?, ?>) response;
                LOG.debug("Connect request failed {}", failure, failure.getCause());
                throw Throwables.propagate(failure.getCause());
            }

            LOG.debug("Resolved backend information to {}", response);

            Preconditions.checkArgument(response instanceof ConnectClientSuccess, "Unhandled response {}", response);
            final ConnectClientSuccess success = (ConnectClientSuccess) response;

            return new ShardBackendInfo(success.getBackend(),
                nextSessionId.getAndIncrement(), success.getVersion(), shardName, UnsignedLong.fromLongBits(cookie),
                success.getDataTree(), success.getMaxMessages());
        }));
    }
}
