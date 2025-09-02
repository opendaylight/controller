/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.MigratedSerializable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.StateCommand;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.Support;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.controller.cluster.raft.spi.TermInfoStore;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.api.EntryMeta;
import org.opendaylight.raft.api.TermInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for recovery implementations.
 *
 * @param <T> {@link State} type
 */
abstract sealed class Recovery<T extends @NonNull State> permits JournalRecovery, TransientRecovery {
    private static final Logger LOG = LoggerFactory.getLogger(Recovery.class);

    final @NonNull RaftActorRecoveryCohort recoveryCohort;
    final @NonNull RecoveryLog recoveryLog;
    final @NonNull RaftActor actor;
    private final @NonNull RaftActorSnapshotCohort<T> snapshotCohort;
    private final int snapshotInterval;
    private final int maxBatchSize;

    // our timers
    private Stopwatch recoveryTimer;
    private Stopwatch snapshotTimer;

    // the number of commands we have submitted to recoveryCohort
    private int currentBatchSize;

    // tracking of whether we have recovered any data and whether there was a migrated payload
    private boolean dataRecovered;
    private boolean migratedDataRecovered;

    @NonNullByDefault
    Recovery(final RaftActor actor, final RaftActorSnapshotCohort<T> snapshotCohort,
            final RaftActorRecoveryCohort recoveryCohort, final ConfigParams configParams) {
        this.actor = requireNonNull(actor);
        this.snapshotCohort = requireNonNull(snapshotCohort);
        this.recoveryCohort = requireNonNull(recoveryCohort);
        snapshotInterval = configParams.getRecoverySnapshotIntervalSeconds();
        maxBatchSize = configParams.getJournalRecoveryLogBatchSize();
        recoveryLog = new RecoveryLog(actor.memberId());
    }

    @NonNullByDefault
    final RecoveryResult recover() throws IOException {
        startRecoveryTimers();

        // TermInfo first
        var termInfo = termInfoStore().loadAndSetTerm();
        if (termInfo != null) {
            setDataRecovered();
        }

        // Snapshot next
        final var loaded = snapshotStore().lastSnapshot();
        if (loaded != null) {
            LOG.debug("{}: initializing from snapshot taken at {}", memberId(), loaded.timestamp());
            setDataRecovered();

            final var lastIncluded = loaded.lastIncluded();
            if (termInfo == null) {
                termInfo = new TermInfo(lastIncluded.term());
                LOG.warn("{}: recovered snapshot without TermInfo, assuming {}", memberId(), termInfo);
            }

            final var raftSnapshot = loaded.readRaftSnapshot(actor.objectStreams());
            final var votingConfig = raftSnapshot.votingConfig();
            if (votingConfig != null) {
                actor.peerInfos().updateVotingConfig(votingConfig);
            }
            final var unappliedEntries = raftSnapshot.unappliedEntries();
            for (var entry : unappliedEntries) {
                if (isMigratedPayload(entry)) {
                    setMigratedDataRecovered();
                }
            }

            doRecover(loaded.lastIncluded(), loaded.readSnapshot(snapshotCohort.support().reader()), unappliedEntries);
        } else {
            doRecover(EntryInfo.of(-1, -1), null, List.of());
        }

        // flush everything we recovered
       applyRecoveredCommands();

        final var recoveryTime = stopRecoveryTimers();
        LOG.info("{}: Recovery completed in {}: last log index = {}, last log term = {}, napshot index = {}, "
            + "snapshot term = {}, journal size = {}", memberId(), recoveryTime, recoveryLog.lastIndex(),
            recoveryLog.lastTerm(), recoveryLog.getSnapshotIndex(), recoveryLog.getSnapshotTerm(), recoveryLog.size());

        if (migratedDataRecovered()) {
            LOG.info("{}: Snapshot capture initiated after recovery due to migrated messages", memberId());
            saveFinalSnapshot();
            return new RecoveryResult(recoveryLog, false);
        }
        return new RecoveryResult(recoveryLog, !dataRecovered());
    }

