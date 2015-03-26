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
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.RaftActor.DeleteEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.messages.UpdateElectionTerm;
import org.slf4j.Logger;

/**
 * Support class that handles persistence recovery for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorRecoverySupport {
    private final DataPersistenceProvider persistence;
    private final RaftActorContext context;
    private final RaftActorBehavior currentBehavior;
    private final RaftActorRecoveryCohort cohort;

    private int currentRecoveryBatchCount;

    private Stopwatch recoveryTimer;
    private final Logger log;

    RaftActorRecoverySupport(DataPersistenceProvider persistence, RaftActorContext context,
            RaftActorBehavior currentBehavior, RaftActorRecoveryCohort cohort) {
        this.persistence = persistence;
        this.context = context;
        this.currentBehavior = currentBehavior;
        this.cohort = cohort;
        this.log = context.getLogger();
    }

    boolean handleRecoveryMessage(Object message) {
        boolean recoveryComplete = false;
        if(persistence.isRecoveryApplicable()) {
            if (message instanceof SnapshotOffer) {
                onRecoveredSnapshot((SnapshotOffer) message);
            } else if (message instanceof ReplicatedLogEntry) {
                onRecoveredJournalLogEntry((ReplicatedLogEntry) message);
            } else if (message instanceof ApplyLogEntries) {
                // Handle this message for backwards compatibility with pre-Lithium versions.
                onRecoveredApplyLogEntries(((ApplyLogEntries) message).getToIndex());
            } else if (message instanceof ApplyJournalEntries) {
                onRecoveredApplyLogEntries(((ApplyJournalEntries) message).getToIndex());
            } else if (message instanceof DeleteEntries) {
                replicatedLog().removeFrom(((DeleteEntries) message).getFromIndex());
            } else if (message instanceof UpdateElectionTerm) {
                context.getTermInformation().update(((UpdateElectionTerm) message).getCurrentTerm(),
                        ((UpdateElectionTerm) message).getVotedFor());
            } else if (message instanceof RecoveryCompleted) {
                onRecoveryCompletedMessage();
                recoveryComplete = true;
            }
        } else if (message instanceof RecoveryCompleted) {
            recoveryComplete = true;
        }

        return recoveryComplete;
    }

    private ReplicatedLog replicatedLog() {
        return context.getReplicatedLog();
    }

    private void initRecoveryTimer() {
        if(recoveryTimer == null) {
            recoveryTimer = Stopwatch.createStarted();
        }
    }

    private void onRecoveredSnapshot(SnapshotOffer offer) {
        if(log.isDebugEnabled()) {
            log.debug("{}: SnapshotOffer called..", context.getId());
        }

        initRecoveryTimer();

        Snapshot snapshot = (Snapshot) offer.snapshot();

        // Create a replicated log with the snapshot information
        // The replicated log can be used later on to retrieve this snapshot
        // when we need to install it on a peer

        context.setReplicatedLog(ReplicatedLogImpl.newInstance(snapshot, context, persistence, currentBehavior));
        context.setLastApplied(snapshot.getLastAppliedIndex());
        context.setCommitIndex(snapshot.getLastAppliedIndex());

        Stopwatch timer = Stopwatch.createStarted();

        // Apply the snapshot to the actors state
        cohort.applyRecoverySnapshot(snapshot.getState());

        timer.stop();
        log.info("Recovery snapshot applied for {} in {}: snapshotIndex={}, snapshotTerm={}, journal-size=" +
                replicatedLog().size(), context.getId(), timer.toString(),
                replicatedLog().getSnapshotIndex(), replicatedLog().getSnapshotTerm());
    }

    private void onRecoveredJournalLogEntry(ReplicatedLogEntry logEntry) {
        if(log.isDebugEnabled()) {
            log.debug("{}: Received ReplicatedLogEntry for recovery: {}", context.getId(), logEntry.getIndex());
        }

        replicatedLog().append(logEntry);
    }

    private void onRecoveredApplyLogEntries(long toIndex) {
        if(log.isDebugEnabled()) {
            log.debug("{}: Received ApplyLogEntries for recovery, applying to state: {} to {}",
                    context.getId(), context.getLastApplied() + 1, toIndex);
        }

        for (long i = context.getLastApplied() + 1; i <= toIndex; i++) {
            batchRecoveredLogEntry(replicatedLog().get(i));
        }

        context.setLastApplied(toIndex);
        context.setCommitIndex(toIndex);
    }

    private void batchRecoveredLogEntry(ReplicatedLogEntry logEntry) {
        initRecoveryTimer();

        int batchSize = context.getConfigParams().getJournalRecoveryLogBatchSize();
        if(currentRecoveryBatchCount == 0) {
            cohort.startLogRecoveryBatch(batchSize);
        }

        cohort.appendRecoveredLogEntry(logEntry.getData());

        if(++currentRecoveryBatchCount >= batchSize) {
            endCurrentLogRecoveryBatch();
        }
    }

    private void endCurrentLogRecoveryBatch() {
        cohort.applyCurrentLogRecoveryBatch();
        currentRecoveryBatchCount = 0;
    }

    private void onRecoveryCompletedMessage() {
        if(currentRecoveryBatchCount > 0) {
            endCurrentLogRecoveryBatch();
        }

        String recoveryTime = "";
        if(recoveryTimer != null) {
            recoveryTimer.stop();
            recoveryTime = " in " + recoveryTimer.toString();
            recoveryTimer = null;
        }

        log.info("Recovery completed" + recoveryTime + " - Switching actor to Follower - " +
                 "Persistence Id =  " + context.getId() +
                 " Last index in log = {}, snapshotIndex = {}, snapshotTerm = {}, " +
                 "journal-size = {}", replicatedLog().lastIndex(), replicatedLog().getSnapshotIndex(),
                 replicatedLog().getSnapshotTerm(), replicatedLog().size());
    }
}
