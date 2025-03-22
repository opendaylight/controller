/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.persistence.AbstractPersistentActor;
import org.apache.pekko.persistence.RecoveryCompleted;
import org.apache.pekko.persistence.SnapshotOffer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.MigratedSerializable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.spi.RaftEntryMeta;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class that handles persistence recovery for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorRecoverySupport {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorRecoverySupport.class);

    private final @NonNull LocalAccess localAccess;
    private final @NonNull RaftActorContext context;
    private final @NonNull RaftActorRecoveryCohort cohort;
    private final @Nullable TermInfo origTermInfo;

    private int currentRecoveryBatchCount;
    private boolean dataRecoveredWithPersistenceDisabled;
    private boolean anyDataRecovered;
    private boolean hasMigratedDataRecovered;

    private Stopwatch recoveryTimer;
    private Stopwatch recoverySnapshotTimer;

    RaftActorRecoverySupport(final @NonNull LocalAccess localAccess, final RaftActorContext context,
            final RaftActorRecoveryCohort cohort) {
        this.localAccess = requireNonNull(localAccess);
        this.context = requireNonNull(context);
        this.cohort = requireNonNull(cohort);
        try {
            origTermInfo = localAccess.termInfoStore().loadAndSetTerm();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    boolean handleRecoveryMessage(final AbstractPersistentActor actor, final Object message) {
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
            case ClusterConfig msg -> context.updatePeerIds(msg);
            case UpdateElectionTerm(var termInfo) -> context.setTermInfo(termInfo);
            case RecoveryCompleted msg -> {
                onRecoveryCompletedMessage(actor);
                return true;
            }
            default -> {
                // No-op
            }
        }
        return false;
    }

    private @NonNull String memberId() {
        return localAccess.memberId();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void possiblyRestoreFromSnapshot() {
        final var restoreFromSnapshot = cohort.getRestoreFromSnapshot();
        if (restoreFromSnapshot == null) {
            return;
        }

        if (anyDataRecovered) {
            LOG.warn("{}: The provided restore snapshot was not applied because the persistence store is not empty",
                    memberId());
            return;
        }

        LOG.debug("{}: Restore snapshot: {}", memberId(), restoreFromSnapshot);

        context.getSnapshotManager().applyFromRecovery(restoreFromSnapshot);
    }

    private ReplicatedLog replicatedLog() {
        return context.getReplicatedLog();
    }

    private void initRecoveryTimers() {
        if (recoveryTimer == null) {
            recoveryTimer = Stopwatch.createStarted();
        }
        if (recoverySnapshotTimer == null && context.getConfigParams().getRecoverySnapshotIntervalSeconds() > 0) {
            recoverySnapshotTimer = Stopwatch.createStarted();
        }
    }

    private void onRecoveredSnapshot(final SnapshotOffer offer) {
        LOG.debug("{}: SnapshotOffer called.", memberId());

        initRecoveryTimers();

        var snapshot = (Snapshot) offer.snapshot();

        for (var entry : snapshot.getUnAppliedEntries()) {
            if (isMigratedPayload(entry)) {
                hasMigratedDataRecovered = true;
            }
        }

        if (!context.getPersistenceProvider().isRecoveryApplicable()) {
            // We may have just transitioned to disabled and have a snapshot containing state data and/or log
            // entries - we don't want to preserve these, only the server config and election term info.

            snapshot = Snapshot.create(EmptyState.INSTANCE, List.of(), -1, -1, -1, -1,
                snapshot.termInfo(), snapshot.getServerConfiguration());
        }

        // Create a replicated log with the snapshot information
        // The replicated log can be used later on to retrieve this snapshot
        // when we need to install it on a peer

        context.setReplicatedLog(new ReplicatedLogImpl(context, snapshot));
        context.setTermInfo(snapshot.termInfo());

        final var timer = Stopwatch.createStarted();

        // Apply the snapshot to the actors state
        final State snapshotState = snapshot.getState();
        if (snapshotState.needsMigration()) {
            hasMigratedDataRecovered = true;
        }
        if (!(snapshotState instanceof EmptyState)) {
            cohort.applyRecoverySnapshot(snapshotState);
        }

        if (snapshot.getServerConfiguration() != null) {
            context.updatePeerIds(snapshot.getServerConfiguration());
        }

        timer.stop();
        final var replLog = replicatedLog();
        LOG.info("Recovery snapshot applied for {} in {}: snapshotIndex={}, snapshotTerm={}, journal-size={}",
                memberId(), timer, replLog.getSnapshotIndex(), replLog.getSnapshotTerm(), replLog.size());
    }

    private void onRecoveredJournalLogEntry(final ReplicatedLogEntry logEntry) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{}: Received ReplicatedLogEntry for recovery: index: {}, size: {}", memberId(),
                    logEntry.index(), logEntry.size());
        }

        final var data = logEntry.getData();
        if (isMigratedSerializable(data)) {
            hasMigratedDataRecovered = true;
        }

        if (data instanceof ClusterConfig clusterConfig) {
            context.updatePeerIds(clusterConfig);
        }

        if (context.getPersistenceProvider().isRecoveryApplicable()) {
            replicatedLog().append(logEntry);
        } else if (!(data instanceof ClusterConfig)) {
            dataRecoveredWithPersistenceDisabled = true;
        }
    }

    private void onRecoveredApplyLogEntries(final long toIndex) {
        if (!context.getPersistenceProvider().isRecoveryApplicable()) {
            dataRecoveredWithPersistenceDisabled = true;
            return;
        }

        final var replLog = replicatedLog();
        long lastUnappliedIndex = replLog.getLastApplied() + 1;

        if (LOG.isDebugEnabled()) {
            // it can happen that lastUnappliedIndex > toIndex, if the AJE is in the persistent journal
            // but the entry itself has made it to that state and recovered via the snapshot
            LOG.debug("{}: Received apply journal entries for recovery, applying to state: {} to {}", memberId(),
                lastUnappliedIndex, toIndex);
        }

        long lastApplied = lastUnappliedIndex - 1;
        for (long i = lastUnappliedIndex; i <= toIndex; i++) {
            final var logEntry = replicatedLog().get(i);
            if (logEntry != null) {
                lastApplied++;
                batchRecoveredLogEntry(logEntry);
                if (shouldTakeRecoverySnapshot() && !context.getSnapshotManager().isCapturing()) {
                    if (currentRecoveryBatchCount > 0) {
                        endCurrentLogRecoveryBatch();
                    }
                    replLog.setLastApplied(lastApplied);
                    replLog.setCommitIndex(lastApplied);
                    takeRecoverySnapshot(logEntry);
                }
            } else {
                // Shouldn't happen but cover it anyway.
                LOG.error("{}: Log entry not found for index {}", memberId(), i);
                break;
            }
        }

        replLog.setLastApplied(lastApplied);
        replLog.setCommitIndex(lastApplied);
    }

    private void onDeleteEntries(final DeleteEntries deleteEntries) {
        if (context.getPersistenceProvider().isRecoveryApplicable()) {
            replicatedLog().removeFrom(deleteEntries.getFromIndex());
        } else {
            dataRecoveredWithPersistenceDisabled = true;
        }
    }

    private void batchRecoveredLogEntry(final ReplicatedLogEntry logEntry) {
        initRecoveryTimers();

        if (logEntry.getData() instanceof ClusterConfig) {
            // FIXME: explain why ClusterConfig is special
            return;
        }

        int batchSize = context.getConfigParams().getJournalRecoveryLogBatchSize();
        if (currentRecoveryBatchCount == 0) {
            cohort.startLogRecoveryBatch(batchSize);
        }

        cohort.appendRecoveredLogEntry(logEntry.getData());

        if (++currentRecoveryBatchCount >= batchSize) {
            endCurrentLogRecoveryBatch();
        }
    }

    private void takeRecoverySnapshot(final RaftEntryMeta logEntry) {
        LOG.info("Time for recovery snapshot on entry with index {}", logEntry.index());
        final var snapshotManager = context.getSnapshotManager();
        if (snapshotManager.capture(logEntry, -1)) {
            LOG.info("Capturing snapshot, resetting timer for the next recovery snapshot interval.");
            recoverySnapshotTimer.reset().start();
        } else {
            LOG.info("SnapshotManager is not able to capture snapshot at this time. It will be retried "
                + "again with the next recovered entry.");
        }
    }

    private boolean shouldTakeRecoverySnapshot() {
        return recoverySnapshotTimer != null && recoverySnapshotTimer.elapsed(TimeUnit.SECONDS)
            >= context.getConfigParams().getRecoverySnapshotIntervalSeconds();
    }

    private void endCurrentLogRecoveryBatch() {
        cohort.applyCurrentLogRecoveryBatch();
        currentRecoveryBatchCount = 0;
    }

    private void onRecoveryCompletedMessage(final AbstractPersistentActor raftActor) {
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

        if (recoverySnapshotTimer != null) {
            recoverySnapshotTimer.stop();
            recoverySnapshotTimer = null;
        }

        final var replLog = replicatedLog();
        LOG.info("{}: Recovery completed {} - Switching actor to Follower - last log index = {}, last log term = {}, "
                + "snapshot index = {}, snapshot term = {}, journal size = {}", memberId(), recoveryTime,
                replLog.lastIndex(), replLog.lastTerm(), replLog.getSnapshotIndex(), replLog.getSnapshotTerm(),
                replLog.size());


        // Populate property-based storage if needed, or roll-back any voting information leaked by recovery process
        final var infoStore = localAccess.termInfoStore();
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

        if (dataRecoveredWithPersistenceDisabled
                || hasMigratedDataRecovered && !context.getPersistenceProvider().isRecoveryApplicable()) {
            if (hasMigratedDataRecovered) {
                LOG.info("{}: Saving snapshot after recovery due to migrated messages", memberId());
            } else {
                LOG.info("{}: Saving snapshot after recovery due to data persistence disabled", memberId());
            }

            // Either data persistence is disabled and we recovered some data entries (ie we must have just
            // transitioned to disabled or a persistence backup was restored) or we recovered migrated
            // messages. Either way, we persist a snapshot and delete all the messages from the akka journal
            // to clean out unwanted messages.

            Snapshot snapshot = Snapshot.create(EmptyState.INSTANCE, List.of(), -1, -1, -1, -1, context.termInfo(),
                context.getPeerServerInfo(true));

            raftActor.saveSnapshot(snapshot);
            raftActor.deleteMessages(raftActor.lastSequenceNr());
        } else if (hasMigratedDataRecovered) {
            LOG.info("{}: Snapshot capture initiated after recovery due to migrated messages", memberId());

            context.getSnapshotManager().capture(replLog.lastMeta(), -1);
        } else {
            possiblyRestoreFromSnapshot();
        }
    }

    private static boolean isMigratedPayload(final ReplicatedLogEntry repLogEntry) {
        return isMigratedSerializable(repLogEntry.getData());
    }

    private static boolean isMigratedSerializable(final Object message) {
        return message instanceof MigratedSerializable migrated && migrated.isMigrated();
    }
}
