/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.cluster.access.client.BackendInfoResolver;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link BackendInfoResolver} implementation for static shard configuration based on ShardManager. Each string-named
 * shard is assigned a single cookie and this mapping is stored in a bidirectional map. Information about corresponding
 * shard leader is resolved via {@link ActorContext}. The product of resolution is {@link ShardBackendInfo}.
 *
 * @author Robert Varga
 */
@ThreadSafe
final class ModuleShardBackendResolver extends AbstractShardBackendResolver {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleShardBackendResolver.class);

    private final ConcurrentMap<Long, ShardState> backends = new ConcurrentHashMap<>();
    private final ActorContext actorContext;

    @GuardedBy("this")
    private long nextShard = 1;

    private volatile BiMap<String, Long> shards = ImmutableBiMap.of(DefaultShardStrategy.DEFAULT_SHARD, 0L);

    // FIXME: we really need just ActorContext.findPrimaryShardAsync()
    ModuleShardBackendResolver(final ClientIdentifier clientId, final ActorContext actorContext) {
        super(clientId, actorContext);
        this.actorContext = Preconditions.checkNotNull(actorContext);
    }

    Long resolveShardForPath(final YangInstanceIdentifier path) {
        final String shardName = actorContext.getShardStrategyFactory().getStrategy(path).findShard(path);
        Long cookie = shards.get(shardName);
        if (cookie == null) {
            synchronized (this) {
                cookie = shards.get(shardName);
                if (cookie == null) {
                    cookie = nextShard++;

                    Builder<String, Long> builder = ImmutableBiMap.builder();
                    builder.putAll(shards);
                    builder.put(shardName, cookie);
                    shards = builder.build();
                }
            }
        }

        return cookie;
    }


    @Override
    public CompletionStage<ShardBackendInfo> getBackendInfo(final Long cookie) {
        /*
         * We cannot perform a simple computeIfAbsent() here because we need to control sequencing of when the state
         * is inserted into the map and retired from it (based on the stage result).
         *
         * We do not want to hook another stage one processing completes and hooking a removal on failure from a compute
         * method runs the inherent risk of stage completing before the insertion does (i.e. we have a removal of
         * non-existent element.
         */
        final ShardState existing = backends.get(cookie);
        if (existing != null) {
            return existing.getStage();
        }

        final String shardName = shards.inverse().get(cookie);
        if (shardName == null) {
            LOG.warn("Failing request for non-existent cookie {}", cookie);
            throw new IllegalArgumentException("Cookie " + cookie + " does not have a shard assigned");
        }

        LOG.debug("Resolving cookie {} to shard {}", cookie, shardName);
        final ShardState toInsert = resolveBackendInfo(shardName, cookie);

        final ShardState raced = backends.putIfAbsent(cookie, toInsert);
        if (raced != null) {
            // We have had a concurrent insertion, return that
            LOG.debug("Race during insertion of state for cookie {} shard {}", cookie, shardName);
            return raced.getStage();
        }

        // We have succeeded in populating the map, now we need to take care of pruning the entry if it fails to
        // complete
        final CompletionStage<ShardBackendInfo> stage = toInsert.getStage();
        stage.whenComplete((info, failure) -> {
            if (failure != null) {
                LOG.debug("Resolution of cookie {} shard {} failed, removing state", cookie, shardName, failure);
                backends.remove(cookie, toInsert);

                // Remove cache state in case someone else forgot to invalidate it
                flushCache(shardName);
            }
        });

        return stage;
    }

    @Override
    public CompletionStage<ShardBackendInfo> refreshBackendInfo(final Long cookie,
            final ShardBackendInfo staleInfo) {
        final ShardState existing = backends.get(cookie);
        if (existing != null) {
            if (!staleInfo.equals(existing.getResult())) {
                return existing.getStage();
            }

            LOG.debug("Invalidating backend information {}", staleInfo);
            flushCache(staleInfo.getShardName());

            LOG.trace("Invalidated cache %s", staleInfo);
            backends.remove(cookie, existing);
        }

        return getBackendInfo(cookie);
    }
}
