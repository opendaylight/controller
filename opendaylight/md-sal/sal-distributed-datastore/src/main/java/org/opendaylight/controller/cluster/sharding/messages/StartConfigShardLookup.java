/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding.messages;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

/**
 * Message that should be sent to ShardedDataTreeActor when the lookup one of one of the internal shards used
 * for tracking the state of shards and producers.
 * Replied to with Succes once the shard has a leader.
 */
public class StartConfigShardLookup {

    private final LogicalDatastoreType type;
    private final String shardName;

    public StartConfigShardLookup(final LogicalDatastoreType type, final String shardName) {
        this.type = type;
        this.shardName = shardName;
    }

    public LogicalDatastoreType getType() {
        return type;
    }

    public String getShardName() {
        return shardName;
    }

    @Override
    public String toString() {
        return "StartConfigShardLookup{"
                + "type=" + type
                + ", shardName='" + shardName + '\'' + '}';
    }
}
