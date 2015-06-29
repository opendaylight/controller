/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.japi.Procedure;
import akka.persistence.SnapshotSelectionCriteria;
import com.google.protobuf.ByteString;
import java.util.List;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.slf4j.Logger;

/**
 * Implementation of SnapshotSupport.
 *
 * @author Thomas Pantelis
 */
class SnapshotSupportImpl implements SnapshotSupport {
    private final Procedure<Void> createSnapshotProcedure;
    private final DataPersistenceProvider persistenceProvider;
    private final RaftActorContext context;
    private final Logger log;
    private CaptureSnapshot currentCaptureSnapshot;

    SnapshotSupportImpl(RaftActorContext context, DataPersistenceProvider persistenceProvider,
            Procedure<Void> createSnapshotProcedure, Logger log) {
        this.createSnapshotProcedure = createSnapshotProcedure;
        this.persistenceProvider = persistenceProvider;
        this.context = context;
        this.log = log;
    }

    @Override
    public void capture(long lastAppliedTerm, long lastAppliedIndex, long replicatedToAllIndex,
            boolean isInstallSnapshotInitiated) {

        List<ReplicatedLogEntry> unAppliedEntries = context.getReplicatedLog().getFrom(lastAppliedIndex + 1);

        long lastSequenceNumber = persistenceProvider.getLastSequenceNumber();

        log.debug("lastSequenceNumber prior to capture: {}", lastSequenceNumber);

        ReplicatedLogEntry replicatedToAllEntry = context.getReplicatedLog().get(replicatedToAllIndex);

        currentCaptureSnapshot = new CaptureSnapshot(context.getReplicatedLog().lastIndex(),
                context.getReplicatedLog().lastTerm(), lastAppliedIndex, lastAppliedTerm,
                (replicatedToAllEntry != null ? replicatedToAllEntry.getIndex() : -1),
                (replicatedToAllEntry != null ? replicatedToAllEntry.getTerm() : -1),
                unAppliedEntries, lastSequenceNumber, isInstallSnapshotInitiated);

        context.setSnapshotCaptureInitiated(true);

        try {
            createSnapshotProcedure.apply(null);
        } catch (Exception e) {
            log.error("Error creating snapshot", e);
            context.setSnapshotCaptureInitiated(false);
            currentCaptureSnapshot = null;
        }
    }

    @Override
    public void rollback() {
        context.getReplicatedLog().snapshotRollback();

        log.info("{}: Replicated Log rollbacked. Snapshot will be attempted in the next cycle." +
            "snapshotIndex:{}, snapshotTerm:{}, log-size:{}", context.getId(),
            context.getReplicatedLog().getSnapshotIndex(),
            context.getReplicatedLog().getSnapshotTerm(),
            context.getReplicatedLog().size());

        currentCaptureSnapshot = null;
    }

    @Override
    public void commit(long sequenceNumber) {
        context.getReplicatedLog().snapshotCommit();

        // Trim akka snapshots
        // FIXME : Not sure how exactly the SnapshotSelectionCriteria is applied
        // For now guessing that it is ANDed.
        persistenceProvider.deleteSnapshots(new SnapshotSelectionCriteria(
            sequenceNumber - context.getConfigParams().getSnapshotBatchCount(), 43200000));

        // Trim akka journal
        if(currentCaptureSnapshot != null) {
            persistenceProvider.deleteMessages(currentCaptureSnapshot.getLastSequenceNumber());
            currentCaptureSnapshot = null;
        }
    }

    @Override
    public void persist(ByteString stateInBytes, RaftActorBehavior behavior, boolean isLeader) {
        // create a snapshot object from the state provided and save it
        // when snapshot is saved async, SaveSnapshotSuccess is raised.

        Snapshot sn = Snapshot.create(stateInBytes.toByteArray(),
            currentCaptureSnapshot.getUnAppliedEntries(),
            currentCaptureSnapshot.getLastIndex(), currentCaptureSnapshot.getLastTerm(),
            currentCaptureSnapshot.getLastAppliedIndex(), currentCaptureSnapshot.getLastAppliedTerm());

        persistenceProvider.saveSnapshot(sn);

        log.info("{}: Persisting of snapshot done:{}", context.getId(), sn.getLogMessage());

        long dataThreshold = Runtime.getRuntime().totalMemory() *
                context.getConfigParams().getSnapshotDataThresholdPercentage() / 100;
        if (context.getReplicatedLog().dataSize() > dataThreshold) {
            if(log.isDebugEnabled()) {
                log.debug("{}: dataSize {} exceeds dataThreshold {} - doing snapshotPreCommit with index {}",
                        context.getId(), context.getReplicatedLog().dataSize(), dataThreshold,
                        currentCaptureSnapshot.getLastAppliedIndex());
            }

            // if memory is less, clear the log based on lastApplied.
            // this could/should only happen if one of the followers is down
            // as normally we keep removing from the log when its replicated to all.
            context.getReplicatedLog().snapshotPreCommit(currentCaptureSnapshot.getLastAppliedIndex(),
                    currentCaptureSnapshot.getLastAppliedTerm());

            // Don't reset replicatedToAllIndex to -1 as this may prevent us from trimming the log after an
            // install snapshot to a follower.
            if(currentCaptureSnapshot.getReplicatedToAllIndex() >= 0) {
                behavior.setReplicatedToAllIndex(currentCaptureSnapshot.getReplicatedToAllIndex());
            }

        } else if(currentCaptureSnapshot.getReplicatedToAllIndex() != -1){
            // clear the log based on replicatedToAllIndex
            context.getReplicatedLog().snapshotPreCommit(currentCaptureSnapshot.getReplicatedToAllIndex(),
                    currentCaptureSnapshot.getReplicatedToAllTerm());

            behavior.setReplicatedToAllIndex(currentCaptureSnapshot.getReplicatedToAllIndex());
        } else {
            // The replicatedToAllIndex was not found in the log
            // This means that replicatedToAllIndex never moved beyond -1 or that it is already in the snapshot.
            // In this scenario we may need to save the snapshot to the akka persistence
            // snapshot for recovery but we do not need to do the replicated log trimming.
            context.getReplicatedLog().snapshotPreCommit(context.getReplicatedLog().getSnapshotIndex(),
                    context.getReplicatedLog().getSnapshotTerm());
        }

        log.info("{}: Removed in-memory snapshotted entries, adjusted snaphsotIndex:{} " +
            "and term:{}", context.getId(), currentCaptureSnapshot.getLastAppliedIndex(),
            currentCaptureSnapshot.getLastAppliedTerm());

        if (isLeader && currentCaptureSnapshot.isInstallSnapshotInitiated()) {
            // this would be call straight to the leader and won't initiate in serialization
            behavior.handleMessage(context.getActor(), new SendInstallSnapshot(stateInBytes));
        }

        context.setSnapshotCaptureInitiated(false);
    }
}
