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
import java.util.List;
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

    RecoveryResult recoverJournal(final RecoveryResult pekkoResult) throws IOException {
        final var pekkoLog = pekkoResult.recoveryLog();

        startRecoveryTimers();

        // First up: reconcile recoveryLog state w.r.t. recoveryCohort. We must always prove continuity of
        //           EntryJournal's contents, perhaps trimming it to match.
        recoveryLog.setSnapshotIndex(pekkoLog.getSnapshotIndex());
        recoveryLog.setSnapshotTerm(pekkoLog.getSnapshotTerm());
        recoveryLog.setCommitIndex(pekkoLog.getCommitIndex());
        recoveryLog.setLastApplied(pekkoLog.getLastApplied());

        // Next up: reconcile the contents of pekkoLog with journal
        reconcileAndRecover(pekkoLog);

        // Finally: flush everything we recovered
        applyRecoveredCommands();

        final var recoveryTime = stopRecoveryTimers();
        LOG.debug("{}: journal recovery completed{} with journalIndex={}", memberId(), recoveryTime,
            recoveryLog.firstJournalIndex());

        return new RecoveryResult(recoveryLog, pekkoResult.canRestoreFromSnapshot() && !dataRecovered());
    }

    private void reconcileAndRecover(final RecoveryLog pekkoLog) throws IOException {
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
            recoveryLog.setFirstJournalIndex(firstIndex);

            var journalIndex = firstIndex;
            var journalEntry = reader.nextEntry();
            if (journalIndex == 1 && journalEntry == null) {
                LOG.debug("{}: empty journal: appending {} entries", memberId(), pekkoSize);
                reader.close();
                journal.setApplyTo(0);
                for (long i = 0; i < pekkoSize; ++i) {
                    final var entry = pekkoLog.entryAt(i);
                    writeEntry(entry);
                    recoverEntry(entry);
                }
                return;
            }

            // Iterate over both pekkoLog and reader to ensure any entries match.
            for (long i = 0; i < pekkoSize; ++i) {
                final var pekkoEntry = pekkoLog.entryAt(i);
                if (journalEntry != null) {
                    // present in both: check index/term equality, but do not compare commands, e.g. trust them same
                    // serialization to save CPU/memory of deserializing a potentially large object.
                    if (journalEntry.index() != pekkoEntry.index() || journalEntry.term() != pekkoEntry.term()) {
                        LOG.info("{}: mismatch between journal {} and {}, trimming journal", memberId(), journalEntry,
                            pekkoEntry);
                        journal.discardTail(journalIndex);
                        writeEntry(pekkoEntry);
                        reader.resetToRead(journalIndex + 1);
                    }
                } else {
                    // not in the journal: write and possibly adjust applyTo index
                    LOG.debug("{}: writing entry {} to {}", memberId(), pekkoEntry, journalIndex);
                    writeEntry(pekkoEntry);
                    if (recoveryLog.getLastApplied() >= pekkoEntry.index()) {
                        journal.setApplyTo(journalIndex);
                    }
                }

                recoverEntry(pekkoEntry);
                journalIndex = reader.nextJournalIndex();
                journalEntry = reader.nextEntry();
            }

            // Journal's idea on how far we can go in applying entries.
            var applyToIndex = journal.applyToJournalIndex();
            LOG.debug("{}: applying entries up to {}", memberId(), applyToIndex);

            // Process everything in the journal, being mindful of snapshot intervals. We should not be touching
            // pekkoLog past this point.
            var lastApplied = recoveryLog.getLastApplied();
            while (journalEntry != null) {
                final var logEntry = journalEntry.toLogEntry(actor.objectStreams());

                if (recoveryLog.isInSnapshot(logEntry.index())) {
                    LOG.debug("{}: entry {} implied by snapshot, adjusting replayFrom/applyTo to at least {}",
                        memberId(), logEntry, journalIndex);
                    journal.discardHead(journalIndex + 1);
                    if (applyToIndex < journalIndex) {
                        journal.setApplyTo(journalIndex);
                        applyToIndex = journalIndex;
                    }
                } else {
                    LOG.debug("{}: recovered journal {}", memberId(), logEntry);
                    recoverEntry(logEntry);
                    if (isMigratedPayload(logEntry)) {
                        setMigratedDataRecovered();
                    }

                    if (journalIndex <= applyToIndex) {
                        applyEntry(logEntry);
                        lastApplied = logEntry.index();
                    }
                }

                journalIndex = reader.nextJournalIndex();
                journalEntry = reader.nextEntry();
            }

            // We should have processed entries at least to applyToIndex
            if (journalIndex <= applyToIndex) {
                throw new IOException("Incomplete journal: expected to apply entries up to " + applyToIndex
                    + ", encountered entries only to " + (journalIndex - 1));
            }

            // Guard against time travel w.r.t. Pekko-recovered state
            final var pekkoCommitIndex = pekkoLog.getCommitIndex();
            if (pekkoCommitIndex > lastApplied) {
                throw new IOException("Cannot move commitIndex from " + pekkoCommitIndex + " to " + lastApplied);
            }
            final var pekkoLastApplied = pekkoLog.getLastApplied();
            if (pekkoLastApplied > lastApplied) {
                throw new IOException("Cannot move lastApplied from " + pekkoLastApplied + " to " + lastApplied);
            }

            // Recover commitIndex is implied to be at least, we will discover the actual value as part of RAFT join
            recoveryLog.setCommitIndex(lastApplied);
            recoveryLog.setLastApplied(lastApplied);
        }
    }

    private void recoverEntry(final LogEntry entry) throws IOException {
        if (!recoveryLog.append(entry)) {
            throw new IOException("Failed to append entry " + entry);
        }
        setDataRecovered();
        actor.recoveryObserver().onCommandRecovered(entry.command());
    }

    private void writeEntry(final LogEntry entry) throws IOException {
        final var bodySize = journal.appendEntry(entry);
        LOG.trace("{}: journal entry body size {}", memberId(), bodySize);
    }

    @Override
    List<LogEntry> filterSnapshotUnappliedEntries(final List<LogEntry> unappliedEntries) {
        return List.of();
    }

    @Override
    void discardSnapshottedEntries() throws IOException {
        journal.discardHead(recoveryLog.firstJournalIndex());
    }
}
