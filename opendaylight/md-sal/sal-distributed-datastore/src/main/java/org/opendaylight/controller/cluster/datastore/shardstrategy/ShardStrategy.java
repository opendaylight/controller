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
 * The role of ShardStrategy is to figure out which shards a given piece of data belongs to.
 */
public interface ShardStrategy {
    /**
     * Find the name of the shard in which the data pointed to by the specified path belongs in.
     *
     * <p>
     * Should return the name of the default shard DefaultShardStrategy.DEFAULT_SHARD
     * if no matching shard was found
     *
     * @param path the location of the data in the logical tree
     * @return the corresponding shard name.
     */
    String findShard(YangInstanceIdentifier path);
}
