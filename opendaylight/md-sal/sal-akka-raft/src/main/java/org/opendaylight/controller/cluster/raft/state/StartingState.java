/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import com.google.common.annotations.Beta;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.RaftActorRecoveryCohort;
import org.opendaylight.controller.cluster.raft.behaviors.Candidate;

/**
 * Basic interface to {@link RaftActor} startup. This interface is provides basic bootstrap of recovery.
 */
// FIXME: better name? Initializing? Recovering? are there multiple stages to it?
@Beta
@NonNullByDefault
public non-sealed interface StartingState extends ActiveState, RaftActorRecoveryCohort {
    /**
     * Invoked when {@link RaftActor} has determined that the state initialization is complete and it is ready to
     * transition to {@link Candidate} behavior. Implementations need to
     *
     * @return the {@link CandidateState}
     */
    CandidateState toCandidate();
}
