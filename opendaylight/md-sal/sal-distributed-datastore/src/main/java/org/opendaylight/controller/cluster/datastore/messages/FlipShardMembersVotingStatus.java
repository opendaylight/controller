/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;

/**
 * A local message sent to the ShardManager to flip the raft voting states for members of a shard.
 *
 * @author Thomas Pantelis
 */
public class FlipShardMembersVotingStatus {
    private final String shardName;

    public FlipShardMembersVotingStatus(String shardName) {
        this.shardName = Preconditions.checkNotNull(shardName);
    }

    public String getShardName() {
        return shardName;
    }

    @Override
    public String toString() {
        return "FlipShardMembersVotingStatus [shardName=" + shardName + "]";
    }
}
