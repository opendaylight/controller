/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.journal.Journal;
import org.opendaylight.controller.cluster.raft.state.CandidateState;
import org.opendaylight.controller.cluster.raft.state.FollowerState;
import org.opendaylight.controller.cluster.raft.state.InactiveState;
import org.opendaylight.controller.cluster.raft.state.PreLeaderState;

@NonNullByDefault
final class CandidateShard extends StartedShard implements CandidateState {
    CandidateShard(final StartingShard starting, final ShardDataTree dataTree) {
        super(starting, dataTree);
    }

    CandidateShard(final StartedShard prev) {
        super(prev);
    }

    @Override
    public InactiveState toInactive() {
        return stop();
    }

    @Override
    public FollowerState toFollower() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PreLeaderState toPreLeader(final Journal journal) {
        // TODO Auto-generated method stub
        return null;
    }
}
