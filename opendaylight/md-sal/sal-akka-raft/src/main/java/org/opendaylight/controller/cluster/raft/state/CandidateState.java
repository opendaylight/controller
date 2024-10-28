/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.journal.Journal;

/**
 * The state of a RAFT candidate.
 */
@NonNullByDefault
public non-sealed interface CandidateState extends RaftStateBehavior {
    @Override
    default RaftState raftState() {
        return RaftState.Candidate;
    }

    FollowerState toFollower();

    PreLeaderState toPreLeader(Journal journal);
}
