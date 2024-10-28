/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.api.RaftActorAccess;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.opendaylight.controller.cluster.raft.state.CandidateState;
import org.opendaylight.controller.cluster.raft.state.InactiveState;
import org.opendaylight.controller.cluster.raft.state.StartingState;

@NonNullByDefault
final class StartingShard extends ActiveShard implements StartingState {
    StartingShard(final InactiveShard stopped, final RaftActorAccess actorAccess,
            final DefaultShardStatsMXBean shardStatsBean) {
        super(stopped, actorAccess, shardStatsBean);
    }

    @Override
    public CandidateState toCandidate() {
        // FIXME: need data tree
        return new CandidateShard(this, null);
    }

    @Override
    public InactiveState toInactive() {
        return stop();
    }

    @Override
    public void startLogRecoveryBatch(final int maxBatchSize) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendRecoveredLogEntry(final Payload data) {
        // TODO Auto-generated method stub

    }

    @Override
    public void applyRecoverySnapshot(final State snapshotState) {
        // TODO Auto-generated method stub

    }

    @Override
    public void applyCurrentLogRecoveryBatch() {
        // TODO Auto-generated method stub

    }

    @Override
    public @Nullable Snapshot getRestoreFromSnapshot() {
        // TODO Auto-generated method stub
        return null;
    }
}
