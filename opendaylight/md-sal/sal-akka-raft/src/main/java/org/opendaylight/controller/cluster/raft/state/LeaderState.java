/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import org.opendaylight.controller.cluster.raft.RaftState;

/**
 * The state of a RAFT leader.
 */
public non-sealed interface LeaderState extends ElectedStateBehavior {
    @Override
    default RaftState raftState() {
        return RaftState.Leader;
    }

    @Override
    default LeaderState toLeader() {
        return this;
    }
}
