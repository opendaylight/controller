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
import org.apache.pekko.dispatch.Futures;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import scala.concurrent.Future;

/**
 * Maintains a cache of PrimaryShardInfo Future instances per shard.
 *
 * @author Thomas Pantelis
 */
public class PrimaryShardInfoFutureCache {
    private final Cache<String, Future<PrimaryShardInfo>> primaryShardInfoCache = CacheBuilder.newBuilder().build();

    public @Nullable Future<PrimaryShardInfo> getIfPresent(@NonNull String shardName) {
        return primaryShardInfoCache.getIfPresent(shardName);
    }

    public void putSuccessful(@NonNull String shardName, @NonNull PrimaryShardInfo info) {
        primaryShardInfoCache.put(shardName, Futures.successful(info));
    }

    public void remove(@NonNull String shardName) {
        primaryShardInfoCache.invalidate(shardName);
    }
}