    @NonNullByDefault
    abstract void doRecover(EntryInfo lastIncluded, @Nullable State state, List<LogEntry> entries)
        throws IOException;

    @NonNullByDefault
    final void initializeState(final EntryInfo lastIncluded, final @Nullable State state) {
        if (state != null) {
            if (state.needsMigration()) {
                setMigratedDataRecovered();
            }
            recoveryCohort.applyRecoveredSnapshot(state);
            actor.recoveryObserver().onSnapshotRecovered(state);
        }

        // Safety check
        verify(recoveryLog.size() == 0, "Non-empty recovery log %s", recoveryLog);

        final var snapshotIndex = lastIncluded.index();
        recoveryLog.setSnapshotIndex(snapshotIndex);
        recoveryLog.setLastApplied(snapshotIndex);
        recoveryLog.setCommitIndex(snapshotIndex);
        recoveryLog.setSnapshotTerm(lastIncluded.term());
    }

    @NonNullByDefault
    final String memberId() {
        return actor.memberId();
    }

    @NonNullByDefault
    final SnapshotStore snapshotStore() {
        return actor.persistence().snapshotStore();
    }

    @NonNullByDefault
    final TermInfoStore termInfoStore() {
        return actor.localAccess().termInfoStore();
    }

    @NonNullByDefault
    final Support<T> support() {
        return snapshotCohort.support();
    }

    final void startRecoveryTimers() {
        if (recoveryTimer == null) {
            recoveryTimer = Stopwatch.createStarted();
        }
        if (snapshotTimer == null && snapshotInterval > 0) {
            snapshotTimer = Stopwatch.createStarted();
        }
    }

    @NonNullByDefault
    final String stopRecoveryTimers() {
        final String recoveryTime;
        if (recoveryTimer != null) {
            recoveryTime = "in " + recoveryTimer.stop();
            recoveryTimer = null;
        } else {
            recoveryTime = "";
        }

        if (snapshotTimer != null) {
            snapshotTimer.stop();
            snapshotTimer = null;
        }
        return recoveryTime;
    }

    @NonNullByDefault
    final void applyEntry(final LogEntry logEntry) {
        // We deal with RaftCommands separately
        if (logEntry.command() instanceof StateCommand command) {
            if (currentBatchSize == 0) {
                recoveryCohort.startLogRecoveryBatch(maxBatchSize);
            }

            recoveryCohort.appendRecoveredCommand(command);

            if (++currentBatchSize >= maxBatchSize) {
                applyRecoveryBatch();
            }
        }

        if (snapshotTimer != null && snapshotTimer.elapsed(TimeUnit.SECONDS) >= snapshotInterval) {
            final var lastApplied = logEntry.index();

            LOG.info("{}: Time for recovery snapshot", memberId());
            applyRecoveredCommands();
            recoveryLog.setLastApplied(lastApplied);
            recoveryLog.setCommitIndex(lastApplied);
            takeSnapshot(logEntry);
            LOG.info("{}: Resetting timer for the next recovery snapshot", memberId());
            snapshotTimer.reset().start();
        }
    }

    final void applyRecoveredCommands() {
        if (currentBatchSize > 0) {
            applyRecoveryBatch();
        }
    }

    private void applyRecoveryBatch() {
        recoveryCohort.applyCurrentLogRecoveryBatch();
        currentBatchSize = 0;
    }

    // Called after we have applied an entry: lastMeta cannot return null and it matches lastApplied
    void saveRecoverySnapshot() {
        takeSnapshot(verifyNotNull(recoveryLog.lastMeta()));
    }

    // Called after recovery has been completed to clean out persistence: there may not be any entries in the log at all
    void saveFinalSnapshot() {
        var lastApplied = recoveryLog.lookupMeta(recoveryLog.getLastApplied());
        if (lastApplied == null) {
            lastApplied = EntryInfo.of(recoveryLog.getSnapshotIndex(), recoveryLog.getSnapshotTerm());
            LOG.debug("{}: no applied entries in recovery log, re-snapshotting {}", memberId(), lastApplied);
        }
        takeSnapshot(lastApplied);
    }

