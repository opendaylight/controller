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
import java.io.OutputStream;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;

/**
 * RaftActorSnapshotCohort implementation that does nothing.
 *
 * @author Thomas Pantelis
 */
public final class NoopRaftActorSnapshotCohort implements RaftActorSnapshotCohort<EmptyState> {
    public static final @NonNull NoopRaftActorSnapshotCohort INSTANCE = new NoopRaftActorSnapshotCohort();

    private NoopRaftActorSnapshotCohort() {
        // Hidden on purpose
    }

    @Override
    public Class<EmptyState> stateClass() {
        return EmptyState.class;
    }

    @Override
    public EmptyState takeSnapshot() {
        return EmptyState.INSTANCE;
    }

    @Override
    public void applySnapshot(final EmptyState snapshotState) {
        // No-op
    }

    @Override
    public EmptyState deserializeSnapshot(final ByteSource snapshotBytes) {
        // FIXME: check whether snapshotBytes is empty
        return takeSnapshot();
    }

    @Override
    public void serializeSnapshot(final EmptyState snapshotState, final OutputStream out) throws IOException {
        // No-op
    }
}
