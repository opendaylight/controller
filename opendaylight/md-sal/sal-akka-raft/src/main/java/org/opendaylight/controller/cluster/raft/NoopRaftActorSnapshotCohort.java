/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.ObjectInputStream;
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
    public State createSnapshot() {
        return EmptyState.INSTANCE;
    }

    @Override
    public void applySnapshot(final State snapshotState) {
        // No-op
    }

    @Override
    public State deserializeSnapshot(final ByteSource snapshotBytes) throws IOException {
        try (var ois = new ObjectInputStream(snapshotBytes.openStream())) {
            return (State) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to read state", e);
        }
    }
}
