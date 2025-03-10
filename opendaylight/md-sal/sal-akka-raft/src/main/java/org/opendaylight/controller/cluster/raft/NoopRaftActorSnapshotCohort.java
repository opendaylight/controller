/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.common.io.ByteSource;
import java.io.OutputStream;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;

/**
 * RaftActorSnapshotCohort implementation that does nothing.
 *
 * @author Thomas Pantelis
 */
public final class NoopRaftActorSnapshotCohort implements RaftActorSnapshotCohort {
    public static final @NonNull NoopRaftActorSnapshotCohort INSTANCE = new NoopRaftActorSnapshotCohort();

    private NoopRaftActorSnapshotCohort() {
        // Hidden on purpose
    }

    @Override
    public void createSnapshot(final ActorRef actorRef, final OutputStream installSnapshotStream) {
        // No-op
    }

    @Override
    public void applySnapshot(final State snapshotState) {
        // No-op
    }

    @Override
    public State deserializeSnapshot(final ByteSource snapshotBytes) {
        return EmptyState.INSTANCE;
    }
}
