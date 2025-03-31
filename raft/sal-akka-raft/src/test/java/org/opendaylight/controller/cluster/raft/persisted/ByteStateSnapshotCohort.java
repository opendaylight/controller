/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.IOException;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.raft.spi.InputStreamProvider;

/**
 * {@link RaftActorSnapshotCohort} for {@link ByteState}.
 */
public interface ByteStateSnapshotCohort extends RaftActorSnapshotCohort<ByteState> {
    @Override
    default Class<ByteState> stateClass() {
        return ByteState.class;
    }

    @Override
    default ByteState deserializeSnapshot(final InputStreamProvider snapshotBytes) throws IOException {
        return ByteState.of(snapshotBytes.openStream().readAllBytes());
    }
}
