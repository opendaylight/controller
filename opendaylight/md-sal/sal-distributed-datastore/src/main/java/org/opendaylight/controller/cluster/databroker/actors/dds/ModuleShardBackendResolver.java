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
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.google.common.primitives.UnsignedLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.client.BackendInfo;
import org.opendaylight.controller.cluster.access.client.BackendInfoResolver;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.common.actor.ExplicitAsk;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
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
final class ModuleShardBackendResolver extends BackendInfoResolver<ShardBackendInfo> {
    private static final CompletableFuture<ShardBackendInfo> NULL_FUTURE = CompletableFuture.completedFuture(null);
    private static final Logger LOG = LoggerFactory.getLogger(ModuleShardBackendResolver.class);

    /**
     * Fall-over-dead timeout. If we do not make progress in this long, just fall over and propagate the failure.
     * All users are expected to fail, possibly attempting to recover by restarting. It is fair to remain
     * non-operational.
     */
    // TODO: maybe make this configurable somehow?
    private static final Timeout DEAD_TIMEOUT = Timeout.apply(15, TimeUnit.MINUTES);

    private final ActorContext actorContext;
    // FIXME: this counter should be in superclass somewhere
    private final AtomicLong nextSessionId = new AtomicLong();
    private final Function1<ActorRef, ?> connectFunction;

    @GuardedBy("this")
    private long nextShard = 1;

    private volatile BiMap<String, Long> shards = ImmutableBiMap.of(DefaultShardStrategy.DEFAULT_SHARD, 0L);

    // FIXME: we really need just ActorContext.findPrimaryShardAsync()
    ModuleShardBackendResolver(final ClientIdentifier clientId, final ActorContext actorContext) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.connectFunction = ExplicitAsk.toScala(t -> new ConnectClientRequest(clientId, t, ABIVersion.BORON,
            ABIVersion.current()));
    }

    @Override
    protected void invalidateBackendInfo(final CompletionStage<? extends BackendInfo> info) {
        LOG.trace("Initiated invalidation of backend information {}", info);
        info.thenAccept(this::invalidate);
    }

    private void invalidate(final BackendInfo result) {
        Preconditions.checkArgument(result instanceof ShardBackendInfo);
        LOG.debug("Invalidating backend information {}", result);
        actorContext.getPrimaryShardInfoCache().remove(((ShardBackendInfo)result).getShardName());
    }

    Long resolveShardForPath(final YangInstanceIdentifier path) {
        final String shardName = actorContext.getShardStrategyFactory().getStrategy(path).findShard(path);
        Long cookie = shards.get(shardName);
        if (cookie == null) {
            synchronized (this) {
                cookie = shards.get(shardName);
                if (cookie == null) {
                    cookie = nextShard++;

                    Builder<String, Long> b = ImmutableBiMap.builder();
                    b.putAll(shards);
                    b.put(shardName, cookie);
                    shards = b.build();
                }
            }
        }

        return cookie;
    }

    @Override
    protected CompletableFuture<ShardBackendInfo> resolveBackendInfo(final Long cookie) {
        final String shardName = shards.inverse().get(cookie);
        if (shardName == null) {
            LOG.warn("Failing request for non-existent cookie {}", cookie);
            return NULL_FUTURE;
        }

        final CompletableFuture<ShardBackendInfo> ret = new CompletableFuture<ShardBackendInfo>();

        FutureConverters.toJava(actorContext.findPrimaryShardAsync(shardName)).thenCompose(info -> {
            LOG.debug("Looking up primary info for {} from {}", shardName, info);
            return FutureConverters.toJava(ExplicitAsk.ask(info.getPrimaryShardActor(), connectFunction, DEAD_TIMEOUT));
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
        }).whenComplete((info, t) -> {
            if (t != null) {
                ret.completeExceptionally(t);
            } else {
                ret.complete(info);
            }
        });

        LOG.debug("Resolving cookie {} to shard {}", cookie, shardName);
        return ret;
    }
}
