/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;

/**
 * A message sent to ShardManager to gracefully shutdown local
 * module-based shard replica.
 */
public class ShutdownShardReplica {

    private final String shardName;

    public ShutdownShardReplica(final String shardName) {
        this.shardName = Preconditions.checkNotNull(shardName);
    }

    public String getShardName() {
        return shardName;
    }

    @Override
    public String toString() {
        return "ShutdownShardReplica [shardName=" + shardName + "]";
    }
}
