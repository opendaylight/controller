/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;

/**
 * Behavior of RAFT-managed state. These mirror {@link RaftActor} and {@link RaftActorBehavior} lifecycle, specifically:
 * <ul>
 *   <li>{@link InactiveState}, which is where we start from</a>
 *   <li>{@link StartingState} covers initial {@link RaftActor} startup and tasks leading to state being
 *       initialized/recovered</li>
 *   <li>one of {@link RaftStateBehavior}s, each corresponding to a {@link RaftState}.
 * </ul>
 *
 * <p>Methods defined in this interface hierarchy must not be invoked outside of {@link RaftActor} confinement.
 */
@NonNullByDefault
public sealed interface StateBehavior permits ActiveState, InactiveState {

    // TODO: async?
    InactiveState toInactive();
}
