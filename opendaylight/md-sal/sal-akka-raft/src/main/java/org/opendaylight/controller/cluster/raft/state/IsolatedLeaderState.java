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
 * The state of a RAFT leader, which is observed to be isolated from its peers.
 */
@NonNullByDefault
public non-sealed interface IsolatedLeaderState extends ElectedStateBehavior {
    @Override
    default RaftState raftState() {
        return RaftState.IsolatedLeader;
    }

    @Override
    default IsolatedLeaderState toIsolatedLeader() {
        return this;
    }
}
