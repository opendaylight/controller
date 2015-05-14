/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import akka.dispatch.Futures;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import scala.concurrent.Future;

/**
 * Maintains a cache of PrimaryShardInfo Future instances per shard.
 *
 * @author Thomas Pantelis
 */
public class PrimaryShardInfoFutureCache {
    private final Cache<String, Future<PrimaryShardInfo>> primaryShardInfoCache = CacheBuilder.newBuilder().build();

    @GuardedBy("shardInfoListeners")
    private final Collection<ShardInfoListenerRegistration<?>> shardInfoListeners = new ArrayList<>();

    public @Nullable Future<PrimaryShardInfo> getIfPresent(@Nonnull String shardName) {
        return primaryShardInfoCache.getIfPresent(shardName);
    }

    public void putSuccessful(@Nonnull String shardName, @Nonnull PrimaryShardInfo info) {
        primaryShardInfoCache.put(shardName, Futures.successful(info));

        synchronized (shardInfoListeners) {
            for (ShardInfoListenerRegistration<?> reg : shardInfoListeners) {
                reg.getInstance().onShardInfoUpdated(shardName, info);
            }
        }
    }

    public void remove(@Nonnull String shardName) {
        primaryShardInfoCache.invalidate(shardName);

        synchronized (shardInfoListeners) {
            for (ShardInfoListenerRegistration<?> reg : shardInfoListeners) {
                reg.getInstance().onShardInfoUpdated(shardName, null);
            }
        }
    }

    public <T extends ShardInfoListener> ShardInfoListenerRegistration<T> registerShardInfoListener(@Nonnull final T listener) {
        final ShardInfoListenerRegistration<T> reg = new ShardInfoListenerRegistration<T>(listener, this);

        synchronized (shardInfoListeners) {
            shardInfoListeners.add(reg);
        }
        return reg;
    }

    void removeShardInfoListener(@Nonnull final ShardInfoListenerRegistration<?> registration) {
        synchronized (shardInfoListeners) {
            shardInfoListeners.remove(registration);
        }
    }
}
