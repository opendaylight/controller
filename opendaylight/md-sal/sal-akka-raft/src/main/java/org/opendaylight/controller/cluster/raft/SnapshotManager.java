/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

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
    private final RaftActorContext context;

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
        if(!isCapturing()) {
            currentState.captureToInstall(lastLogEntry, replicatedToAllIndex);
        } else {
            LOG.error("Illegal call to captureToInstall made in state = {}", this);
        }
    }

    @Override
    public void capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex) {
        if(!isCapturing()) {
            currentState.capture(lastLogEntry, replicatedToAllIndex);
        } else {
            LOG.error("Illegal call to capture made in state = {}", this);
        }
    }

    @Override
    public void persist(DataPersistenceProvider persistenceProvider, byte[] snapshotBytes, RaftActorBehavior currentBehavior) {
        if(isCapturing()) {
            currentState.persist(persistenceProvider, snapshotBytes, currentBehavior);
        } else {
            LOG.error("Illegal call to persist made in state = {}", this);
        }
    }

    @Override
    public void commit(DataPersistenceProvider persistenceProvider, long sequenceNumber) {
        if(currentState instanceof Persisting) {
            currentState.commit(persistenceProvider, sequenceNumber);
        } else {
            LOG.error("Illegal call to commit made in state = {}", this);
        }
    }

    @Override
    public void rollback() {
        if(currentState instanceof Persisting) {
            currentState.rollback();
        } else {
            LOG.error("Illegal call to rollback made in state = {}", this);
        }
    }

    @Override
    public long trimLog(long desiredTrimIndex) {
        if(!isCapturing()) {
            return currentState.trimLog(desiredTrimIndex);
        } else {
            LOG.error("Illegal call to trimLog made in state = {}", this);
        }
        return -1;
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
            throw new UnsupportedOperationException();
        }

        @Override
        public void captureToInstall(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex) {
            throw new UnsupportedOperationException();
        }


        @Override
        public void persist(DataPersistenceProvider persistenceProvider, byte[] snapshotBytes, RaftActorBehavior currentBehavior) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void commit(DataPersistenceProvider persistenceProvider, long sequenceNumber) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void rollback() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long trimLog(long desiredTrimIndex) {
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
                    new LastAppliedTermInformationReader(context.getReplicatedLog(), context.getLastApplied(),
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
                    new ReplicatedToAllTermInformationReader(context.getReplicatedLog(), replicatedToAllIndex);

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

    }

    private class Capturing extends AbstractSnapshotState {

        @Override
        public boolean isCapturing() {
            return true;
        }

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
        public long trimLog(long desiredTrimIndex) {
            throw new UnsupportedOperationException();
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
    }

    private static interface TermInformationReader {
        long getIndex();
        long getTerm();
    }

    private static class LastAppliedTermInformationReader implements TermInformationReader{
        private final long index;
        private final long term;

        LastAppliedTermInformationReader(ReplicatedLog log, long originalIndex,
                                         ReplicatedLogEntry lastLogEntry, boolean hasFollowers){
            ReplicatedLogEntry entry = log.get(originalIndex);
            if (!hasFollowers) {
                index = lastLogEntry.getIndex();
                term = lastLogEntry.getTerm();
            } else if (entry != null) {
                index = entry.getIndex();
                term = entry.getTerm();
            } else if(originalIndex == log.getSnapshotIndex()){
                index = log.getSnapshotIndex();
                term = log.getSnapshotTerm();
            } else {
                this.index = -1L;
                this.term = -1L;
            }
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
        private final long index;
        private final long term;

        ReplicatedToAllTermInformationReader(ReplicatedLog log, long originalIndex){
            ReplicatedLogEntry entry = log.get(originalIndex);
            if (entry != null) {
                index = entry.getIndex();
                term = entry.getTerm();
            } else {
                this.index = -1L;
                this.term = -1L;
            }
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
