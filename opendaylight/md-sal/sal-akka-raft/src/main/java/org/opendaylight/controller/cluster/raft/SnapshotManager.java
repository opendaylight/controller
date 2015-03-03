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
import com.google.protobuf.ByteString;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.SnapshotState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnapshotManager implements SnapshotState {

    private static final Logger LOG = LoggerFactory.getLogger(SnapshotManager.class);

    private final SnapshotState IDLE = new Idle();
    private final SnapshotState CAPTURING = new Capturing();
    private final SnapshotState PERSISTING = new Persisting();
    private final SnapshotState CREATING = new Creating();
    private final RaftActorContext context;
    private final LastAppliedTermInformationReader lastAppliedTermInformationReader =
            new LastAppliedTermInformationReader();
    private final ReplicatedToAllTermInformationReader replicatedToAllTermInformationReader =
            new ReplicatedToAllTermInformationReader();


    private SnapshotState currentState = IDLE;
    private CaptureSnapshot captureSnapshot;

    public SnapshotManager(RaftActorContext context) {
        this.context = context;
    }

    @Override
    public boolean isCapturing() {
        return currentState.isCapturing();
    }

    @Override
    public void captureToInstall(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex) {
        currentState.captureToInstall(lastLogEntry, replicatedToAllIndex);
    }

    @Override
    public void capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex) {
        currentState.capture(lastLogEntry, replicatedToAllIndex);
    }

    @Override
    public void create(Procedure<Void> callback) {
        currentState.create(callback);
    }

    @Override
    public void persist(DataPersistenceProvider persistenceProvider, byte[] snapshotBytes, RaftActorBehavior currentBehavior) {
        currentState.persist(persistenceProvider, snapshotBytes, currentBehavior);
    }

    @Override
    public void commit(DataPersistenceProvider persistenceProvider, long sequenceNumber) {
        currentState.commit(persistenceProvider, sequenceNumber);
    }

    @Override
    public void rollback() {
        currentState.rollback();
    }

    @Override
    public long trimLog(long desiredTrimIndex) {
        return currentState.trimLog(desiredTrimIndex);
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
        public void capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex) {
            LOG.debug("capture should not be called in state {}", this);
        }

        @Override
        public void captureToInstall(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex) {
            LOG.debug("captureToInstall should not be called in state {}", this);
        }

        @Override
        public void create(Procedure<Void> callback) {
            LOG.debug("create should not be called in state {}", this);
        }

        @Override
        public void persist(DataPersistenceProvider persistenceProvider, byte[] snapshotBytes, RaftActorBehavior currentBehavior) {
            LOG.debug("persist should not be called in state {}", this);
        }

        @Override
        public void commit(DataPersistenceProvider persistenceProvider, long sequenceNumber) {
            LOG.debug("commit should not be called in state {}", this);
        }

        @Override
        public void rollback() {
            LOG.debug("rollback should not be called in state {}", this);
        }

        @Override
        public long trimLog(long desiredTrimIndex) {
            LOG.debug("trimLog should not be called in state {}", this);
            return -1;
        }

        protected long doTrimLog(long desiredTrimIndex){
            //  we would want to keep the lastApplied as its used while capturing snapshots
            long lastApplied = context.getLastApplied();
            long tempMin = Math.min(desiredTrimIndex, (lastApplied > -1 ? lastApplied - 1 : -1));

            if (tempMin > -1 && context.getReplicatedLog().isPresent(tempMin))  {
                LOG.debug("{}: fakeSnapshot purging log to {} for term {}", persistenceId(), tempMin,
                        context.getTermInformation().getCurrentTerm());

                //use the term of the temp-min, since we check for isPresent, entry will not be null
                ReplicatedLogEntry entry = context.getReplicatedLog().get(tempMin);
                context.getReplicatedLog().snapshotPreCommit(tempMin, entry.getTerm());
                context.getReplicatedLog().snapshotCommit();
                return tempMin;
            }

            return -1;
        }
    }

    private class Idle extends AbstractSnapshotState {

        private void capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex, boolean toInstall) {
            TermInformationReader lastAppliedTermInfoReader =
                    lastAppliedTermInformationReader.init(context.getReplicatedLog(), context.getLastApplied(),
                            lastLogEntry, hasFollowers());

            long lastAppliedIndex = lastAppliedTermInfoReader.getIndex();
            long lastAppliedTerm = lastAppliedTermInfoReader.getTerm();

            if (LOG.isDebugEnabled()) {
                LOG.debug("{}: Snapshot Capture logSize: {}", persistenceId(), context.getReplicatedLog().size());
                LOG.debug("{}: Snapshot Capture lastApplied:{} ", persistenceId(), context.getLastApplied());
                LOG.debug("{}: Snapshot Capture lastAppliedIndex:{}", persistenceId(), lastAppliedIndex);
                LOG.debug("{}: Snapshot Capture lastAppliedTerm:{}", persistenceId(), lastAppliedTerm);
            }

            TermInformationReader replicatedToAllTermInfoReader =
                    replicatedToAllTermInformationReader.init(context.getReplicatedLog(), replicatedToAllIndex);

            long newReplicatedToAllIndex = replicatedToAllTermInfoReader.getIndex();
            long newReplicatedToAllTerm = replicatedToAllTermInfoReader.getTerm();

            // send a CaptureSnapshot to self to make the expensive operation async.
            captureSnapshot = new CaptureSnapshot(lastLogEntry.getIndex(),
                    lastLogEntry.getTerm(), lastAppliedIndex, lastAppliedTerm,
                    newReplicatedToAllIndex, newReplicatedToAllTerm, toInstall);

            SnapshotManager.this.currentState = CAPTURING;

            context.getActor().tell(captureSnapshot, context.getActor());
        }

        @Override
        public void capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex) {
            capture(lastLogEntry, replicatedToAllIndex, false);
        }

        @Override
        public void captureToInstall(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex) {
            capture(lastLogEntry, replicatedToAllIndex, true);
        }

        @Override
        public String toString() {
            return "Idle";
        }

        @Override
        public long trimLog(long desiredTrimIndex) {
            return doTrimLog(desiredTrimIndex);
        }
    }

    private class Capturing extends AbstractSnapshotState {

        @Override
        public boolean isCapturing() {
            return true;
        }

        @Override
        public void create(Procedure<Void> callback) {
            try {
                callback.apply(null);
                SnapshotManager.this.currentState = CREATING;
            } catch (Exception e) {
                LOG.error("Unexpected error occurred", e);
            }
        }

        @Override
        public String toString() {
            return "Capturing";
        }

    }

    private class Creating extends AbstractSnapshotState {
        @Override
        public void persist(DataPersistenceProvider persistenceProvider, byte[] snapshotBytes,
                            RaftActorBehavior currentBehavior) {
            // create a snapshot object from the state provided and save it
            // when snapshot is saved async, SaveSnapshotSuccess is raised.

            Snapshot sn = Snapshot.create(snapshotBytes,
                    context.getReplicatedLog().getFrom(captureSnapshot.getLastAppliedIndex() + 1),
                    captureSnapshot.getLastIndex(), captureSnapshot.getLastTerm(),
                    captureSnapshot.getLastAppliedIndex(), captureSnapshot.getLastAppliedTerm());

            persistenceProvider.saveSnapshot(sn);

            LOG.info("{}: Persisting of snapshot done:{}", persistenceId(), sn.getLogMessage());

            long dataThreshold = Runtime.getRuntime().totalMemory() *
                    context.getConfigParams().getSnapshotDataThresholdPercentage() / 100;
            if (context.getReplicatedLog().dataSize() > dataThreshold) {
                // if memory is less, clear the log based on lastApplied.
                // this could/should only happen if one of the followers is down
                // as normally we keep removing from the log when its replicated to all.
                context.getReplicatedLog().snapshotPreCommit(captureSnapshot.getLastAppliedIndex(),
                        captureSnapshot.getLastAppliedTerm());

                currentBehavior.setReplicatedToAllIndex(captureSnapshot.getReplicatedToAllIndex());
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
        public void commit(DataPersistenceProvider persistenceProvider, long sequenceNumber) {
            context.getReplicatedLog().snapshotCommit();
            persistenceProvider.deleteSnapshots(new SnapshotSelectionCriteria(
                    sequenceNumber - context.getConfigParams().getSnapshotBatchCount(), 43200000));

            persistenceProvider.deleteMessages(sequenceNumber);

            SnapshotManager.this.currentState = IDLE;
        }

        @Override
        public void rollback() {
            context.getReplicatedLog().snapshotRollback();

            LOG.info("{}: Replicated Log rolled back. Snapshot will be attempted in the next cycle." +
                            "snapshotIndex:{}, snapshotTerm:{}, log-size:{}", persistenceId(),
                    context.getReplicatedLog().getSnapshotIndex(),
                    context.getReplicatedLog().getSnapshotTerm(),
                    context.getReplicatedLog().size());

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

    private static class LastAppliedTermInformationReader implements TermInformationReader{
        private long index;
        private long term;

        public LastAppliedTermInformationReader init(ReplicatedLog log, long originalIndex,
                                         ReplicatedLogEntry lastLogEntry, boolean hasFollowers){
            ReplicatedLogEntry entry = log.get(originalIndex);
            if (!hasFollowers) {
                if(lastLogEntry != null) {
                    index = lastLogEntry.getIndex();
                    term = lastLogEntry.getTerm();
                } else {
                    this.index = -1L;
                    this.term = -1L;
                }
            } else if (entry != null) {
                index = entry.getIndex();
                term = entry.getTerm();
            } else if(log.getSnapshotIndex() > -1){
                index = log.getSnapshotIndex();
                term = log.getSnapshotTerm();
            } else {
                this.index = -1L;
                this.term = -1L;
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
            if (entry != null) {
                index = entry.getIndex();
                term = entry.getTerm();
            } else {
                this.index = -1L;
                this.term = -1L;
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
