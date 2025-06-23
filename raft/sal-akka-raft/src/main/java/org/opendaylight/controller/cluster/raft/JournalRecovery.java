/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
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
    private final EntryJournal journal;

    JournalRecovery(final RaftActor actor, final RaftActorSnapshotCohort<T> snapshotCohort,
            final RaftActorRecoveryCohort recoveryCohort, final ConfigParams configParams, final EntryJournal journal) {
        super(actor, snapshotCohort, recoveryCohort, configParams);
        this.journal = requireNonNull(journal);
    }

    PekkoReplicatedLog recoverJournal(final PekkoReplicatedLog prevPekkoLog) throws IOException {
        // First up: reconcile the contents of prevPekkoLog with journal
        final var journalIndex = reconcileJournal(prevPekkoLog);




        //
        // If pekkoLog contains any entries, it has come from Pekko persistence and we need to do some more work to
        // ensure migrate those entries into the EntryJournal. This can occur during multiple recoveries, as we may get
        // interrupted while populating the journal or while we were taking the snapshot.

        return pekkoLog;
    }

    /**
     * Reconcile the contents of {@code prevPekkoLog} and {@code journal}.
     *
     * @param prevPekkoLog the {@link PekkoReplicatedLog} to reconcile with
     * @return the {@code journalIndex} of the first entry in the journal
     * @throws IOException if an I/O error occurs
     */
    private long reconcileJournal(final PekkoReplicatedLog prevPekkoLog) throws IOException {
        try (var reader = journal.openReader()) {
            final var firstIndex = reader.nextJournalIndex();
            final var firstEntry = reader.nextEntry();
            if (firstIndex == 1 && firstEntry == null) {
                // The journal has no entries, hence it should have no other metadata as well. Ditch the reader and
                // initialize the journal to the entries contained in prevPekkoLog. We also defensively initialize
                // applyToJournalIndex to 0.
                reader.close();
                journal.setApplyToJournalIndex(0);
                for (long i = 0, size = prevPekkoLog.size(); i < size; ++i) {
                    journal.appendEntry(prevPekkoLog.entryAt(i));
                }
                return 1;
            }





            // hard case: we need to reconcile any unapplied entries, snapshot->journal continuity and commitIndex

            // FIXME: finish this

            return firstIndex;
        }
    }
}
