/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.shardstrategy;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * The DefaultShardStrategy basically puts all data into the default Shard
 * <p>
 *   The default shard stores data for all modules for which a specific set of shards has not been configured
 * </p>
 */
public final class DefaultShardStrategy implements ShardStrategy {
    public static final String NAME = "default";
    public static final String DEFAULT_SHARD = "default";
    private static final DefaultShardStrategy INSTANCE = new DefaultShardStrategy();

    private DefaultShardStrategy() {
        // Hidden to force a singleton instnace
    }

    public static DefaultShardStrategy getInstance() {
        return INSTANCE;
    }

    @Override
    public String findShard(YangInstanceIdentifier path) {
        return DEFAULT_SHARD;
    }
}
