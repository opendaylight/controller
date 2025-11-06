/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;

/**
 * Maintains a cache of PrimaryShardInfo Future instances per shard.
 *
 * @author Thomas Pantelis
 */
public class PrimaryShardInfoFutureCache {
    private final Cache<String, CompletionStage<PrimaryShardInfo>> primaryShardInfoCache =
        CacheBuilder.newBuilder().build();

    public @Nullable CompletionStage<PrimaryShardInfo> getIfPresent(final @NonNull String shardName) {
        return primaryShardInfoCache.getIfPresent(shardName);
    }

    public void putSuccessful(final @NonNull String shardName, final @NonNull PrimaryShardInfo info) {
        primaryShardInfoCache.put(shardName, CompletableFuture.completedStage(info));
    }

    public void remove(@NonNull
    final String shardName) {
        primaryShardInfoCache.invalidate(shardName);
    }
}
