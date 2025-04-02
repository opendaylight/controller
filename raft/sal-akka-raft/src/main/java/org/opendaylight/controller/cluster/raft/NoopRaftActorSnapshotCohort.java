/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.io.OutputStream;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.raft.spi.InputStreamProvider;

/**
 * RaftActorSnapshotCohort implementation that does nothing.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
public final class NoopRaftActorSnapshotCohort implements RaftActorSnapshotCohort<EmptyState> {
    public static final NoopRaftActorSnapshotCohort INSTANCE = new NoopRaftActorSnapshotCohort();

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
    public void writeSnapshot(final EmptyState snapshot, final OutputStream out) {
        // No-op
    }

    @Override
    public EmptyState readSnapshot(final InputStreamProvider source) {
        // XXX: we really should be reading the bytes
        return takeSnapshot();
    }
}
