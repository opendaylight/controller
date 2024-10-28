/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.api.RaftActorAccess;

@NonNullByDefault
public final class TestInactive implements InactiveState {

    public static final InactiveState INSTANCE = new TestInactive();

    private TestInactive() {
        // Hidden on purpose
    }

    @Override
    public StartingState toStarting(final RaftActorAccess actorAccess) {
        return new TestStarting(actorAccess);
    }
}
