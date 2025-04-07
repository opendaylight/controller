/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.Support;

/**
 * {@link RaftActorSnapshotCohort} for {@link ByteState}.
 */
@NonNullByDefault
public interface ByteStateSnapshotCohort extends RaftActorSnapshotCohort<ByteState> {
    @Override
    default Support<ByteState> support() {
        return ByteState.SUPPORT;
    }

    @Override
    default void applySnapshot(final ByteState snapshotState) {
        // No-op
    }
}
