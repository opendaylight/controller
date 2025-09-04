/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A local message sent to the ShardManager to flip the raft voting states for members of a shard.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
public record FlipShardMembersVotingStatus(String shardName) {
    public FlipShardMembersVotingStatus {
        requireNonNull(shardName);
    }
}
