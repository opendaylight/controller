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
import java.util.List;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
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

    private ApplySnapshot applySnapshot;
    private Procedure<byte[]> applySnapshotProcedure;

    public SnapshotManager(RaftActorContext context, Logger logger) {
        this.context = context;
        this.LOG = logger;
    }

    public boolean isApplying() {
        return applySnapshot != null;
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
    public void apply(ApplySnapshot snapshot) {
        currentState.apply(snapshot);
    }

    @Override
    public void persist(final byte[] snapshotBytes, final long totalMemory) {
        currentState.persist(snapshotBytes, totalMemory);
    }

    @Override
    public void commit(final long sequenceNumber) {
        currentState.commit(sequenceNumber);
    }

    @Override
    public void rollback() {
        currentState.rollback();
    }

    @Override
    public long trimLog(final long desiredTrimIndex) {
        return currentState.trimLog(desiredTrimIndex);
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
        return context.hasFollowers();
    }

    private String persistenceId(){
        return context.getId();
    }

    public CaptureSnapshot newCaptureSnapshot(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex,
            boolean installSnapshotInitiated) {
        TermInformationReader lastAppliedTermInfoReader =
                lastAppliedTermInformationReader.init(context.getReplicatedLog(), context.getLastApplied(),
                        lastLogEntry, hasFollowers());

        long lastAppliedIndex = lastAppliedTermInfoReader.getIndex();
        long lastAppliedTerm = lastAppliedTermInfoReader.getTerm();

        TermInformationReader replicatedToAllTermInfoReader =
                replicatedToAllTermInformationReader.init(context.getReplicatedLog(), replicatedToAllIndex);

        long newReplicatedToAllIndex = replicatedToAllTermInfoReader.getIndex();
        long newReplicatedToAllTerm = replicatedToAllTermInfoReader.getTerm();

        List<ReplicatedLogEntry> unAppliedEntries = context.getReplicatedLog().getFrom(lastAppliedIndex + 1);

        long lastLogEntryIndex = lastAppliedIndex;
        long lastLogEntryTerm = lastAppliedTerm;
        if(lastLogEntry != null) {
            lastLogEntryIndex = lastLogEntry.getIndex();
            lastLogEntryTerm = lastLogEntry.getTerm();
        } else {
            LOG.debug("{}: Capturing Snapshot : lastLogEntry is null. Using lastAppliedIndex {} and lastAppliedTerm {} instead.",
                    persistenceId(), lastAppliedIndex, lastAppliedTerm);
        }

        return new CaptureSnapshot(lastLogEntryIndex, lastLogEntryTerm, lastAppliedIndex, lastAppliedTerm,
                newReplicatedToAllIndex, newReplicatedToAllTerm, unAppliedEntries, installSnapshotInitiated);
    }

    private class AbstractSnapshotState implements SnapshotState {

        @Override
        public boolean isCapturing() {
            return true;
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
        public void apply(ApplySnapshot snapshot) {
            LOG.debug("apply should not be called in state {}", this);
        }

        @Override
        public void persist(final byte[] snapshotBytes, final long totalMemory) {
            LOG.debug("persist should not be called in state {}", this);
        }

        @Override
        public void commit(final long sequenceNumber) {
            LOG.debug("commit should not be called in state {}", this);
        }

        @Override
        public void rollback() {
            LOG.debug("rollback should not be called in state {}", this);
        }

        @Override
        public long trimLog(final long desiredTrimIndex) {
            LOG.debug("trimLog should not be called in state {}", this);
            return -1;
        }

        protected long doTrimLog(final long desiredTrimIndex) {
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
            }

            final RaftActorBehavior currentBehavior = context.getCurrentBehavior();
            if(tempMin > currentBehavior.getReplicatedToAllIndex()) {
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

        @Override
        public boolean isCapturing() {
            return false;
        }

        private boolean capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex, String targetFollower) {
            captureSnapshot = newCaptureSnapshot(lastLogEntry, replicatedToAllIndex, targetFollower != null);

            if(captureSnapshot.isInstallSnapshotInitiated()) {
                LOG.info("{}: Initiating snapshot capture {} to install on {}",
                        persistenceId(), captureSnapshot, targetFollower);
            } else {
                LOG.info("{}: Initiating snapshot capture {}", persistenceId(), captureSnapshot);
            }

            lastSequenceNumber = context.getPersistenceProvider().getLastSequenceNumber();

            LOG.debug("{}: lastSequenceNumber prior to capture: {}", persistenceId(), lastSequenceNumber);

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
        public void apply(ApplySnapshot applySnapshot) {
            SnapshotManager.this.applySnapshot = applySnapshot;

            lastSequenceNumber = context.getPersistenceProvider().getLastSequenceNumber();

            LOG.debug("lastSequenceNumber prior to persisting applied snapshot: {}", lastSequenceNumber);

            context.getPersistenceProvider().saveSnapshot(applySnapshot.getSnapshot());

            SnapshotManager.this.currentState = PERSISTING;
        }

        @Override
        public String toString() {
            return "Idle";
        }

        @Override
        public long trimLog(final long desiredTrimIndex) {
            return doTrimLog(desiredTrimIndex);
        }
    }

    private class Creating extends AbstractSnapshotState {

        @Override
        public void persist(final byte[] snapshotBytes, final long totalMemory) {
            // create a snapshot object from the state provided and save it
            // when snapshot is saved async, SaveSnapshotSuccess is raised.

            Snapshot snapshot = Snapshot.create(snapshotBytes,
                    captureSnapshot.getUnAppliedEntries(),
                    captureSnapshot.getLastIndex(), captureSnapshot.getLastTerm(),
                    captureSnapshot.getLastAppliedIndex(), captureSnapshot.getLastAppliedTerm(),
                    context.getTermInformation().getCurrentTerm(),
                    context.getTermInformation().getVotedFor(), context.getPeerServerInfo(true));

            context.getPersistenceProvider().saveSnapshot(snapshot);

            LOG.info("{}: Persisting of snapshot done: {}", persistenceId(), snapshot);

            long dataThreshold = totalMemory *
                    context.getConfigParams().getSnapshotDataThresholdPercentage() / 100;
            boolean dataSizeThresholdExceeded = context.getReplicatedLog().dataSize() > dataThreshold;

            boolean logSizeExceededSnapshotBatchCount =
                    context.getReplicatedLog().size() >= context.getConfigParams().getSnapshotBatchCount();

            final RaftActorBehavior currentBehavior = context.getCurrentBehavior();
            if (dataSizeThresholdExceeded || logSizeExceededSnapshotBatchCount) {
                if(LOG.isDebugEnabled()) {
                    if(dataSizeThresholdExceeded) {
                        LOG.debug("{}: log data size {} exceeds the memory threshold {} - doing snapshotPreCommit with index {}",
                                context.getId(), context.getReplicatedLog().dataSize(), dataThreshold,
                                captureSnapshot.getLastAppliedIndex());
                    } else {
                        LOG.debug("{}: log size {} exceeds the snapshot batch count {} - doing snapshotPreCommit with index {}",
                                context.getId(), context.getReplicatedLog().size(),
                                context.getConfigParams().getSnapshotBatchCount(), captureSnapshot.getLastAppliedIndex());
                    }
                }

                // We either exceeded the memory threshold or the log size exceeded the snapshot batch
                // count so, to keep the log memory footprint in check, clear the log based on lastApplied.
                // This could/should only happen if one of the followers is down as normally we keep
                // removing from the log as entries are replicated to all.
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

            LOG.info("{}: Removed in-memory snapshotted entries, adjusted snaphsotIndex: {} " +
                    "and term: {}", context.getId(), context.getReplicatedLog().getSnapshotIndex(),
                    context.getReplicatedLog().getSnapshotTerm());

            if (context.getId().equals(currentBehavior.getLeaderId())
                    && captureSnapshot.isInstallSnapshotInitiated()) {
                // this would be call straight to the leader and won't initiate in serialization
                currentBehavior.handleMessage(context.getActor(), new SendInstallSnapshot(snapshot));
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
        public void commit(final long sequenceNumber) {
            LOG.debug("{}: Snapshot success -  sequence number: {}", persistenceId(), sequenceNumber);

            if(applySnapshot != null) {
                try {
                    Snapshot snapshot = applySnapshot.getSnapshot();

                    //clears the followers log, sets the snapshot index to ensure adjusted-index works
                    context.setReplicatedLog(ReplicatedLogImpl.newInstance(snapshot, context));
                    context.setLastApplied(snapshot.getLastAppliedIndex());
                    context.setCommitIndex(snapshot.getLastAppliedIndex());
                    context.getTermInformation().update(snapshot.getElectionTerm(), snapshot.getElectionVotedFor());

                    if(snapshot.getState().length > 0 ) {
                        applySnapshotProcedure.apply(snapshot.getState());
                    }

                    applySnapshot.getCallback().onSuccess();
                } catch (Exception e) {
                    LOG.error("{}: Error applying snapshot", context.getId(), e);
                }
            } else {
                context.getReplicatedLog().snapshotCommit();
            }

            context.getPersistenceProvider().deleteSnapshots(new SnapshotSelectionCriteria(
                    sequenceNumber - context.getConfigParams().getSnapshotBatchCount(), Long.MAX_VALUE, 0L, 0L));

            context.getPersistenceProvider().deleteMessages(lastSequenceNumber);

            snapshotComplete();
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
            } else {
                applySnapshot.getCallback().onFailure();
            }

            snapshotComplete();
        }

        private void snapshotComplete() {
            lastSequenceNumber = -1;
            applySnapshot = null;
            SnapshotManager.this.currentState = IDLE;

            context.getActor().tell(SnapshotComplete.INSTANCE, context.getActor());
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
