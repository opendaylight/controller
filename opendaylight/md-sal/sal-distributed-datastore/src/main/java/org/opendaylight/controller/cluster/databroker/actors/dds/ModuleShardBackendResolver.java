/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.collect.ImmutableBiMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.pekko.dispatch.ExecutionContexts;
import org.apache.pekko.dispatch.OnComplete;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.client.BackendInfoResolver;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.datastore.shardmanager.RegisterForShardAvailabilityChanges;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * {@link BackendInfoResolver} implementation for static shard configuration based on ShardManager. Each string-named
 * shard is assigned a single cookie and this mapping is stored in a bidirectional map. Information about corresponding
 * shard leader is resolved via {@link ActorUtils}. The product of resolution is {@link ShardBackendInfo}.
 *
 * <p>This class is thread-safe.
 */
final class ModuleShardBackendResolver extends AbstractShardBackendResolver {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleShardBackendResolver.class);

    private final ConcurrentMap<Long, ResolvingBackendInfo> backends = new ConcurrentHashMap<>();

    private final Future<Registration> shardAvailabilityChangesRegFuture;

    private @GuardedBy("this") long nextShard = 1;

    private volatile ImmutableBiMap<String, Long> shards = ImmutableBiMap.of(DefaultShardStrategy.DEFAULT_SHARD, 0L);

    // FIXME: we really need just ActorContext.findPrimaryShardAsync()
    ModuleShardBackendResolver(final ClientIdentifier clientId, final ActorUtils actorUtils) {
        super(clientId, actorUtils);

        shardAvailabilityChangesRegFuture = Patterns.ask(actorUtils.getShardManager(),
            new RegisterForShardAvailabilityChanges(this::onShardAvailabilityChange),
            Timeout.apply(60, TimeUnit.MINUTES))
                .map(reply -> (Registration)reply, ExecutionContexts.global());

        shardAvailabilityChangesRegFuture.onComplete(new OnComplete<Registration>() {
            @Override
            public void onComplete(final Throwable failure, final Registration reply) {
                if (failure != null) {
                    LOG.error("RegisterForShardAvailabilityChanges failed", failure);
                }
            }
        }, ExecutionContexts.global());
    }

    private void onShardAvailabilityChange(final String shardName) {
        LOG.debug("onShardAvailabilityChange for {}", shardName);

        Long cookie = shards.get(shardName);
        if (cookie == null) {
            LOG.debug("No shard cookie found for {}", shardName);
            return;
        }

        notifyStaleBackendInfoCallbacks(cookie);
    }

    Long resolveShardForPath(final YangInstanceIdentifier path) {
        return resolveCookie(actorUtils().getShardStrategyFactory().getStrategy(path).findShard(path));
    }

    Stream<Long> resolveAllShards() {
        return actorUtils().getConfiguration().getAllShardNames().stream()
            .sorted()
            .map(this::resolveCookie);
    }

    private @NonNull Long resolveCookie(final String shardName) {
        final Long cookie = shards.get(shardName);
        return cookie != null ? cookie : populateShard(shardName);
    }

    private synchronized @NonNull Long populateShard(final String shardName) {
        Long cookie = shards.get(shardName);
        if (cookie == null) {
            cookie = nextShard++;
            shards = ImmutableBiMap.<String, Long>builder().putAll(shards).put(shardName, cookie).build();
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
        final ResolvingBackendInfo existing = backends.get(cookie);
        if (existing != null) {
            return existing.stage();
        }

        final String shardName = shards.inverse().get(cookie);
        if (shardName == null) {
            LOG.warn("Failing request for non-existent cookie {}", cookie);
            throw new IllegalArgumentException("Cookie " + cookie + " does not have a shard assigned");
        }

        LOG.debug("Resolving cookie {} to shard {}", cookie, shardName);
        final ResolvingBackendInfo toInsert = resolveBackendInfo(shardName, cookie);

        final ResolvingBackendInfo raced = backends.putIfAbsent(cookie, toInsert);
        if (raced != null) {
            // We have had a concurrent insertion, return that
            LOG.debug("Race during insertion of state for cookie {} shard {}", cookie, shardName);
            return raced.stage();
        }

        // We have succeeded in populating the map, now we need to take care of pruning the entry if it fails to
        // complete
        final CompletionStage<ShardBackendInfo> stage = toInsert.stage();
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
        final ResolvingBackendInfo existing = backends.get(cookie);
        if (existing != null) {
            if (!staleInfo.equals(existing.result())) {
                return existing.stage();
            }

            LOG.debug("Invalidating backend information {}", staleInfo);
            flushCache(staleInfo.getName());

            LOG.trace("Invalidated cache {}", staleInfo);
            backends.remove(cookie, existing);
        }

        return getBackendInfo(cookie);
    }

    @Override
    public void close() {
        shardAvailabilityChangesRegFuture.onComplete(new OnComplete<Registration>() {
            @Override
            public void onComplete(final Throwable failure, final Registration reply) {
                reply.close();
            }
        }, ExecutionContexts.global());
    }

    @Override
    public String resolveCookieName(final Long cookie) {
        return verifyNotNull(shards.inverse().get(cookie), "Unexpected null cookie: %s", cookie);
    }
}
