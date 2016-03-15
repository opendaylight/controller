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
import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.base.Stopwatch;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.PersistentDataProvider;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.DeleteEntries;
import org.opendaylight.controller.cluster.raft.base.messages.UpdateElectionTerm;
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

    private Stopwatch recoveryTimer;
    private final Logger log;

    RaftActorRecoverySupport(final RaftActorContext context, final RaftActorRecoveryCohort cohort) {
        this.context = context;
        this.cohort = cohort;
        this.log = context.getLogger();
    }

    boolean handleRecoveryMessage(Object message, PersistentDataProvider persistentProvider) {
        log.trace("{}: handleRecoveryMessage: {}", context.getId(), message);

        anyDataRecovered = anyDataRecovered || !(message instanceof RecoveryCompleted);

        boolean recoveryComplete = false;
        DataPersistenceProvider persistence = context.getPersistenceProvider();
        if (message instanceof org.opendaylight.controller.cluster.raft.RaftActor.UpdateElectionTerm) {
            // Handle this message for backwards compatibility with pre-Lithium versions.
            org.opendaylight.controller.cluster.raft.RaftActor.UpdateElectionTerm update =
                    (org.opendaylight.controller.cluster.raft.RaftActor.UpdateElectionTerm)message;
            context.getTermInformation().update(update.getCurrentTerm(), update.getVotedFor());
        } else if (message instanceof UpdateElectionTerm) {
            context.getTermInformation().update(((UpdateElectionTerm) message).getCurrentTerm(),
                    ((UpdateElectionTerm) message).getVotedFor());
        } else if(persistence.isRecoveryApplicable()) {
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
            } else if (message instanceof RecoveryCompleted) {
                onRecoveryCompletedMessage();
                possiblyRestoreFromSnapshot();
                recoveryComplete = true;
            }
        } else if (message instanceof RecoveryCompleted) {
            recoveryComplete = true;

            if(dataRecoveredWithPersistenceDisabled) {
                // Data persistence is disabled but we recovered some data entries so we must have just
                // transitioned to disabled or a persistence backup was restored. Either way, delete all the
                // messages from the akka journal for efficiency and so that we do not end up with consistency
                // issues in case persistence is -re-enabled.
                persistentProvider.deleteMessages(persistentProvider.getLastSequenceNumber());

                // Delete all the akka snapshots as they will not be needed
                persistentProvider.deleteSnapshots(new SnapshotSelectionCriteria(scala.Long.MaxValue(),
                        scala.Long.MaxValue(), 0L, 0L));

                // Since we cleaned out the journal, we need to re-write the current election info.
                context.getTermInformation().updateAndPersist(context.getTermInformation().getCurrentTerm(),
                        context.getTermInformation().getVotedFor());
            }

            possiblyRestoreFromSnapshot();
        } else {
            boolean isServerConfigPayload = false;
            if(message instanceof ReplicatedLogEntry){
                ReplicatedLogEntry repLogEntry = (ReplicatedLogEntry)message;
                if(isServerConfigurationPayload(repLogEntry)){
                    isServerConfigPayload = true;
                    context.updatePeerIds((ServerConfigurationPayload)repLogEntry.getData());
                }
            }

            if(!isServerConfigPayload){
                dataRecoveredWithPersistenceDisabled = true;
            }
        }

        return recoveryComplete;
    }

    private void possiblyRestoreFromSnapshot() {
        byte[] restoreFromSnapshot = cohort.getRestoreFromSnapshot();
        if(restoreFromSnapshot == null) {
            return;
        }

        if(anyDataRecovered) {
            log.warn("{}: The provided restore snapshot was not applied because the persistence store is not empty",
                    context.getId());
            return;
        }

        try(ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(restoreFromSnapshot))) {
            Snapshot snapshot = (Snapshot) ois.readObject();

            log.debug("{}: Deserialized restore snapshot: {}", context.getId(), snapshot);

            context.getSnapshotManager().apply(new ApplySnapshot(snapshot));
        } catch(Exception e) {
            log.error("{}: Error deserializing snapshot restore", context.getId(), e);
        }
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

        context.setReplicatedLog(ReplicatedLogImpl.newInstance(snapshot, context));
        context.setLastApplied(snapshot.getLastAppliedIndex());
        context.setCommitIndex(snapshot.getLastAppliedIndex());
        context.getTermInformation().update(snapshot.getElectionTerm(), snapshot.getElectionVotedFor());

        Stopwatch timer = Stopwatch.createStarted();

        // Apply the snapshot to the actors state
        cohort.applyRecoverySnapshot(snapshot.getState());

        if (snapshot.getServerConfiguration() != null) {
            context.updatePeerIds(snapshot.getServerConfiguration());
        }

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

        if(isServerConfigurationPayload(logEntry)){
            context.updatePeerIds((ServerConfigurationPayload)logEntry.getData());
        }
        replicatedLog().append(logEntry);
    }

    private void onRecoveredApplyLogEntries(long toIndex) {
        long lastUnappliedIndex = context.getLastApplied() + 1;

        if(log.isDebugEnabled()) {
            // it can happen that lastUnappliedIndex > toIndex, if the AJE is in the persistent journal
            // but the entry itself has made it to that state and recovered via the snapshot
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
                log.error("{}: Log entry not found for index {}", context.getId(), i);
                break;
            }
        }

        context.setLastApplied(lastApplied);
        context.setCommitIndex(lastApplied);
    }

    private void batchRecoveredLogEntry(ReplicatedLogEntry logEntry) {
        initRecoveryTimer();

        int batchSize = context.getConfigParams().getJournalRecoveryLogBatchSize();
        if(!isServerConfigurationPayload(logEntry)){
            if(currentRecoveryBatchCount == 0) {
                cohort.startLogRecoveryBatch(batchSize);
            }

            cohort.appendRecoveredLogEntry(logEntry.getData());

            if(++currentRecoveryBatchCount >= batchSize) {
                endCurrentLogRecoveryBatch();
            }
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

    private static boolean isServerConfigurationPayload(ReplicatedLogEntry repLogEntry){
        return (repLogEntry.getData() instanceof ServerConfigurationPayload);
    }
}
