/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.primitives.UnsignedLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.actors.client.BackendInfo;
import org.opendaylight.controller.cluster.datastore.actors.client.BackendInfoResolver;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.compat.java8.FutureConverters;

/**
 * {@link BackendInfoResolver} implementation for static shard configuration based on ShardManager. Each string-named
 * shard is assigned a single cookie and this mapping is stored in a bidirectional map. Information about corresponding
 * shard leader is resolved via {@link ActorContext}. The product of resolution is {@link ShardBackendInfo}.
 *
 * @author Robert Varga
 */
final class ModuleShardBackendResolver extends BackendInfoResolver<ShardBackendInfo> {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleShardBackendResolver.class);
    /**
     * Fall-over-dead timeout. If we do not make progress in this long, just fall over and propagate the failure.
     * All users are expected to fail, possibly attempting to recover by restarting. It is fair to remain
     * non-operational.
     */
    // TODO: maybe make this configurable somehow?
    private static final Timeout DEAD_TIMEOUT = Timeout.apply(15, TimeUnit.MINUTES);

    private final ActorContext ctx;

    private volatile BiMap<String, Long> shards = ImmutableBiMap.of();

    // FIXME: we really need just ActorContext.findPrimaryShardAsync()
    ModuleShardBackendResolver(final ActorContext ctx) {
        this.ctx = Preconditions.checkNotNull(ctx);
    }

    @Override
    protected void invalidateBackendInfo(final CompletionStage<? extends BackendInfo> info) {
        LOG.trace("Initiated invalidation of backend information {}", info);
        info.thenAccept(this::invalidate);
    }

    private void invalidate(final BackendInfo result) {
        Preconditions.checkArgument(result instanceof ShardBackendInfo);
        LOG.debug("Invalidating backend information {}", result);
        ctx.getPrimaryShardInfoCache().remove(((ShardBackendInfo)result).getShardName());
    }

    @Override
    protected CompletionStage<ShardBackendInfo> resolveBackendInfo(final Long cookie) {
        final String shardName = shards.inverse().get(cookie);
        if (shardName == null) {
            LOG.warn("Failing request for non-existent cookie {}", cookie);
            return CompletableFuture.completedFuture(null);
        }

        LOG.debug("Resolving cookie {} to shard {}", cookie, shardName);
        return FutureConverters.toJava(ctx.findPrimaryShardAsync(shardName))
                .thenApply(o -> createBackendInfo(o, shardName, cookie));
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
