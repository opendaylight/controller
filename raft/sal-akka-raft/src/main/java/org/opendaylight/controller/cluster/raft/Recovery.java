/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

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
import org.opendaylight.controller.cluster.raft.persisted.MigratedSerializable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.StateCommand;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.Support;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.api.EntryMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for recovery implementations.
 *
 * @param <T> {@link State} type
 */
abstract sealed class Recovery<T extends @NonNull State> permits JournalRecovery {
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
    final String memberId() {
        return actor.memberId();
    }

    @NonNullByDefault
    final SnapshotStore snapshotStore() {
        return actor.persistence().snapshotStore();
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

    @NonNullByDefault
    abstract List<LogEntry> filterSnapshotUnappliedEntries(List<LogEntry> unappliedEntries);

    abstract void discardSnapshottedEntries() throws IOException;

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
