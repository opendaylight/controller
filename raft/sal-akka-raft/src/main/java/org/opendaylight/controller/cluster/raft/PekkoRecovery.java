/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import org.apache.pekko.persistence.RecoveryCompleted;
import org.apache.pekko.persistence.SnapshotOffer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFile;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.TermInfoStore;
import org.opendaylight.raft.api.TermInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single attempt at recovering Pekko state. Essentially replays Pekko persistence into backing {@link SnapshotStore},
 * {@link TermInfoStore} and a {@link ReplicatedLog}.
 *
 * @param <T> {@link State} type
 */
// non-sealed for testing
non-sealed class PekkoRecovery<T extends @NonNull State> extends Recovery<T> {
    static final class ToTransient<T extends @NonNull State> extends PekkoRecovery<T> {
        private boolean dataRecoveredWithPersistenceDisabled;

        @NonNullByDefault
        ToTransient(final RaftActor actor, final RaftActorSnapshotCohort<T> snapshotCohort,
                final RaftActorRecoveryCohort recoveryCohort, final ConfigParams configParams) throws IOException {
            super(actor, snapshotCohort, recoveryCohort, configParams);
        }

        @Override
        @Deprecated(since = "11.0.0", forRemoval = true)
        void onDeleteEntries(final DeleteEntries deleteEntries) {
            dataRecoveredWithPersistenceDisabled = true;
        }

        @Override
        void onRecoveredApplyLogEntries(final long toIndex) {
            dataRecoveredWithPersistenceDisabled = true;
        }

        @Override
        void appendRecoveredEntry(final ReplicatedLogEntry logEntry) {
            if (!(logEntry.command() instanceof VotingConfig)) {
                dataRecoveredWithPersistenceDisabled = true;
            }
        }

        @Override
        Snapshot processRecoveredSnapshot(final Snapshot snapshot) {
            // We may have just transitioned to disabled and have a snapshot containing state data and/or log entries -
            // we don't want to preserve these, only the server config and election term info.
            return Snapshot.create(null, List.of(), -1, -1, -1, -1, snapshot.termInfo(), snapshot.votingConfig());
        }

        @Override
        boolean completeRecovery() {
            if (super.completeRecovery()) {
                if (!dataRecoveredWithPersistenceDisabled) {
                    return true;
                }
                LOG.info("{}: Saving snapshot after recovery due to data persistence disabled", memberId());
                saveEmptySnapshot();
            }
            return false;
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

    private static final Logger LOG = LoggerFactory.getLogger(PekkoRecovery.class);

    private final @Nullable TermInfo origTermInfo;
    private final @Nullable SnapshotFile origSnapshot;

    private long lastDeletedSequenceNr;

    @NonNullByDefault
    PekkoRecovery(final RaftActor actor, final RaftActorSnapshotCohort<T> snapshotCohort,
            final RaftActorRecoveryCohort recoveryCohort, final ConfigParams configParams) throws IOException {
        super(actor, snapshotCohort, recoveryCohort, configParams);

        origTermInfo = termInfoStore().loadAndSetTerm();
        if (origTermInfo != null) {
            setDataRecovered();
        }

        final var loaded = snapshotStore().lastSnapshot();
        if (loaded != null) {
            initializeLog(loaded.timestamp(),
                Snapshot.ofRaft(origTermInfo, loaded.readRaftSnapshot(actor.objectStreams()),
                loaded.lastIncluded(), loaded.readSnapshot(snapshotCohort.support().reader())));
        }
        origSnapshot = loaded;
    }

    final @Nullable RecoveryResult handleRecoveryMessage(final Object message) {
        LOG.trace("{}: handleRecoveryMessage: {}", memberId(), message);

        if (isMigratedSerializable(message)) {
            setMigratedDataRecovered();
        }

        switch (message) {
            case SnapshotOffer msg -> onRecoveredSnapshot(msg);
            case ReplicatedLogEntry msg -> onRecoveredJournalLogEntry(msg);
            case ApplyJournalEntries msg -> onRecoveredApplyLogEntries(msg.getToIndex());
            case DeleteEntries msg -> onDeleteEntries(msg);
            case VotingConfig msg -> actor.peerInfos().updateVotingConfig(msg);
            case UpdateElectionTerm(var termInfo) -> termInfoStore().setTerm(termInfo);
            case RecoveryCompleted msg -> {
                final var canRecoverFromSnapshot = onRecoveryCompletedMessage();
                final var lastSeq = actor.lastSequenceNr();
                if (lastSeq > lastDeletedSequenceNr) {
                    LOG.info("{}: taking snapshot to clear Pekko persistence to {}", memberId(), lastSeq);
                    saveFinalSnapshot();
                }

                return new RecoveryResult(recoveryLog, canRecoverFromSnapshot && !dataRecovered());
            }
            default -> LOG.debug("{}: ignoring unhandled message {}", memberId(), message);
        }

        setDataRecovered();
        return null;
    }

    @NonNullByDefault
    private TermInfoStore termInfoStore() {
        return actor.localAccess().termInfoStore();
    }

    private void onRecoveredSnapshot(final SnapshotOffer offer) {
        LOG.debug("{}: SnapshotOffer called.", memberId());
        final var snapshot = (Snapshot) offer.snapshot();
        final var timestamp = Instant.ofEpochMilli(offer.metadata().timestamp());
        final var local = origSnapshot;
        if (local != null) {
            LOG.warn("{}: ignoring Pekko snapshot from {} containing {} up to {} in favor of {} contained in {}",
                memberId(), timestamp, snapshot.lastApplied(), snapshot.last(), local.lastIncluded(), local.source());
            return;
        }

        initializeLog(timestamp, snapshot);

        LOG.info("{}: migrating from Pekko persistent-snapshot taken at {}", memberId(), timestamp);
        final var sw = Stopwatch.createStarted();
        try {
            snapshotStore().saveSnapshot(new RaftSnapshot(snapshot.votingConfig(), snapshot.getUnAppliedEntries()),
                snapshot.lastApplied(), RaftActor.toStorage(support(), snapshot.state()), timestamp);
        } catch (IOException e) {
            LOG.error("{}: failed to save local snapshot", memberId(), e);
            throw new UncheckedIOException(e);
        }

        LOG.info("{}: local snapshot saved in {}, deleting Pekko-persisted snapshots", memberId(), sw.stop());
        actor.nukePekkoSnapshots();
    }

    private void initializeLog(final Instant timestamp, final Snapshot recovered) {
        LOG.debug("{}: initializing from snapshot taken at {}", memberId(), timestamp);
        startRecoveryTimers();
        setDataRecovered();

        for (var entry : recovered.getUnAppliedEntries()) {
            if (isMigratedPayload(entry)) {
                setMigratedDataRecovered();
            }
        }

        final var toApply = processRecoveredSnapshot(recovered);

        // Create a replicated log with the snapshot information
        // The replicated log can be used later on to retrieve this snapshot
        // when we need to install it on a peer
        recoveryLog.resetToSnapshot(toApply);
        termInfoStore().setTerm(toApply.termInfo());

        final var votingConfig = toApply.votingConfig();
        if (votingConfig != null) {
            actor.peerInfos().updateVotingConfig(votingConfig);
        }

        final var timer = Stopwatch.createStarted();

        // Apply the snapshot to the actors state
        final var snapshotState = toApply.state();
        if (snapshotState != null) {
            if (snapshotState.needsMigration()) {
                setMigratedDataRecovered();
            }
            recoveryCohort.applyRecoveredSnapshot(snapshotState);
        }

        LOG.info("Recovery snapshot applied for {} in {}: snapshotIndex={}, snapshotTerm={}, journal-size={}",
            memberId(), timer.stop(), recoveryLog.getSnapshotIndex(), recoveryLog.getSnapshotTerm(),
            recoveryLog.size());
    }

    @NonNullByDefault
    Snapshot processRecoveredSnapshot(final Snapshot recovered) {
        return recovered;
    }

    private void onRecoveredJournalLogEntry(final ReplicatedLogEntry logEntry) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{}: Received ReplicatedLogEntry for recovery: index: {}, size: {}", memberId(),
                logEntry.index(), logEntry.size());
        }

        final var command = logEntry.command();
        if (isMigratedSerializable(command)) {
            setMigratedDataRecovered();
        }

        if (command instanceof VotingConfig newVotingConfig) {
            actor.peerInfos().updateVotingConfig(newVotingConfig);
        }

        appendRecoveredEntry(logEntry);
    }

    @NonNullByDefault
    void appendRecoveredEntry(final ReplicatedLogEntry logEntry) {
        recoveryLog.append(logEntry);
    }

    void onRecoveredApplyLogEntries(final long toIndex) {
        long lastApplied = recoveryLog.getLastApplied();
        long lastUnapplied = lastApplied + 1;

        // it can happen that lastUnappliedIndex > toIndex, if the AJE is in the persistent journal but the entry itself
        // has made it to that state and recovered via the snapshot
        LOG.debug("{}: Received apply journal entries for recovery, applying to state: {} to {}", memberId(),
            lastUnapplied, toIndex);

        while (lastUnapplied <= toIndex) {
            final var logEntry = recoveryLog.lookup(lastUnapplied);
            if (logEntry == null) {
                // Shouldn't happen but cover it anyway.
                LOG.error("{}: Log entry not found for index {}", memberId(), lastUnapplied);
                break;
            }

            lastApplied++;
            startRecoveryTimers();
            applyEntry(logEntry);
            lastUnapplied++;
        }

        recoveryLog.setLastApplied(lastApplied);
        recoveryLog.setCommitIndex(lastApplied);
    }

    @Deprecated(since = "11.0.0", forRemoval = true)
    void onDeleteEntries(final DeleteEntries deleteEntries) {
        recoveryLog.removeRecoveredEntries(deleteEntries.getFromIndex());
    }

    @Override
    final List<LogEntry> filterSnapshotUnappliedEntries(List<LogEntry> unappliedEntries) {
        return unappliedEntries;
    }

    @Override
    final void discardSnapshottedEntries() {
        lastDeletedSequenceNr = actor.lastSequenceNr();
        actor.deleteMessages(lastDeletedSequenceNr);
    }

    private boolean onRecoveryCompletedMessage() {
        applyRecoveredCommands();

        final var recoveryTime = stopRecoveryTimers();

        // FIXME: not 'switching', move that log somewhere else
        LOG.info("{}: Recovery completed {} - Switching actor to Follower - last log index = {}, last log term = {}, "
            + "snapshot index = {}, snapshot term = {}, journal size = {}", memberId(), recoveryTime,
            recoveryLog.lastIndex(), recoveryLog.lastTerm(), recoveryLog.getSnapshotIndex(),
            recoveryLog.getSnapshotTerm(), recoveryLog.size());


        // Populate property-based storage if needed, or roll-back any voting information leaked by recovery process
        final var infoStore = termInfoStore();
        final var orig = origTermInfo;
        if (orig == null) {
            // No original info observed, seed it after recovery and trigger a snapshot
            final var current = infoStore.currentTerm();
            try {
                infoStore.storeAndSetTerm(current);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            // From this point on we will not update TermInfo from Akka persistence
            LOG.info("{}: Local TermInfo store seeded with {}", memberId(), current);
        } else {
            // Undo whatever recovery has done to what we have observed
            LOG.debug("{}: restoring local {}", memberId(), orig);
            infoStore.setTerm(orig);
        }

        return completeRecovery();
    }

    boolean completeRecovery() {
        if (migratedDataRecovered()) {
            LOG.info("{}: Snapshot capture initiated after recovery due to migrated messages", memberId());
            saveFinalSnapshot();
            return false;
        }
        return true;
    }

    // Either data persistence is disabled and we recovered some data entries (i.e. we must have just transitioned
    // to disabled or a persistence backup was restored) or we recovered migrated messages. Either way, we persist
    // a snapshot and delete all the messages from the Pekko journal to clean out unwanted messages.
    final void saveEmptySnapshot() {
        try {
            actor.saveEmptySnapshot();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        lastDeletedSequenceNr = actor.lastSequenceNr();
        actor.deleteMessages(lastDeletedSequenceNr);
    }
}
