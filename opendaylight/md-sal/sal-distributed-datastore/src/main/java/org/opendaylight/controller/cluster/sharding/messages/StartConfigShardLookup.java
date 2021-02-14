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
 * Message that should be sent to ShardedDataTreeActor when the lookup of the prefix config shard should begin.
 * Replied to with Succes once the shard has a leader.
 */
@Deprecated(forRemoval = true)
public class StartConfigShardLookup {

    private final LogicalDatastoreType type;

    public StartConfigShardLookup(final LogicalDatastoreType type) {
        this.type = type;
    }

    public LogicalDatastoreType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "StartConfigShardLookup{type=" + type + '}';
    }
}
