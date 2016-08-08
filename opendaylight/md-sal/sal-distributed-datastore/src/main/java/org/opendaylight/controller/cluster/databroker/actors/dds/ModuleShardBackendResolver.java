/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.dispatch.ExecutionContexts;
import akka.dispatch.OnComplete;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.client.BackendInfo;
import org.opendaylight.controller.cluster.access.client.BackendInfoResolver;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

/**
 * {@link BackendInfoResolver} implementation for static shard configuration based on ShardManager. Each string-named
 * shard is assigned a single cookie and this mapping is stored in a bidirectional map. Information about corresponding
 * shard leader is resolved via {@link ActorContext}. The product of resolution is {@link ShardBackendInfo}.
 *
 * @author Robert Varga
 */
final class ModuleShardBackendResolver extends BackendInfoResolver<ShardBackendInfo> {
    private static final ExecutionContext DIRECT_EXECUTION_CONTEXT =
            ExecutionContexts.fromExecutor(MoreExecutors.directExecutor());
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

    @GuardedBy("this")
    private long nextShard = 1;

    private volatile BiMap<String, Long> shards = ImmutableBiMap.of(DefaultShardStrategy.DEFAULT_SHARD, 0L);

    // FIXME: we really need just ActorContext.findPrimaryShardAsync()
    ModuleShardBackendResolver(final ActorContext actorContext) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
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

        final CompletableFuture<ShardBackendInfo> ret = new CompletableFuture<>();

        actorContext.findPrimaryShardAsync(shardName).onComplete(new OnComplete<PrimaryShardInfo>() {
            @Override
            public void onComplete(final Throwable t, final PrimaryShardInfo v) {
                if (t != null) {
                    ret.completeExceptionally(t);
                } else {
                    ret.complete(createBackendInfo(v, shardName, cookie));
                }
            }
        }, DIRECT_EXECUTION_CONTEXT);

        LOG.debug("Resolving cookie {} to shard {}", cookie, shardName);
        return ret;
    }

    private static ABIVersion toABIVersion(final short version) {
        switch (version) {
            case DataStoreVersions.BORON_VERSION:
                return ABIVersion.BORON;
        }

        throw new IllegalArgumentException("Unsupported version " + version);
    }

    private static ShardBackendInfo createBackendInfo(final Object result, final String shardName, final Long cookie) {
        Preconditions.checkArgument(result instanceof PrimaryShardInfo);
        final PrimaryShardInfo info = (PrimaryShardInfo) result;

        LOG.debug("Creating backend information for {}", info);
        return new ShardBackendInfo(info.getPrimaryShardActor().resolveOne(DEAD_TIMEOUT).value().get().get(),
            toABIVersion(info.getPrimaryShardVersion()), shardName, UnsignedLong.fromLongBits(cookie),
            info.getLocalShardDataTree());
     }
}
