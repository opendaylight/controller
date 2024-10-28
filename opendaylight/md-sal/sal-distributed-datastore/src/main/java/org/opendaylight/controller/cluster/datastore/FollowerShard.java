/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.channels.ScatteringByteChannel;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.datastore.messages.MakeLeaderLocal;
import org.opendaylight.controller.cluster.raft.messages.RequestLeadership;
import org.opendaylight.controller.cluster.raft.state.CandidateState;
import org.opendaylight.controller.cluster.raft.state.FollowerState;
import org.opendaylight.controller.cluster.raft.state.FollowerStateException;
import org.opendaylight.controller.cluster.raft.state.InactiveState;

@NonNullByDefault
final class FollowerShard extends StartedShard implements FollowerState {
    private final ActorSelection leaderActor;

    FollowerShard(final StartedShard prev, final ActorSelection leaderActor) {
        super(prev);
        this.leaderActor = requireNonNull(leaderActor);
    }

    @Override
    public InactiveState toInactive() {
        return stop();
    }

    @Override
    public CandidateState toCandidate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    void handleMakeLeaderLocal(final MakeLeaderLocal message, final ActorRef sender) {
        leaderActor.tell(new RequestLeadership(shardId, sender), self());
    }

    @Override
    public void applyEntry(final ScatteringByteChannel in) throws IOException, FollowerStateException {
        // TODO Auto-generated method stub
    }
}
