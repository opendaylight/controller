/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.shardstrategy;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Shard Strategy that resolves a path to a prefix shard name.
 */
public class PrefixShardStrategy implements ShardStrategy {

    public static final String NAME = "prefix";

    private final String shardName;
    private final YangInstanceIdentifier prefix;

    public PrefixShardStrategy(final String shardName,
                               final YangInstanceIdentifier prefix) {
        this.shardName = shardName != null ? shardName : DefaultShardStrategy.DEFAULT_SHARD;
        this.prefix = prefix;
    }

    @Override
    public String findShard(final YangInstanceIdentifier path) {
        return shardName;
    }

    @Override
    public YangInstanceIdentifier getPrefixForPath(YangInstanceIdentifier path) {
        return prefix;
    }
}
