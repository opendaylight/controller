/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A message sent to the ShardManager to dynamically add a new local shard that is a replica for an existing shard that
 * is already available in the cluster.
 *
 * @param shardName name of the shard that is to be locally replicated.
 */
@NonNullByDefault
public record AddShardReplica(String shardName) {
    public AddShardReplica {
        requireNonNull(shardName);
    }
}
