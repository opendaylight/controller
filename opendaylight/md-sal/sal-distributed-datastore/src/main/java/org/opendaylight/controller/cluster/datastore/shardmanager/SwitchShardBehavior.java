/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.shardmanager;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.raft.RaftState;

final class SwitchShardBehavior {
    private final ShardIdentifier shardId;
    private final RaftState newState;
    private final long term;

    SwitchShardBehavior(final ShardIdentifier shardId, final RaftState newState, final long term) {
        this.newState = Preconditions.checkNotNull(newState);
        this.shardId = shardId;
        this.term = term;
    }

    @Nullable ShardIdentifier getShardId() {
        return shardId;
    }

    RaftState getNewState() {
        return newState;
    }

    long getTerm() {
        return term;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SwitchShardBehavior{");
        sb.append("shardId='").append(shardId).append('\'');
        sb.append(", newState='").append(newState).append('\'');
        sb.append(", term=").append(term);
        sb.append('}');
        return sb.toString();
    }
}
