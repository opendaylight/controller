/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.raft.api.EntryInfo;

/**
 * An attempt to recover to non-persistent storage.
 *
 * @param <T> {@link State} type
 */
@NonNullByDefault
final class TransientRecovery<T extends State> extends Recovery<T> {
    TransientRecovery(final RaftActor actor, final RaftActorSnapshotCohort<T> snapshotCohort,
            final RaftActorRecoveryCohort recoveryCohort, final ConfigParams configParams) {
        super(actor, snapshotCohort, recoveryCohort, configParams);
    }

    @Override
    void doRecover(final EntryInfo lastIncluded, final @Nullable State state, final List<LogEntry> entries) {
        // This should be boil down to a no-op, but let's be explicit
        initializeState(EntryInfo.of(-1, -1), null);
    }

    @Override
    void discardSnapshottedEntries() {
        // No-op
    }

    @Override
    void saveRecoverySnapshot() {
        saveEmptySnapshot();
    }

    @Override
    void saveFinalSnapshot() {
        saveEmptySnapshot();
    }
}
