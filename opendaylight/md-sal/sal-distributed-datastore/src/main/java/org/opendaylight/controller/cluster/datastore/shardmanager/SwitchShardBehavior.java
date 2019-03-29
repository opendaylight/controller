/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.raft.RaftState;

final class SwitchShardBehavior {
    private final ShardIdentifier shardId;
    private final RaftState newState;
    private final long term;

    SwitchShardBehavior(final ShardIdentifier shardId, final RaftState newState, final long term) {
        this.newState = requireNonNull(newState);
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
        return "SwitchShardBehavior{"
                + "shardId='" + shardId + '\''
                + ", newState='" + newState + '\''
                + ", term=" + term
                + '}';
    }
}
