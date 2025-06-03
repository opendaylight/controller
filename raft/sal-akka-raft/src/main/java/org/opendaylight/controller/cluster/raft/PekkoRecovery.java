/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.persistence.RecoveryCompleted;
import org.apache.pekko.persistence.SnapshotOffer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.MigratedSerializable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFile;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.StateCommand;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.controller.cluster.raft.spi.TermInfoStore;
import org.opendaylight.raft.api.EntryMeta;
import org.opendaylight.raft.api.TermInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single attempt at recovering Pekko state. Essentially replays Pekko persistence into backing {@link SnapshotStore},
 * {@link TermInfoStore} and a {@link ReplicatedLog}.
 */
// non-sealed for testing
class PekkoRecovery<T extends @NonNull State> {
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
    }

    private static final Logger LOG = LoggerFactory.getLogger(PekkoRecovery.class);

    private final @NonNull RaftActor actor;
    private final @NonNull RaftActorRecoveryCohort recoveryCohort;
    private final @NonNull RaftActorSnapshotCohort<T> snapshotCohort;
    private final @NonNull PekkoReplicatedLog pekkoLog;
    private final @Nullable TermInfo origTermInfo;
    private final @Nullable SnapshotFile origSnapshot;
    private final int snapshotInterval;
    private final int batchSize;

    private int currentRecoveryBatchCount;
    private boolean anyDataRecovered;
    private boolean hasMigratedDataRecovered;
    private Stopwatch recoveryTimer;
    private Stopwatch snapshotTimer;

    @NonNullByDefault
    PekkoRecovery(final RaftActor actor, final RaftActorSnapshotCohort<T> snapshotCohort,
            final RaftActorRecoveryCohort recoveryCohort, final ConfigParams configParams)
                throws IOException {
        this.actor = requireNonNull(actor);
        this.snapshotCohort = requireNonNull(snapshotCohort);
        this.recoveryCohort = requireNonNull(recoveryCohort);

        final var localAccess = actor.localAccess();
        pekkoLog = new PekkoReplicatedLog(localAccess.memberId());
        origTermInfo = localAccess.termInfoStore().loadAndSetTerm();

        snapshotInterval = configParams.getRecoverySnapshotIntervalSeconds();
        batchSize = configParams.getJournalRecoveryLogBatchSize();

        final var loaded = snapshotStore().lastSnapshot();
        if (loaded != null) {
            initializeLog(loaded.timestamp(), Snapshot.ofRaft(origTermInfo, loaded.readRaftSnapshot(),
                loaded.lastIncluded(), loaded.readSnapshot(snapshotCohort.support().reader())));
        }
        origSnapshot = loaded;
    }

    @VisibleForTesting
    final @NonNull PekkoReplicatedLog pekkoLog() {
        return pekkoLog;
    }

    private @NonNull SnapshotStore snapshotStore() {
        return actor.persistence().snapshotStore();
    }

    final @Nullable PekkoReplicatedLog handleRecoveryMessage(final Object message) {
        LOG.trace("{}: handleRecoveryMessage: {}", memberId(), message);

        anyDataRecovered = anyDataRecovered || !(message instanceof RecoveryCompleted);

        if (isMigratedSerializable(message)) {
            hasMigratedDataRecovered = true;
        }

        switch (message) {
            case SnapshotOffer msg -> onRecoveredSnapshot(msg);
            case ReplicatedLogEntry msg -> onRecoveredJournalLogEntry(msg);
            case ApplyJournalEntries msg -> onRecoveredApplyLogEntries(msg.getToIndex());
            case DeleteEntries msg -> onDeleteEntries(msg);
            case VotingConfig msg -> actor.peerInfos().updateVotingConfig(msg);
            case UpdateElectionTerm(var termInfo) ->  setTermInfo(termInfo);
            case RecoveryCompleted msg -> {
                onRecoveryCompletedMessage();
                return pekkoLog;
            }
            default -> {
                // No-op
            }
        }
        return null;
    }

    final @NonNull String memberId() {
        return actor.memberId();
    }

    private void initRecoveryTimers() {
        if (recoveryTimer == null) {
            recoveryTimer = Stopwatch.createStarted();
        }
        if (snapshotTimer == null && snapshotInterval > 0) {
            snapshotTimer = Stopwatch.createStarted();
        }
    }

    @NonNullByDefault
    private void setTermInfo(final TermInfo termInfo) {
        actor.localAccess().termInfoStore().setTerm(termInfo);
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
                snapshot.lastApplied(), toStorage(snapshot.state()), timestamp);
        } catch (IOException e) {
            LOG.error("{}: failed to save local snapshot", memberId(), e);
            throw new UncheckedIOException(e);
        }

        LOG.info("{}: local snapshot saved in {}, deleting Pekko-persisted snapshots", memberId(), sw.stop());
        actor.nukePekkoSnapshots();
    }

    private @Nullable ToStorage<?> toStorage(final @Nullable State state) {
        final var support = snapshotCohort.support();
        return ToStorage.ofNullable(support.writer(), support.snapshotType().cast(state));
    }

    private void initializeLog(final Instant timestamp, final Snapshot recovered) {
        LOG.debug("{}: initializing from snapshot taken at {}", memberId(), timestamp);
        initRecoveryTimers();
        anyDataRecovered = true;

        for (var entry : recovered.getUnAppliedEntries()) {
            if (isMigratedPayload(entry)) {
                hasMigratedDataRecovered = true;
            }
        }

        final var toApply = processRecoveredSnapshot(recovered);

        // Create a replicated log with the snapshot information
        // The replicated log can be used later on to retrieve this snapshot
        // when we need to install it on a peer
        pekkoLog.resetToSnapshot(toApply);
        setTermInfo(toApply.termInfo());

        final var votingConfig = toApply.votingConfig();
        if (votingConfig != null) {
            actor.peerInfos().updateVotingConfig(votingConfig);
        }

        final var timer = Stopwatch.createStarted();

        // Apply the snapshot to the actors state
        final var snapshotState = toApply.state();
        if (snapshotState != null) {
            if (snapshotState.needsMigration()) {
                hasMigratedDataRecovered = true;
            }
            recoveryCohort.applyRecoveredSnapshot(snapshotState);
        }

        LOG.info("Recovery snapshot applied for {} in {}: snapshotIndex={}, snapshotTerm={}, journal-size={}",
            memberId(), timer.stop(), pekkoLog.getSnapshotIndex(), pekkoLog.getSnapshotTerm(), pekkoLog.size());
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
            hasMigratedDataRecovered = true;
        }

        if (command instanceof VotingConfig newVotingConfig) {
            actor.peerInfos().updateVotingConfig(newVotingConfig);
        }

        appendRecoveredEntry(logEntry);
    }

    @NonNullByDefault
    void appendRecoveredEntry(final ReplicatedLogEntry logEntry) {
        pekkoLog.append(logEntry);
    }

    void onRecoveredApplyLogEntries(final long toIndex) {
        long lastUnappliedIndex = pekkoLog.getLastApplied() + 1;

        if (LOG.isDebugEnabled()) {
            // it can happen that lastUnappliedIndex > toIndex, if the AJE is in the persistent journal
            // but the entry itself has made it to that state and recovered via the snapshot
            LOG.debug("{}: Received apply journal entries for recovery, applying to state: {} to {}", memberId(),
                lastUnappliedIndex, toIndex);
        }

        long lastApplied = lastUnappliedIndex - 1;
        for (long i = lastUnappliedIndex; i <= toIndex; i++) {
            final var logEntry = pekkoLog.lookup(i);
            if (logEntry == null) {
                // Shouldn't happen but cover it anyway.
                LOG.error("{}: Log entry not found for index {}", memberId(), i);
                break;
            }

            lastApplied++;
            initRecoveryTimers();

            // We deal with RaftCommands separately
            if (logEntry.command() instanceof StateCommand command) {
                batchRecoveredCommand(command);
            }

            if (snapshotTimer != null && snapshotTimer.elapsed(TimeUnit.SECONDS) >= snapshotInterval) {
                LOG.info("{}: Time for recovery snapshot", memberId());
                if (currentRecoveryBatchCount > 0) {
                    endCurrentLogRecoveryBatch();
                }
                pekkoLog.setLastApplied(lastApplied);
                pekkoLog.setCommitIndex(lastApplied);
                takeSnapshot(logEntry);
                LOG.info("{}: Resetting timer for the next recovery snapshot", memberId());
                snapshotTimer.reset().start();
            }
        }

        pekkoLog.setLastApplied(lastApplied);
        pekkoLog.setCommitIndex(lastApplied);
    }

    @Deprecated(since = "11.0.0", forRemoval = true)
    void onDeleteEntries(final DeleteEntries deleteEntries) {
        pekkoLog.removeRecoveredEntries(deleteEntries.getFromIndex());
    }

    private void batchRecoveredCommand(final StateCommand command) {
        if (currentRecoveryBatchCount == 0) {
            recoveryCohort.startLogRecoveryBatch(batchSize);
        }

        recoveryCohort.appendRecoveredCommand(command);

        if (++currentRecoveryBatchCount >= batchSize) {
            endCurrentLogRecoveryBatch();
        }
    }

    // Take an intermediate snapshot. This serves two functions:
    //   - trim the replicated log to last applied entry, ensuring minimal memory footprint
    //   - transfer Pekko-persisted messages to SnapshotStore
    // The second point is important, because we want to complete evacuating Pekko persistence completely before we
    // instantiate EntryStore.
    //
    // While it would seem we can use SnapshotManager to achieve this, that is not what we want here because:
    //   - recovery really should happen before we ever instantiate SnapshotManager
    //   - we want this to happen synchronously, without giving Pekko a chance to flood us with subsequent messages
    //   - SnapshotManager can only access EntryStore, whereas we have access to RaftActor's persistence directly
    //   - once we have migrated EntryStore to not use Pekko, we'll have explicit control over stored entries, and we
    //     do not want to be shifting entries from EntryStore to SnapshotStore.
    private void takeSnapshot(final EntryMeta logEntry) {
        LOG.info("{}: Taking snapshot on entry with index {}", memberId(), logEntry.index());

        final var sw = Stopwatch.createStarted();
        // FIXME: We really do not have followers at this point, but information from VotingConfig may indicate we do.
        //        We should inline newCaptureSnapshot() logic here with the appropriate specialization.
        final var peerInfos = actor.peerInfos();
        final var request = pekkoLog.newCaptureSnapshot(logEntry, -1, false, !peerInfos.isEmpty());
        final var lastSeq = actor.lastSequenceNr();
        final var snapshotState = snapshotCohort.takeSnapshot();
        final var timestamp = Instant.now();

        try {
            snapshotStore().saveSnapshot(
                new RaftSnapshot(peerInfos.votingConfig(true), request.getUnAppliedEntries()), request.lastApplied(),
                ToStorage.ofNullable(snapshotCohort.support().writer(), snapshotState), timestamp);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        actor.deleteMessages(lastSeq);

        pekkoLog.snapshotPreCommit(request.getLastAppliedIndex(), request.getLastAppliedTerm());
        pekkoLog.snapshotCommit();

        LOG.info("{}: Snapshot completed in {}, resetting timer for the next recovery snapshot", memberId(), sw.stop());
    }

    private void endCurrentLogRecoveryBatch() {
        recoveryCohort.applyCurrentLogRecoveryBatch();
        currentRecoveryBatchCount = 0;
    }

    private void onRecoveryCompletedMessage() {
        if (currentRecoveryBatchCount > 0) {
            endCurrentLogRecoveryBatch();
        }

        final String recoveryTime;
        if (recoveryTimer != null) {
            recoveryTime = " in " + recoveryTimer.stop();
            recoveryTimer = null;
        } else {
            recoveryTime = "";
        }

        if (snapshotTimer != null) {
            snapshotTimer.stop();
            snapshotTimer = null;
        }

        LOG.info("{}: Recovery completed {} - Switching actor to Follower - last log index = {}, last log term = {}, "
            + "snapshot index = {}, snapshot term = {}, journal size = {}", memberId(), recoveryTime,
            pekkoLog.lastIndex(), pekkoLog.lastTerm(), pekkoLog.getSnapshotIndex(), pekkoLog.getSnapshotTerm(),
            pekkoLog.size());


        // Populate property-based storage if needed, or roll-back any voting information leaked by recovery process
        final var infoStore = actor.localAccess().termInfoStore();
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

        if (completeRecovery()) {
            final var snapshot = recoveryCohort.getRestoreFromSnapshot();
            if (snapshot != null) {
                restoreFrom(snapshot);
            }
        }
    }

    boolean completeRecovery() {
        if (hasMigratedDataRecovered) {
            LOG.info("{}: Snapshot capture initiated after recovery due to migrated messages", memberId());
            saveRecoverySnapshot();
            return false;
        }
        return true;
    }

    @NonNullByDefault
    private void restoreFrom(final Snapshot snapshot) {
        if (anyDataRecovered) {
            LOG.warn("{}: The provided restore snapshot was not applied because the persistence store is not empty",
                memberId());
            return;
        }

        LOG.debug("{}: Restoring snapshot: {}", memberId(), snapshot);
        final var timestamp = Instant.now();
        initializeLog(timestamp, snapshot);

        try {
            snapshotStore().saveSnapshot(
                new RaftSnapshot(actor.peerInfos().votingConfig(true), snapshot.getUnAppliedEntries()),
                snapshot.lastApplied(), toStorage(snapshot.state()), timestamp);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void saveRecoverySnapshot() {
        takeSnapshot(pekkoLog.lastMeta());
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
        actor.deleteMessages(actor.lastSequenceNr());
    }

    @NonNullByDefault
    private static boolean isMigratedPayload(final LogEntry logEntry) {
        return isMigratedSerializable(logEntry.command().toSerialForm());
    }

    private static boolean isMigratedSerializable(final Object message) {
        return message instanceof MigratedSerializable migrated && migrated.isMigrated();
    }
}