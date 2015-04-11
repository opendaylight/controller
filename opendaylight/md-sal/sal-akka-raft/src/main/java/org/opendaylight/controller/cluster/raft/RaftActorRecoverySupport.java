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
import org.opendaylight.controller.cluster.raft.RaftActor.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.DeleteEntries;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.slf4j.Logger;

/**
 * Support class that handles persistence recovery for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorRecoverySupport {
    private final RaftActorContext context;
    private final RaftActorBehavior currentBehavior;
    private final RaftActorRecoveryCohort cohort;

    private int currentRecoveryBatchCount;

    private Stopwatch recoveryTimer;
    private final Logger log;

    RaftActorRecoverySupport(RaftActorContext context, RaftActorBehavior currentBehavior,
            RaftActorRecoveryCohort cohort) {
        this.context = context;
        this.currentBehavior = currentBehavior;
        this.cohort = cohort;
        this.log = context.getLogger();
    }

    boolean handleRecoveryMessage(Object message) {
        boolean recoveryComplete = false;
        if(context.getPersistenceProvider().isRecoveryApplicable()) {
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
            } else if (message instanceof org.opendaylight.controller.cluster.raft.RaftActor.DeleteEntries) {
                // Handle this message for backwards compatibility with pre-Lithium versions.
                replicatedLog().removeFrom(((org.opendaylight.controller.cluster.raft.RaftActor.DeleteEntries) message).getFromIndex());
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

        context.setReplicatedLog(ReplicatedLogImpl.newInstance(snapshot, context, currentBehavior));
        context.setLastApplied(snapshot.getLastAppliedIndex());
        context.setCommitIndex(snapshot.getLastAppliedIndex());

        Stopwatch timer = Stopwatch.createStarted();

        // Apply the snapshot to the actors state
        cohort.applyRecoverySnapshot(snapshot.getState());

        timer.stop();
        log.info("Recovery snapshot applied for {} in {}: snapshotIndex={}, snapshotTerm={}, journal-size={}",
                context.getId(), timer.toString(), replicatedLog().getSnapshotIndex(),
                replicatedLog().getSnapshotTerm(), replicatedLog().size());
    }

    private void onRecoveredJournalLogEntry(ReplicatedLogEntry logEntry) {
        if(log.isDebugEnabled()) {
            log.debug("{}: Received ReplicatedLogEntry for recovery: index: {}, size: {}", context.getId(),
                    logEntry.getIndex(), logEntry.size());
        }

        replicatedLog().append(logEntry);
    }

    private void onRecoveredApplyLogEntries(long toIndex) {
        long lastUnappliedIndex = context.getLastApplied() + 1;

        if(log.isDebugEnabled()) {
            log.debug("{}: Received apply journal entries for recovery, applying to state: {} to {}",
                    context.getId(), lastUnappliedIndex, toIndex);
        }

        long lastApplied = lastUnappliedIndex - 1;
        for (long i = lastUnappliedIndex; i <= toIndex; i++) {
            ReplicatedLogEntry logEntry = replicatedLog().get(i);
            if(logEntry != null) {
                lastApplied++;
                batchRecoveredLogEntry(logEntry);
            } else {
                // Shouldn't happen but cover it anyway.
                log.error("Log entry not found for index {}", i);
                break;
            }
        }

        context.setLastApplied(lastApplied);
        context.setCommitIndex(lastApplied);
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
