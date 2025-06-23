/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.opendaylight.controller.cluster.raft.spi.EntryJournal;

/**
 * A single attempt at recovering {@link EntryJournal} state, replaying into a {@link PekkoReplicatedLog}.
 *
 * @param <T> {@link State} type
 */
@NonNullByDefault
final class JournalRecovery<T extends State> extends AbstractRecovery<T> {
    private final PekkoReplicatedLog pekkoLog;
    private final EntryJournal journal;

    JournalRecovery(final RaftActor actor, final RaftActorSnapshotCohort<T> snapshotCohort,
            final RaftActorRecoveryCohort recoveryCohort, final ConfigParams configParams,
            final PekkoReplicatedLog pekkoLog, final EntryJournal journal) {
        super(actor, snapshotCohort, recoveryCohort, configParams);
        this.pekkoLog = requireNonNull(pekkoLog);
        this.journal = requireNonNull(journal);
    }

    PekkoReplicatedLog recoverJournal() {
        // First up: reconcile the contents of pekkoLog with journal w.r.t. contained entries.
        //
        // If pekkoLog contains any entries, it has come from Pekko persistence and we need to do some more work to
        // ensure migrate those entries into the EntryJournal. This can occur during multiple recoveries, as we may get
        // interrupted while populating the journal or while we were taking the snapshot.
        if (pekkoLog.size() > 0) {



        }





        return pekkoLog;
    }
}
