/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.shardstrategy;

import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Shard Strategy that resolves a path into prefix shard name
 */
public class PrefixShardStrategy implements ShardStrategy {

    public static final String NAME = "prefix";

    private final String shardName;
    private final Configuration configuration;

    public PrefixShardStrategy(final String shardName, final Configuration configuration) {
        this.shardName = shardName;
        this.configuration = configuration;
    }

    @Override
    public String findShard(final YangInstanceIdentifier path) {
        final String shardNameForPrefix = configuration.getShardNameForPrefix(path);
        return shardName != null ? shardName : DefaultShardStrategy.DEFAULT_SHARD;
    }
}
