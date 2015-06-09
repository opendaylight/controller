/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.japi.Procedure;
import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import java.util.List;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.slf4j.Logger;

public class SnapshotManager implements SnapshotState {

    private final SnapshotState IDLE = new Idle();
    private final SnapshotState PERSISTING = new Persisting();
    private final SnapshotState CREATING = new Creating();

    private final Logger LOG;
    private final RaftActorContext context;
    private final LastAppliedTermInformationReader lastAppliedTermInformationReader =
            new LastAppliedTermInformationReader();
    private final ReplicatedToAllTermInformationReader replicatedToAllTermInformationReader =
            new ReplicatedToAllTermInformationReader();


    private SnapshotState currentState = IDLE;
    private CaptureSnapshot captureSnapshot;
    private long lastSequenceNumber = -1;

    private Procedure<Void> createSnapshotProcedure;

    private Snapshot applySnapshot;
    private Procedure<byte[]> applySnapshotProcedure;

    public SnapshotManager(RaftActorContext context, Logger logger) {
        this.context = context;
        this.LOG = logger;
    }

    @Override
    public boolean isCapturing() {
        return currentState.isCapturing();
    }

    @Override
    public boolean captureToInstall(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex, String targetFollower) {
        return currentState.captureToInstall(lastLogEntry, replicatedToAllIndex, targetFollower);
    }

