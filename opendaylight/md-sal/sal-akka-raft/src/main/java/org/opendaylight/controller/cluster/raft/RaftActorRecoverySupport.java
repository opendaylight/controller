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
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import org.opendaylight.controller.cluster.PersistentDataProvider;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.MigratedSerializable;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.PersistentPayload;
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
    private final Logger log;

    RaftActorRecoverySupport(final RaftActorContext context, final RaftActorRecoveryCohort cohort) {
        this.context = context;
        this.cohort = cohort;
        this.log = context.getLogger();
    }

    boolean handleRecoveryMessage(Object message, PersistentDataProvider persistentProvider) {
        log.trace("{}: handleRecoveryMessage: {}", context.getId(), message);

        anyDataRecovered = anyDataRecovered || !(message instanceof RecoveryCompleted);

        if(isMigratedSerializable(message)) {
            hasMigratedDataRecovered = true;
        }

        boolean recoveryComplete = false;
        if (message instanceof UpdateElectionTerm) {
            context.getTermInformation().update(((UpdateElectionTerm) message).getCurrentTerm(),
                    ((UpdateElectionTerm) message).getVotedFor());
        } else if (message instanceof SnapshotOffer) {
            onRecoveredSnapshot((SnapshotOffer) message);
        } else if (message instanceof ReplicatedLogEntry) {
            onRecoveredJournalLogEntry((ReplicatedLogEntry) message);
        } else if (message instanceof ApplyJournalEntries) {
            onRecoveredApplyLogEntries(((ApplyJournalEntries) message).getToIndex());
        } else if (message instanceof DeleteEntries) {
            onDeleteEntries((DeleteEntries) message);
        } else if (message instanceof RecoveryCompleted) {
            recoveryComplete = true;
            onRecoveryCompletedMessage(persistentProvider);
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

        for(ReplicatedLogEntry entry: snapshot.getUnAppliedEntries()) {
            if(isMigratedPayload(entry)) {
                hasMigratedDataRecovered = true;
            }
        }

        if(!context.getPersistenceProvider().isRecoveryApplicable()) {
            // We may have just transitioned to disabled and have a snapshot containing state data and/or log
            // entries - we don't want to preserve these, only the server config and election term info.

            snapshot = Snapshot.create(new byte[0], Collections.emptyList(), -1, -1, -1, -1,
                    snapshot.getElectionTerm(), snapshot.getElectionVotedFor(), snapshot.getServerConfiguration());
        }

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

            if(isMigratedSerializable(snapshot.getServerConfiguration())) {
                hasMigratedDataRecovered = true;
            }
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

        if(isMigratedPayload(logEntry)) {
            hasMigratedDataRecovered = true;
        }

        if(context.getPersistenceProvider().isRecoveryApplicable()) {
            replicatedLog().append(logEntry);
        } else if(!isPersistentPayload(logEntry)) {
            dataRecoveredWithPersistenceDisabled = true;
        }
    }

    private void onRecoveredApplyLogEntries(long toIndex) {
        if(!context.getPersistenceProvider().isRecoveryApplicable()) {
            dataRecoveredWithPersistenceDisabled = true;
            return;
        }

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

    private void onDeleteEntries(DeleteEntries deleteEntries) {
        if(context.getPersistenceProvider().isRecoveryApplicable()) {
            replicatedLog().removeFrom(deleteEntries.getFromIndex());
        } else {
            dataRecoveredWithPersistenceDisabled = true;
        }
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

    private void onRecoveryCompletedMessage(PersistentDataProvider persistentProvider) {
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

        if(dataRecoveredWithPersistenceDisabled ||
                (hasMigratedDataRecovered && !context.getPersistenceProvider().isRecoveryApplicable())) {
            if(hasMigratedDataRecovered) {
                log.info("{}: Saving snapshot after recovery due to migrated messages", context.getId());
            } else {
                log.info("{}: Saving snapshot after recovery due to data persistence disabled", context.getId());
            }

            // Either data persistence is disabled and we recovered some data entries (ie we must have just
            // transitioned to disabled or a persistence backup was restored) or we recovered migrated
            // messages. Either way, we persist a snapshot and delete all the messages from the akka journal
            // to clean out unwanted messages.

            Snapshot snapshot = Snapshot.create(new byte[0], Collections.<ReplicatedLogEntry>emptyList(), -1, -1, -1, -1,
                    context.getTermInformation().getCurrentTerm(), context.getTermInformation().getVotedFor(),
                    context.getPeerServerInfo(true));

            persistentProvider.saveSnapshot(snapshot);

            persistentProvider.deleteMessages(persistentProvider.getLastSequenceNumber());
        } else if(hasMigratedDataRecovered) {
            log.info("{}: Snapshot capture initiated after recovery due to migrated messages", context.getId());

            context.getSnapshotManager().capture(replicatedLog().last(), -1);
        } else {
            possiblyRestoreFromSnapshot();
        }
    }

    private static boolean isServerConfigurationPayload(ReplicatedLogEntry repLogEntry){
        return repLogEntry.getData() instanceof ServerConfigurationPayload;
    }

    private static boolean isPersistentPayload(ReplicatedLogEntry repLogEntry){
        return repLogEntry.getData() instanceof PersistentPayload;
    }

    private static boolean isMigratedPayload(ReplicatedLogEntry repLogEntry){
        return isMigratedSerializable(repLogEntry.getData());
    }

    private static boolean isMigratedSerializable(Object message){
        return message instanceof MigratedSerializable && ((MigratedSerializable)message).isMigrated();
    }
}
