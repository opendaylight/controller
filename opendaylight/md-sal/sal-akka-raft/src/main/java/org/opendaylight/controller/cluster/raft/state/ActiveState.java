/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import org.opendaylight.controller.cluster.raft.RaftActor;

/**
 * An active {@link StateBehavior}. It is plugged into {@link RaftActor} message handling via
 * {@link #handleRaftActorMessage(Object)}.
 */
public sealed interface ActiveState extends StateBehavior, ActiveStateMethods permits RaftStateBehavior, StartingState {
    // Nothing else
}