    @Override
    public boolean capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex) {
        return currentState.capture(lastLogEntry, replicatedToAllIndex);
    }

    @Override
    public void apply(Snapshot snapshot) {
        currentState.apply(snapshot);
    }

    @Override
    public void persist(byte[] snapshotBytes, RaftActorBehavior currentBehavior, long totalMemory) {
        currentState.persist(snapshotBytes, currentBehavior, totalMemory);
    }

    @Override
    public void commit(long sequenceNumber, RaftActorBehavior currentBehavior) {
        currentState.commit(sequenceNumber, currentBehavior);
    }

    @Override
    public void rollback() {
        currentState.rollback();
    }

    @Override
    public long trimLog(long desiredTrimIndex, RaftActorBehavior currentBehavior) {
        return currentState.trimLog(desiredTrimIndex, currentBehavior);
    }

    public void setCreateSnapshotCallable(Procedure<Void> createSnapshotProcedure) {
        this.createSnapshotProcedure = createSnapshotProcedure;
    }

    public void setApplySnapshotProcedure(Procedure<byte[]> applySnapshotProcedure) {
        this.applySnapshotProcedure = applySnapshotProcedure;
    }

    public long getLastSequenceNumber() {
        return lastSequenceNumber;
    }

    @VisibleForTesting
    public CaptureSnapshot getCaptureSnapshot() {
        return captureSnapshot;
    }

    private boolean hasFollowers(){
        return context.getPeerAddresses().keySet().size() > 0;
    }

    private String persistenceId(){
        return context.getId();
    }

    private class AbstractSnapshotState implements SnapshotState {

        @Override
        public boolean isCapturing() {
            return false;
        }

        @Override
        public boolean capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex) {
            LOG.debug("capture should not be called in state {}", this);
            return false;
        }

        @Override
        public boolean captureToInstall(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex, String targetFollower) {
            LOG.debug("captureToInstall should not be called in state {}", this);
            return false;
        }

        @Override
        public void apply(Snapshot snapshot) {
            LOG.debug("apply should not be called in state {}", this);
        }

        @Override
        public void persist(byte[] snapshotBytes, RaftActorBehavior currentBehavior, long totalMemory) {
            LOG.debug("persist should not be called in state {}", this);
        }

        @Override
        public void commit(long sequenceNumber, RaftActorBehavior currentBehavior) {
            LOG.debug("commit should not be called in state {}", this);
        }

        @Override
        public void rollback() {
            LOG.debug("rollback should not be called in state {}", this);
        }

        @Override
        public long trimLog(long desiredTrimIndex, RaftActorBehavior currentBehavior) {
            LOG.debug("trimLog should not be called in state {}", this);
            return -1;
        }

        protected long doTrimLog(long desiredTrimIndex, RaftActorBehavior currentBehavior){
            //  we would want to keep the lastApplied as its used while capturing snapshots
            long lastApplied = context.getLastApplied();
            long tempMin = Math.min(desiredTrimIndex, (lastApplied > -1 ? lastApplied - 1 : -1));

            if(LOG.isTraceEnabled()) {
                LOG.trace("{}: performSnapshotWithoutCapture: desiredTrimIndex: {}, lastApplied: {}, tempMin: {}",
                        persistenceId(), desiredTrimIndex, lastApplied, tempMin);
            }

            if (tempMin > -1 && context.getReplicatedLog().isPresent(tempMin)) {
                LOG.debug("{}: fakeSnapshot purging log to {} for term {}", persistenceId(), tempMin,
                        context.getTermInformation().getCurrentTerm());

                //use the term of the temp-min, since we check for isPresent, entry will not be null
                ReplicatedLogEntry entry = context.getReplicatedLog().get(tempMin);
                context.getReplicatedLog().snapshotPreCommit(tempMin, entry.getTerm());
                context.getReplicatedLog().snapshotCommit();
                return tempMin;
            } else if(tempMin > currentBehavior.getReplicatedToAllIndex()) {
                // It's possible a follower was lagging and an install snapshot advanced its match index past
                // the current replicatedToAllIndex. Since the follower is now caught up we should advance the
                // replicatedToAllIndex (to tempMin). The fact that tempMin wasn't found in the log is likely
                // due to a previous snapshot triggered by the memory threshold exceeded, in that case we
                // trim the log to the last applied index even if previous entries weren't replicated to all followers.
                currentBehavior.setReplicatedToAllIndex(tempMin);
            }
            return -1;
        }
    }

    private class Idle extends AbstractSnapshotState {

        private boolean capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex, String targetFollower) {
            TermInformationReader lastAppliedTermInfoReader =
                    lastAppliedTermInformationReader.init(context.getReplicatedLog(), context.getLastApplied(),
                            lastLogEntry, hasFollowers());

            long lastAppliedIndex = lastAppliedTermInfoReader.getIndex();
            long lastAppliedTerm = lastAppliedTermInfoReader.getTerm();

            TermInformationReader replicatedToAllTermInfoReader =
                    replicatedToAllTermInformationReader.init(context.getReplicatedLog(), replicatedToAllIndex);

            long newReplicatedToAllIndex = replicatedToAllTermInfoReader.getIndex();
            long newReplicatedToAllTerm = replicatedToAllTermInfoReader.getTerm();

            // send a CaptureSnapshot to self to make the expensive operation async.

            List<ReplicatedLogEntry> unAppliedEntries = context.getReplicatedLog().getFrom(lastAppliedIndex + 1);

            captureSnapshot = new CaptureSnapshot(lastLogEntry.getIndex(),
                    lastLogEntry.getTerm(), lastAppliedIndex, lastAppliedTerm,
                    newReplicatedToAllIndex, newReplicatedToAllTerm, unAppliedEntries, targetFollower != null);

            if(captureSnapshot.isInstallSnapshotInitiated()) {
                LOG.info("{}: Initiating snapshot capture {} to install on {}",
                        persistenceId(), captureSnapshot, targetFollower);
            } else {
                LOG.info("{}: Initiating snapshot capture {}", persistenceId(), captureSnapshot);
            }

            lastSequenceNumber = context.getPersistenceProvider().getLastSequenceNumber();

            LOG.debug("lastSequenceNumber prior to capture: {}", lastSequenceNumber);

            SnapshotManager.this.currentState = CREATING;

            try {
                createSnapshotProcedure.apply(null);
            } catch (Exception e) {
                SnapshotManager.this.currentState = IDLE;
                LOG.error("Error creating snapshot", e);
                return false;
            }

            return true;
        }

        @Override
        public boolean capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex) {
            return capture(lastLogEntry, replicatedToAllIndex, null);
        }

        @Override
        public boolean captureToInstall(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex, String targetFollower) {
            return capture(lastLogEntry, replicatedToAllIndex, targetFollower);
        }

        @Override
        public void apply(Snapshot snapshot) {
            applySnapshot = snapshot;

            lastSequenceNumber = context.getPersistenceProvider().getLastSequenceNumber();

            LOG.debug("lastSequenceNumber prior to persisting applied snapshot: {}", lastSequenceNumber);

            context.getPersistenceProvider().saveSnapshot(snapshot);

            SnapshotManager.this.currentState = PERSISTING;
        }

        @Override
        public String toString() {
            return "Idle";
        }

        @Override
        public long trimLog(long desiredTrimIndex, RaftActorBehavior currentBehavior) {
            return doTrimLog(desiredTrimIndex, currentBehavior);
        }
    }

    private class Creating extends AbstractSnapshotState {

        @Override
        public boolean isCapturing() {
            return true;
        }

        @Override
        public void persist(byte[] snapshotBytes, RaftActorBehavior currentBehavior, long totalMemory) {
            // create a snapshot object from the state provided and save it
            // when snapshot is saved async, SaveSnapshotSuccess is raised.

            Snapshot sn = Snapshot.create(snapshotBytes,
                    captureSnapshot.getUnAppliedEntries(),
                    captureSnapshot.getLastIndex(), captureSnapshot.getLastTerm(),
                    captureSnapshot.getLastAppliedIndex(), captureSnapshot.getLastAppliedTerm());

            context.getPersistenceProvider().saveSnapshot(sn);

            LOG.info("{}: Persisting of snapshot done:{}", persistenceId(), sn.getLogMessage());

            long dataThreshold = totalMemory *
                    context.getConfigParams().getSnapshotDataThresholdPercentage() / 100;
            if (context.getReplicatedLog().dataSize() > dataThreshold) {

                if(LOG.isDebugEnabled()) {
                    LOG.debug("{}: dataSize {} exceeds dataThreshold {} - doing snapshotPreCommit with index {}",
                            persistenceId(), context.getReplicatedLog().dataSize(), dataThreshold,
                            captureSnapshot.getLastAppliedIndex());
                }

                // if memory is less, clear the log based on lastApplied.
                // this could/should only happen if one of the followers is down
                // as normally we keep removing from the log when its replicated to all.
                context.getReplicatedLog().snapshotPreCommit(captureSnapshot.getLastAppliedIndex(),
                        captureSnapshot.getLastAppliedTerm());

                // Don't reset replicatedToAllIndex to -1 as this may prevent us from trimming the log after an
                // install snapshot to a follower.
                if(captureSnapshot.getReplicatedToAllIndex() >= 0) {
                    currentBehavior.setReplicatedToAllIndex(captureSnapshot.getReplicatedToAllIndex());
                }

            } else if(captureSnapshot.getReplicatedToAllIndex() != -1){
                // clear the log based on replicatedToAllIndex
                context.getReplicatedLog().snapshotPreCommit(captureSnapshot.getReplicatedToAllIndex(),
                        captureSnapshot.getReplicatedToAllTerm());

                currentBehavior.setReplicatedToAllIndex(captureSnapshot.getReplicatedToAllIndex());
            } else {
                // The replicatedToAllIndex was not found in the log
                // This means that replicatedToAllIndex never moved beyond -1 or that it is already in the snapshot.
                // In this scenario we may need to save the snapshot to the akka persistence
                // snapshot for recovery but we do not need to do the replicated log trimming.
                context.getReplicatedLog().snapshotPreCommit(context.getReplicatedLog().getSnapshotIndex(),
                        context.getReplicatedLog().getSnapshotTerm());
            }

            LOG.info("{}: Removed in-memory snapshotted entries, adjusted snaphsotIndex:{} " +
                            "and term:{}", persistenceId(), captureSnapshot.getLastAppliedIndex(),
                    captureSnapshot.getLastAppliedTerm());

            if (context.getId().equals(currentBehavior.getLeaderId())
                    && captureSnapshot.isInstallSnapshotInitiated()) {
                // this would be call straight to the leader and won't initiate in serialization
                currentBehavior.handleMessage(context.getActor(), new SendInstallSnapshot(
                        ByteString.copyFrom(snapshotBytes)));
            }

            captureSnapshot = null;
            SnapshotManager.this.currentState = PERSISTING;
        }

        @Override
        public String toString() {
            return "Creating";
        }

    }

    private class Persisting extends AbstractSnapshotState {

        @Override
        public void commit(long sequenceNumber, RaftActorBehavior currentBehavior) {
            LOG.debug("Snapshot success sequence number:", sequenceNumber);

            if(applySnapshot != null) {
                try {
                    applySnapshotProcedure.apply(applySnapshot.getState());

                    //clears the followers log, sets the snapshot index to ensure adjusted-index works
                    context.setReplicatedLog(ReplicatedLogImpl.newInstance(applySnapshot, context, currentBehavior));
                    context.setLastApplied(applySnapshot.getLastAppliedIndex());
                    context.setCommitIndex(applySnapshot.getLastAppliedIndex());
                } catch (Exception e) {
                    LOG.error("Error applying snapshot", e);
                }
            } else {
                context.getReplicatedLog().snapshotCommit();
            }

            context.getPersistenceProvider().deleteSnapshots(new SnapshotSelectionCriteria(
                    sequenceNumber - context.getConfigParams().getSnapshotBatchCount(), 43200000));

            context.getPersistenceProvider().deleteMessages(lastSequenceNumber);

            lastSequenceNumber = -1;
            applySnapshot = null;
            SnapshotManager.this.currentState = IDLE;
        }

        @Override
        public void rollback() {
            // Nothing to rollback if we're applying a snapshot from the leader.
            if(applySnapshot == null) {
                context.getReplicatedLog().snapshotRollback();

                LOG.info("{}: Replicated Log rolled back. Snapshot will be attempted in the next cycle." +
                        "snapshotIndex:{}, snapshotTerm:{}, log-size:{}", persistenceId(),
                        context.getReplicatedLog().getSnapshotIndex(),
                        context.getReplicatedLog().getSnapshotTerm(),
                        context.getReplicatedLog().size());
            }

            lastSequenceNumber = -1;
            applySnapshot = null;
            SnapshotManager.this.currentState = IDLE;
        }

        @Override
        public String toString() {
            return "Persisting";
        }

    }

    private static interface TermInformationReader {
        long getIndex();
        long getTerm();
    }

    static class LastAppliedTermInformationReader implements TermInformationReader{
        private long index;
        private long term;

        public LastAppliedTermInformationReader init(ReplicatedLog log, long originalIndex,
                                         ReplicatedLogEntry lastLogEntry, boolean hasFollowers){
            ReplicatedLogEntry entry = log.get(originalIndex);
            this.index = -1L;
            this.term = -1L;
            if (!hasFollowers) {
                if(lastLogEntry != null) {
                    // since we have persisted the last-log-entry to persistent journal before the capture,
                    // we would want to snapshot from this entry.
                    index = lastLogEntry.getIndex();
                    term = lastLogEntry.getTerm();
                }
            } else if (entry != null) {
                index = entry.getIndex();
                term = entry.getTerm();
            } else if(log.getSnapshotIndex() > -1){
                index = log.getSnapshotIndex();
                term = log.getSnapshotTerm();
            }
            return this;
        }

        @Override
        public long getIndex(){
            return this.index;
        }

        @Override
        public long getTerm(){
            return this.term;
        }
    }

    private static class ReplicatedToAllTermInformationReader implements TermInformationReader{
        private long index;
        private long term;

        ReplicatedToAllTermInformationReader init(ReplicatedLog log, long originalIndex){
            ReplicatedLogEntry entry = log.get(originalIndex);
            this.index = -1L;
            this.term = -1L;

            if (entry != null) {
                index = entry.getIndex();
                term = entry.getTerm();
            }

            return this;
        }

        @Override
        public long getIndex(){
            return this.index;
        }

        @Override
        public long getTerm(){
            return this.term;
        }
    }
}
