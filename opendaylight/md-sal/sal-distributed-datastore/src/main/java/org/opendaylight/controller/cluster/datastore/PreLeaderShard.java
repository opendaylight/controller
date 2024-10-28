/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.state.CandidateState;
import org.opendaylight.controller.cluster.raft.state.FollowerState;
import org.opendaylight.controller.cluster.raft.state.IsolatedLeaderState;
import org.opendaylight.controller.cluster.raft.state.LeaderState;
import org.opendaylight.controller.cluster.raft.state.PreLeaderState;
import org.opendaylight.controller.cluster.raft.state.InactiveState;

@NonNullByDefault
final class PreLeaderShard extends ElectedShard implements PreLeaderState {
    PreLeaderShard(final CandidateShard candidate) {
        super(candidate);
    }

    @Override
    public CandidateState toCandidate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FollowerState toFollower() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IsolatedLeaderState toIsolatedLeader() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LeaderState toLeader() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InactiveState toInactive() {
        // TODO Auto-generated method stub
        return null;
    }
}
