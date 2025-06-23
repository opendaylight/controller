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
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single attempt at recovering {@link EntryJournal} state, replaying into a {@link PekkoReplicatedLog}.
 *
 * @param <T> {@link State} type
 */
@NonNullByDefault
final class JournalRecovery<T extends State> extends Recovery<T> {
    private static final Logger LOG = LoggerFactory.getLogger(JournalRecovery.class);

    private final EntryJournal journal;

    JournalRecovery(final RaftActor actor, final RaftActorSnapshotCohort<T> snapshotCohort,
            final RaftActorRecoveryCohort recoveryCohort, final ConfigParams configParams, final EntryJournal journal) {
        super(actor, snapshotCohort, recoveryCohort, configParams);
        this.journal = requireNonNull(journal);
    }

    RecoveryLog recoverJournal(final RecoveryLog pekkoLog) throws IOException {
        startRecoveryTimers();

        // First up: reconcile recoveryLog state w.r.t. recoveryCohort. We must always prove continuity of
        //           EntryJournal's contents, perhaps trimming it to match.
        recoveryLog.setSnapshotIndex(pekkoLog.getSnapshotIndex());
        recoveryLog.setSnapshotTerm(pekkoLog.getSnapshotTerm());
        recoveryLog.setCommitIndex(pekkoLog.getCommitIndex());
        recoveryLog.setLastApplied(pekkoLog.getLastApplied());

        // Next up::reconcile the contents of pekkoLog with journal
        final var journalIndex = reconcileAndRecover(pekkoLog);

        final var recoveryTime = stopRecoveryTimers();
        LOG.debug("{}: journal recovery completed{} with journalIndex={}", memberId(), recoveryTime, journalIndex);
        return recoveryLog;
    }

    private long reconcileAndRecover(final RecoveryLog pekkoLog) throws IOException {
        try (var reader = journal.openReader()) {
            // If pekkoLog contains any entries, it has come from Pekko persistence and we need to do some more work to
            // ensure migrate those entries into the EntryJournal. This can occur during multiple recoveries, as we may
            // get interrupted while populating the journal or while we were taking the snapshot.
            //
            // What we need to prove here is that all journal-recovered entries leading match unapplied entries.
            final var pekkoSize = pekkoLog.size();

            // If the journal has no entries, hence it should have no other metadata as well. Ditch the reader and
            // initialize the journal to the entries contained in prevPekkoLog. We also defensively initialize
            // applyToJournalIndex to 0.
            final var firstIndex = reader.nextJournalIndex();

            var journalIndex = firstIndex;
            var journalEntry = reader.nextEntry();
            if (journalIndex == 1 && journalEntry == null) {
                reader.close();
                journal.setApplyToJournalIndex(0);
                for (long i = 0; i < pekkoSize; ++i) {
                    journal.appendEntry(pekkoLog.entryAt(i));
                }
                return 1;
            }

            // Iterate over both pekkoLog and reader to ensure any entries match.
            for (long i = 0; i < pekkoSize; ++i) {
                final var pekkoEntry = pekkoLog.entryAt(i);
                if (journalEntry != null) {
                    // present in both: check index/term equality, but do not compare commands, e.g. trust them same
                    // serialization to save CPU/memory of deserializing a potentially large object.
                    if (journalEntry.index() != pekkoEntry.index() || journalEntry.term() != pekkoEntry.term()) {
                        LOG.info("{}: Mismatch between journal {} and {}, trimming journal", memberId(), journalEntry,
                            pekkoEntry);
                        journal.discardTail(journalIndex);
                        journal.appendEntry(pekkoEntry);
                        reader.resetToRead(journalIndex + 1);
                    }
                } else {
                    // not in the journal: append
                    journal.appendEntry(pekkoEntry);
                }

                recoverEntry(pekkoEntry);
                journalIndex = reader.nextJournalIndex();
                journalEntry = reader.nextEntry();
            }

            // Journal's idea on how far we can go in applying entries.
            // FIXME: check against commitIndex/lastApplied.
            final var applyToIndex = journal.applyToJournalIndex();

            // Process everything in the journal, being mindful of snapshot intervals. We should not be touching
            // pekkoLog past this point.
            while (journalEntry != null) {
                final var logEntry = journalEntry.toLogEntry();
                LOG.debug("{}: recovered journal {}", memberId(), logEntry);
                recoverEntry(logEntry);

                // FIXME: apply to state,  check if we should snapshot, etc.
                //                if (journalIndex <= applyToIndex) {
                //
                //                }

                journalIndex = reader.nextJournalIndex();
                journalEntry = reader.nextEntry();
            }

            // FIXME:
            return journalIndex;
        }
    }

    private void recoverEntry(final LogEntry entry) throws IOException {
        if (!recoveryLog.append(entry)) {
            throw new IOException("Failed to append entry " + entry);
        }
    }
}