    // Either data persistence is disabled and we recovered some data entries (i.e. we must have just transitioned
    // to disabled or a persistence backup was restored) or we recovered migrated messages. Either way, we persist
    // a snapshot and delete all the messages from the Pekko journal to clean out unwanted messages.
    final void saveEmptySnapshot() {
        try {
            actor.saveEmptySnapshot();
            discardSnapshottedEntries();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Take an intermediate recovery snapshot. This serves two functions:
     * <ol>
     *   <li>trim the replicated log to last applied entry, ensuring minimal memory footprint</li>
     *   <li>transfer Pekko-persisted messages to SnapshotStore</li>
     * </ol>
     * The second point is important, because we want to complete evacuating Pekko persistence completely before we
     * instantiate EntryStore.
     *
     * <p>While it would seem we can use SnapshotManager to achieve this, that is not what we want here because:
     * <ul>
     *  <li>recovery really should happen before we ever instantiate SnapshotManager</li>
     *  <li>we want this to happen synchronously, without giving Pekko a chance to flood us with subsequent messages
     *  </li>
     *  <li>SnapshotManager can only access EntryStore, whereas we have access to RaftActor's persistence directly</li>
     *  <li>once we have migrated EntryStore to not use Pekko, we'll have explicit control over stored entries, and we
     *       do not want to be shifting entries from EntryStore to SnapshotStore<li>
     * </ul>
     *
     * @param logEntry the last log entry for the snapshot
     */
    @NonNullByDefault
    private void takeSnapshot(final EntryMeta logEntry) {
        LOG.info("{}: Taking snapshot on entry with index {}", memberId(), logEntry.index());

        final var sw = Stopwatch.createStarted();
        // FIXME: We really do not have followers at this point, but information from VotingConfig may indicate we do.
        //        We should inline newCaptureSnapshot() logic here with the appropriate specialization.
        final var peerInfos = actor.peerInfos();
        final var request = recoveryLog.newCaptureSnapshot(logEntry, -1, false, !peerInfos.isEmpty());
        final var snapshotState = snapshotCohort.takeSnapshot();

        try {
            snapshotStore().saveSnapshot(
                new RaftSnapshot(peerInfos.votingConfig(true),
                    filterSnapshotUnappliedEntries(request.getUnAppliedEntries())), request.lastApplied(),
                ToStorage.ofNullable(snapshotCohort.support().writer(), snapshotState), Instant.now());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        recoveryLog.snapshotPreCommit(request.getLastAppliedIndex(), request.getLastAppliedTerm());
        recoveryLog.snapshotCommit();

        try {
            discardSnapshottedEntries();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to discard journal head", e);
        }

        LOG.info("{}: Snapshot completed in {}, resetting timer for the next recovery snapshot", memberId(), sw.stop());
    }

    @Deprecated(forRemoval = true)
    @NonNullByDefault
    List<LogEntry> filterSnapshotUnappliedEntries(List<LogEntry> unappliedEntries) {
        return List.of();
    }

    abstract void discardSnapshottedEntries() throws IOException;

    final boolean dataRecovered() {
        return dataRecovered;
    }

    final void setDataRecovered() {
        dataRecovered = true;
    }

    final boolean migratedDataRecovered() {
        return migratedDataRecovered;
    }

    final void setMigratedDataRecovered() {
        migratedDataRecovered = true;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("memberId", memberId()).toString();
    }

    @NonNullByDefault
    static final boolean isMigratedPayload(final LogEntry logEntry) {
        return isMigratedSerializable(logEntry.command().toSerialForm());
    }

    @NonNullByDefault
    static final boolean isMigratedSerializable(final Object message) {
        return message instanceof MigratedSerializable migrated && migrated.isMigrated();
    }
}
