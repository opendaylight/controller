/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotOffer;
import com.google.common.base.Stopwatch;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.PersistentDataProvider;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.messages.PersistentPayload;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.MigratedSerializable;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.slf4j.Logger;

/**
 * Support class that handles persistence recovery for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorRecoverySupport {
    private final RaftActorContext context;
    private final RaftActorRecoveryCohort cohort;

    private int currentRecoveryBatchCount;
    private boolean dataRecoveredWithPersistenceDisabled;
    private boolean anyDataRecovered;
    private boolean hasMigratedDataRecovered;

    private Stopwatch recoveryTimer;
    private Stopwatch recoverySnapshotTimer;
    private final Logger log;

    RaftActorRecoverySupport(final RaftActorContext context, final RaftActorRecoveryCohort cohort) {
        this.context = context;
        this.cohort = cohort;
        log = context.getLogger();
    }

    boolean handleRecoveryMessage(final Object message, final PersistentDataProvider persistentProvider) {
        log.trace("{}: handleRecoveryMessage: {}", context.getId(), message);

        anyDataRecovered = anyDataRecovered || !(message instanceof RecoveryCompleted);

        if (isMigratedSerializable(message)) {
            hasMigratedDataRecovered = true;
        }

        boolean recoveryComplete = false;
        if (message instanceof UpdateElectionTerm updateElectionTerm) {
            context.getTermInformation().update(updateElectionTerm.getCurrentTerm(), updateElectionTerm.getVotedFor());
        } else if (message instanceof SnapshotOffer snapshotOffer) {
            onRecoveredSnapshot(snapshotOffer);
        } else if (message instanceof ReplicatedLogEntry replicatedLogEntry) {
            onRecoveredJournalLogEntry(replicatedLogEntry);
        } else if (message instanceof ApplyJournalEntries applyJournalEntries) {
            onRecoveredApplyLogEntries(applyJournalEntries.getToIndex());
        } else if (message instanceof DeleteEntries deleteEntries) {
            onDeleteEntries(deleteEntries);
        } else if (message instanceof ServerConfigurationPayload serverConfigurationPayload) {
            context.updatePeerIds(serverConfigurationPayload);
        } else if (message instanceof RecoveryCompleted) {
            recoveryComplete = true;
            onRecoveryCompletedMessage(persistentProvider);
        }

        return recoveryComplete;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void possiblyRestoreFromSnapshot() {
        Snapshot restoreFromSnapshot = cohort.getRestoreFromSnapshot();
        if (restoreFromSnapshot == null) {
            return;
        }

        if (anyDataRecovered) {
            log.warn("{}: The provided restore snapshot was not applied because the persistence store is not empty",
                    context.getId());
            return;
        }

        log.debug("{}: Restore snapshot: {}", context.getId(), restoreFromSnapshot);

        context.getSnapshotManager().apply(new ApplySnapshot(restoreFromSnapshot));
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
        log.debug("{}: SnapshotOffer called.", context.getId());

        initRecoveryTimers();

        Snapshot snapshot = (Snapshot) offer.snapshot();

        for (ReplicatedLogEntry entry: snapshot.getUnAppliedEntries()) {
            if (isMigratedPayload(entry)) {
                hasMigratedDataRecovered = true;
            }
        }

        if (!context.getPersistenceProvider().isRecoveryApplicable()) {
            // We may have just transitioned to disabled and have a snapshot containing state data and/or log
            // entries - we don't want to preserve these, only the server config and election term info.

            snapshot = Snapshot.create(
                    EmptyState.INSTANCE, Collections.emptyList(), -1, -1, -1, -1,
                    snapshot.getElectionTerm(), snapshot.getElectionVotedFor(), snapshot.getServerConfiguration());
        }

        // Create a replicated log with the snapshot information
        // The replicated log can be used later on to retrieve this snapshot
        // when we need to install it on a peer

        context.setReplicatedLog(ReplicatedLogImpl.newInstance(snapshot, context));
        context.setLastApplied(snapshot.getLastAppliedIndex());
        context.setCommitIndex(snapshot.getLastAppliedIndex());
        context.getTermInformation().update(snapshot.getElectionTerm(), snapshot.getElectionVotedFor());

        final Stopwatch timer = Stopwatch.createStarted();

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
        log.info("Recovery snapshot applied for {} in {}: snapshotIndex={}, snapshotTerm={}, journal-size={}",
                context.getId(), timer, replicatedLog().getSnapshotIndex(), replicatedLog().getSnapshotTerm(),
                replicatedLog().size());
    }

    private void onRecoveredJournalLogEntry(final ReplicatedLogEntry logEntry) {
        if (log.isDebugEnabled()) {
            log.debug("{}: Received ReplicatedLogEntry for recovery: index: {}, size: {}", context.getId(),
                    logEntry.getIndex(), logEntry.size());
        }

        if (isServerConfigurationPayload(logEntry)) {
            context.updatePeerIds((ServerConfigurationPayload)logEntry.getData());
        }

        if (isMigratedPayload(logEntry)) {
            hasMigratedDataRecovered = true;
        }

        if (context.getPersistenceProvider().isRecoveryApplicable()) {
            replicatedLog().append(logEntry);
        } else if (!isPersistentPayload(logEntry)) {
            dataRecoveredWithPersistenceDisabled = true;
        }
    }

    private void onRecoveredApplyLogEntries(final long toIndex) {
        if (!context.getPersistenceProvider().isRecoveryApplicable()) {
            dataRecoveredWithPersistenceDisabled = true;
            return;
        }

        long lastUnappliedIndex = context.getLastApplied() + 1;

        if (log.isDebugEnabled()) {
            // it can happen that lastUnappliedIndex > toIndex, if the AJE is in the persistent journal
            // but the entry itself has made it to that state and recovered via the snapshot
            log.debug("{}: Received apply journal entries for recovery, applying to state: {} to {}",
                    context.getId(), lastUnappliedIndex, toIndex);
        }

        long lastApplied = lastUnappliedIndex - 1;
        for (long i = lastUnappliedIndex; i <= toIndex; i++) {
            ReplicatedLogEntry logEntry = replicatedLog().get(i);
            if (logEntry != null) {
                lastApplied++;
                batchRecoveredLogEntry(logEntry);
                if (shouldTakeRecoverySnapshot() && !context.getSnapshotManager().isCapturing()) {
                    if (currentRecoveryBatchCount > 0) {
                        endCurrentLogRecoveryBatch();
                    }
                    context.setLastApplied(lastApplied);
                    context.setCommitIndex(lastApplied);
                    takeRecoverySnapshot(logEntry);
                }
            } else {
                // Shouldn't happen but cover it anyway.
                log.error("{}: Log entry not found for index {}", context.getId(), i);
                break;
            }
        }

        context.setLastApplied(lastApplied);
        context.setCommitIndex(lastApplied);
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

        int batchSize = context.getConfigParams().getJournalRecoveryLogBatchSize();
        if (!isServerConfigurationPayload(logEntry)) {
            if (currentRecoveryBatchCount == 0) {
                cohort.startLogRecoveryBatch(batchSize);
            }

            cohort.appendRecoveredLogEntry(logEntry.getData());

            if (++currentRecoveryBatchCount >= batchSize) {
                endCurrentLogRecoveryBatch();
            }
        }
    }

    private void takeRecoverySnapshot(final ReplicatedLogEntry logEntry) {
        log.info("Time for recovery snapshot on entry with index {}", logEntry.getIndex());
        final SnapshotManager snapshotManager = context.getSnapshotManager();
        if (snapshotManager.capture(logEntry, -1)) {
            log.info("Capturing snapshot, resetting timer for the next recovery snapshot interval.");
            recoverySnapshotTimer.reset().start();
        } else {
            log.info("SnapshotManager is not able to capture snapshot at this time. It will be retried "
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

    private void onRecoveryCompletedMessage(final PersistentDataProvider persistentProvider) {
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

        log.info("{}: Recovery completed {} - Switching actor to Follower - last log index = {}, last log term = {}, "
                + "snapshot index = {}, snapshot term = {}, journal size = {}", context.getId(), recoveryTime,
                replicatedLog().lastIndex(), replicatedLog().lastTerm(), replicatedLog().getSnapshotIndex(),
                replicatedLog().getSnapshotTerm(), replicatedLog().size());

        if (dataRecoveredWithPersistenceDisabled
                || hasMigratedDataRecovered && !context.getPersistenceProvider().isRecoveryApplicable()) {
            if (hasMigratedDataRecovered) {
                log.info("{}: Saving snapshot after recovery due to migrated messages", context.getId());
            } else {
                log.info("{}: Saving snapshot after recovery due to data persistence disabled", context.getId());
            }

            // Either data persistence is disabled and we recovered some data entries (ie we must have just
            // transitioned to disabled or a persistence backup was restored) or we recovered migrated
            // messages. Either way, we persist a snapshot and delete all the messages from the akka journal
            // to clean out unwanted messages.

            Snapshot snapshot = Snapshot.create(
                    EmptyState.INSTANCE, Collections.<ReplicatedLogEntry>emptyList(),
                    -1, -1, -1, -1,
                    context.getTermInformation().getCurrentTerm(), context.getTermInformation().getVotedFor(),
                    context.getPeerServerInfo(true));

            persistentProvider.saveSnapshot(snapshot);

            persistentProvider.deleteMessages(persistentProvider.getLastSequenceNumber());
        } else if (hasMigratedDataRecovered) {
            log.info("{}: Snapshot capture initiated after recovery due to migrated messages", context.getId());

            context.getSnapshotManager().capture(replicatedLog().last(), -1);
        } else {
            possiblyRestoreFromSnapshot();
        }
    }

    private static boolean isServerConfigurationPayload(final ReplicatedLogEntry repLogEntry) {
        return repLogEntry.getData() instanceof ServerConfigurationPayload;
    }

    private static boolean isPersistentPayload(final ReplicatedLogEntry repLogEntry) {
        return repLogEntry.getData() instanceof PersistentPayload;
    }

    private static boolean isMigratedPayload(final ReplicatedLogEntry repLogEntry) {
        return isMigratedSerializable(repLogEntry.getData());
    }

    private static boolean isMigratedSerializable(final Object message) {
        return message instanceof MigratedSerializable migrated && migrated.isMigrated();
    }
}
