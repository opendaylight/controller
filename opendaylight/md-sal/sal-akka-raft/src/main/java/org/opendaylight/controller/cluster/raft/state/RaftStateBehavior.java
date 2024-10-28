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

/**
 * Common interface for {@link StateBehavior}s corresponding to a {@link #raftState()}.
 */
@NonNullByDefault
public sealed interface RaftStateBehavior extends ActiveState
        permits CandidateState, FollowerState, ElectedStateBehavior {
    /**
     * Returns {@link RaftState} corresponding to this behaviour.
     *
     * @return {@link RaftState} corresponding to this behaviour
     */
    RaftState raftState();
    /**
     * Returns the payload version of this state.
     *
     * @return the payload version
     */
    short payloadVersion();
    /**
     * Take a snapshot of current state.
     *
     * @return a {@link StateSnapshot}
     */
    StateSnapshot takeSnapshot();
}
