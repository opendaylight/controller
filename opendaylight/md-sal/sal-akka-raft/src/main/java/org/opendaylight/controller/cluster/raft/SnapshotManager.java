/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.slf4j.Logger;

/**
 * Manages the capturing of snapshots for a RaftActor.
 *
 * @author Moiz Raja
 * @author Thomas Pantelis
 */
public class SnapshotManager implements SnapshotState {

    @SuppressWarnings("checkstyle:MemberName")
    private final SnapshotState IDLE = new Idle();

    @SuppressWarnings({"checkstyle:MemberName", "checkstyle:AbbreviationAsWordInName"})
    private final SnapshotState PERSISTING = new Persisting();

    @SuppressWarnings({"checkstyle:MemberName", "checkstyle:AbbreviationAsWordInName"})
    private final SnapshotState CREATING = new Creating();

    private final Logger log;
    private final RaftActorContext context;
    private final LastAppliedTermInformationReader lastAppliedTermInformationReader =
            new LastAppliedTermInformationReader();
    private final ReplicatedToAllTermInformationReader replicatedToAllTermInformationReader =
            new ReplicatedToAllTermInformationReader();


    private SnapshotState currentState = IDLE;
    private CaptureSnapshot captureSnapshot;
    private long lastSequenceNumber = -1;

    private Consumer<Optional<OutputStream>> createSnapshotProcedure = null;

    private ApplySnapshot applySnapshot;
    private RaftActorSnapshotCohort snapshotCohort = NoopRaftActorSnapshotCohort.INSTANCE;

    /**
     * Constructs an instance.
     *
     * @param context the RaftActorContext
     * @param logger the Logger
     */
    public SnapshotManager(final RaftActorContext context, final Logger logger) {
        this.context = context;
        this.log = logger;
    }

    public boolean isApplying() {
        return applySnapshot != null;
    }

    @Override
    public boolean isCapturing() {
        return currentState.isCapturing();
    }

    @Override
    public boolean captureToInstall(final ReplicatedLogEntry lastLogEntry, final long replicatedToAllIndex,
            final String targetFollower) {
        return currentState.captureToInstall(lastLogEntry, replicatedToAllIndex, targetFollower);
    }

    @Override
    public boolean capture(final ReplicatedLogEntry lastLogEntry, final long replicatedToAllIndex) {
        return currentState.capture(lastLogEntry, replicatedToAllIndex);
    }

    @Override
    public boolean captureWithForcedTrim(final ReplicatedLogEntry lastLogEntry, final long replicatedToAllIndex) {
        return currentState.captureWithForcedTrim(lastLogEntry, replicatedToAllIndex);
    }

    @Override
    public void apply(final ApplySnapshot snapshot) {
        currentState.apply(snapshot);
    }

    @Override
    public void persist(final Snapshot.State state, final Optional<OutputStream> installSnapshotStream,
            final long totalMemory) {
        currentState.persist(state, installSnapshotStream, totalMemory);
    }

    @Override
    public void commit(final long sequenceNumber, final long timeStamp) {
        currentState.commit(sequenceNumber, timeStamp);
    }

    @Override
    public void rollback() {
        currentState.rollback();
    }

    @Override
    public long trimLog(final long desiredTrimIndex) {
        return currentState.trimLog(desiredTrimIndex);
    }

    @SuppressWarnings("checkstyle:hiddenField")
    void setCreateSnapshotConsumer(final Consumer<Optional<OutputStream>> createSnapshotProcedure) {
        this.createSnapshotProcedure = createSnapshotProcedure;
    }

    void setSnapshotCohort(final RaftActorSnapshotCohort snapshotCohort) {
        this.snapshotCohort = snapshotCohort;
    }

    public Snapshot.@NonNull State convertSnapshot(final ByteSource snapshotBytes) throws IOException {
        return snapshotCohort.deserializeSnapshot(snapshotBytes);
    }

    public long getLastSequenceNumber() {
        return lastSequenceNumber;
    }

    @VisibleForTesting
    public CaptureSnapshot getCaptureSnapshot() {
        return captureSnapshot;
    }

    private boolean hasFollowers() {
        return context.hasFollowers();
    }

    private String persistenceId() {
        return context.getId();
    }

    /**
     * Constructs a CaptureSnapshot instance.
     *
     * @param lastLogEntry the last log entry for the snapshot.
     * @param replicatedToAllIndex the index of the last entry replicated to all followers.
     * @return a new CaptureSnapshot instance.
     */
    public CaptureSnapshot newCaptureSnapshot(final ReplicatedLogEntry lastLogEntry, final long replicatedToAllIndex,
                                              final boolean mandatoryTrim) {
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

        final long lastLogEntryIndex;
        final long lastLogEntryTerm;
        if (lastLogEntry == null) {
            // When we don't have journal present, for example two captureSnapshots executed right after another with no
            // new journal we still want to preserve the index and term in the snapshot.
            lastAppliedIndex = lastLogEntryIndex = context.getReplicatedLog().getSnapshotIndex();
            lastAppliedTerm = lastLogEntryTerm = context.getReplicatedLog().getSnapshotTerm();

            log.debug("{}: Capturing Snapshot : lastLogEntry is null. Using snapshot values lastAppliedIndex {} and "
                    + "lastAppliedTerm {} instead.", persistenceId(), lastAppliedIndex, lastAppliedTerm);
        } else {
            lastLogEntryIndex = lastLogEntry.getIndex();
            lastLogEntryTerm = lastLogEntry.getTerm();
        }

        return new CaptureSnapshot(lastLogEntryIndex, lastLogEntryTerm, lastAppliedIndex, lastAppliedTerm,
                newReplicatedToAllIndex, newReplicatedToAllTerm, unAppliedEntries, mandatoryTrim);
    }

    private class AbstractSnapshotState implements SnapshotState {

        @Override
        public boolean isCapturing() {
            return true;
        }

        @Override
        public boolean capture(final ReplicatedLogEntry lastLogEntry, final long replicatedToAllIndex) {
            log.debug("capture should not be called in state {}", this);
            return false;
        }

        @Override
        public boolean captureToInstall(final ReplicatedLogEntry lastLogEntry, final long replicatedToAllIndex,
                final String targetFollower) {
            log.debug("captureToInstall should not be called in state {}", this);
            return false;
        }

        @Override
        public boolean captureWithForcedTrim(final ReplicatedLogEntry lastLogEntry, final long replicatedToAllIndex) {
            log.debug("captureWithForcedTrim should not be called in state {}", this);
            return false;
        }

        @Override
        public void apply(final ApplySnapshot snapshot) {
            log.debug("apply should not be called in state {}", this);
        }

        @Override
        public void persist(final Snapshot.State state, final Optional<OutputStream> installSnapshotStream,
                final long totalMemory) {
            log.debug("persist should not be called in state {}", this);
        }

        @Override
        public void commit(final long sequenceNumber, final long timeStamp) {
            log.debug("commit should not be called in state {}", this);
        }

        @Override
        public void rollback() {
            log.debug("rollback should not be called in state {}", this);
        }

        @Override
        public long trimLog(final long desiredTrimIndex) {
            log.debug("trimLog should not be called in state {}", this);
            return -1;
        }

        protected long doTrimLog(final long desiredTrimIndex) {
            //  we would want to keep the lastApplied as its used while capturing snapshots
            long lastApplied = context.getLastApplied();
            long tempMin = Math.min(desiredTrimIndex, lastApplied > -1 ? lastApplied - 1 : -1);

            if (log.isTraceEnabled()) {
                log.trace("{}: performSnapshotWithoutCapture: desiredTrimIndex: {}, lastApplied: {}, tempMin: {}",
                        persistenceId(), desiredTrimIndex, lastApplied, tempMin);
            }

            if (tempMin > -1 && context.getReplicatedLog().isPresent(tempMin)) {
                log.debug("{}: fakeSnapshot purging log to {} for term {}", persistenceId(), tempMin,
                        context.getTermInformation().getCurrentTerm());

                //use the term of the temp-min, since we check for isPresent, entry will not be null
                ReplicatedLogEntry entry = context.getReplicatedLog().get(tempMin);
                context.getReplicatedLog().snapshotPreCommit(tempMin, entry.getTerm());
                context.getReplicatedLog().snapshotCommit(false);
                return tempMin;
            }

            final RaftActorBehavior currentBehavior = context.getCurrentBehavior();
            if (tempMin > currentBehavior.getReplicatedToAllIndex()) {
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

        @SuppressWarnings("checkstyle:IllegalCatch")
        private boolean capture(final ReplicatedLogEntry lastLogEntry, final long replicatedToAllIndex,
                final String targetFollower, final boolean mandatoryTrim) {
            captureSnapshot = newCaptureSnapshot(lastLogEntry, replicatedToAllIndex, mandatoryTrim);

            OutputStream installSnapshotStream = null;
            if (targetFollower != null) {
                installSnapshotStream = context.getFileBackedOutputStreamFactory().newInstance();
                log.info("{}: Initiating snapshot capture {} to install on {}",
                        persistenceId(), captureSnapshot, targetFollower);
            } else {
                log.info("{}: Initiating snapshot capture {}", persistenceId(), captureSnapshot);
            }

            lastSequenceNumber = context.getPersistenceProvider().getLastSequenceNumber();

            log.debug("{}: lastSequenceNumber prior to capture: {}", persistenceId(), lastSequenceNumber);

            SnapshotManager.this.currentState = CREATING;

            try {
                createSnapshotProcedure.accept(Optional.ofNullable(installSnapshotStream));
            } catch (Exception e) {
                SnapshotManager.this.currentState = IDLE;
                log.error("Error creating snapshot", e);
                return false;
            }

            return true;
        }

        @Override
        public boolean capture(final ReplicatedLogEntry lastLogEntry, final long replicatedToAllIndex) {
            return capture(lastLogEntry, replicatedToAllIndex, null, false);
        }

        @Override
        public boolean captureToInstall(final ReplicatedLogEntry lastLogEntry, final long replicatedToAllIndex,
                final String targetFollower) {
            return capture(lastLogEntry, replicatedToAllIndex, targetFollower, false);
        }

        @Override
        public boolean captureWithForcedTrim(final ReplicatedLogEntry lastLogEntry, final long replicatedToAllIndex) {
            return capture(lastLogEntry, replicatedToAllIndex, null, true);
        }

        @Override
        public void apply(final ApplySnapshot toApply) {
            SnapshotManager.this.applySnapshot = toApply;

            lastSequenceNumber = context.getPersistenceProvider().getLastSequenceNumber();

            log.debug("lastSequenceNumber prior to persisting applied snapshot: {}", lastSequenceNumber);

            context.getPersistenceProvider().saveSnapshot(toApply.getSnapshot());

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
        public void persist(final Snapshot.State snapshotState, final Optional<OutputStream> installSnapshotStream,
                final long totalMemory) {
            // create a snapshot object from the state provided and save it
            // when snapshot is saved async, SaveSnapshotSuccess is raised.

            Snapshot snapshot = Snapshot.create(snapshotState,
                    captureSnapshot.getUnAppliedEntries(),
                    captureSnapshot.getLastIndex(), captureSnapshot.getLastTerm(),
                    captureSnapshot.getLastAppliedIndex(), captureSnapshot.getLastAppliedTerm(),
                    context.getTermInformation().getCurrentTerm(),
                    context.getTermInformation().getVotedFor(), context.getPeerServerInfo(true));

            context.getPersistenceProvider().saveSnapshot(snapshot);

            log.info("{}: Persisting of snapshot done: {}", persistenceId(), snapshot);

            final ConfigParams config = context.getConfigParams();
            final long absoluteThreshold = config.getSnapshotDataThreshold();
            final long dataThreshold = absoluteThreshold != 0 ? absoluteThreshold * ConfigParams.MEGABYTE
                    : totalMemory * config.getSnapshotDataThresholdPercentage() / 100;

            final boolean dataSizeThresholdExceeded = context.getReplicatedLog().dataSize() > dataThreshold;
            final boolean logSizeExceededSnapshotBatchCount =
                    context.getReplicatedLog().size() >= config.getSnapshotBatchCount();

            final RaftActorBehavior currentBehavior = context.getCurrentBehavior();
            if (dataSizeThresholdExceeded || logSizeExceededSnapshotBatchCount || captureSnapshot.isMandatoryTrim()) {
                if (log.isDebugEnabled()) {
                    if (dataSizeThresholdExceeded) {
                        log.debug("{}: log data size {} exceeds the memory threshold {} - doing snapshotPreCommit "
                                + "with index {}", context.getId(), context.getReplicatedLog().dataSize(),
                                dataThreshold, captureSnapshot.getLastAppliedIndex());
                    } else if (logSizeExceededSnapshotBatchCount) {
                        log.debug("{}: log size {} exceeds the snapshot batch count {} - doing snapshotPreCommit with "
                                + "index {}", context.getId(), context.getReplicatedLog().size(),
                                config.getSnapshotBatchCount(), captureSnapshot.getLastAppliedIndex());
                    } else {
                        log.debug("{}: user triggered or root overwrite snapshot encountered, trimming log up to "
                                + "last applied index {}", context.getId(), captureSnapshot.getLastAppliedIndex());
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
                if (captureSnapshot.getReplicatedToAllIndex() >= 0) {
                    currentBehavior.setReplicatedToAllIndex(captureSnapshot.getReplicatedToAllIndex());
                }

            } else if (captureSnapshot.getReplicatedToAllIndex() != -1) {
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

            log.info("{}: Removed in-memory snapshotted entries, adjusted snaphsotIndex: {} and term: {}",
                    context.getId(), context.getReplicatedLog().getSnapshotIndex(),
                    context.getReplicatedLog().getSnapshotTerm());

            if (installSnapshotStream.isPresent()) {
                if (context.getId().equals(currentBehavior.getLeaderId())) {
                    try {
                        ByteSource snapshotBytes = ((FileBackedOutputStream)installSnapshotStream.get()).asByteSource();
                        currentBehavior.handleMessage(context.getActor(),
                                new SendInstallSnapshot(snapshot, snapshotBytes));
                    } catch (IOException e) {
                        log.error("{}: Snapshot install failed due to an unrecoverable streaming error",
                                context.getId(), e);
                    }
                } else {
                    ((FileBackedOutputStream)installSnapshotStream.get()).cleanup();
                }
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
        @SuppressWarnings("checkstyle:IllegalCatch")
        public void commit(final long sequenceNumber, final long timeStamp) {
            log.debug("{}: Snapshot success -  sequence number: {}", persistenceId(), sequenceNumber);

            if (applySnapshot != null) {
                try {
                    Snapshot snapshot = applySnapshot.getSnapshot();

                    //clears the followers log, sets the snapshot index to ensure adjusted-index works
                    context.setReplicatedLog(ReplicatedLogImpl.newInstance(snapshot, context));
                    context.setLastApplied(snapshot.getLastAppliedIndex());
                    context.setCommitIndex(snapshot.getLastAppliedIndex());
                    context.getTermInformation().update(snapshot.getElectionTerm(), snapshot.getElectionVotedFor());

                    if (snapshot.getServerConfiguration() != null) {
                        context.updatePeerIds(snapshot.getServerConfiguration());
                    }

                    if (!(snapshot.getState() instanceof EmptyState)) {
                        snapshotCohort.applySnapshot(snapshot.getState());
                    }

                    applySnapshot.getCallback().onSuccess();
                } catch (Exception e) {
                    log.error("{}: Error applying snapshot", context.getId(), e);
                }
            } else {
                context.getReplicatedLog().snapshotCommit();
            }

            context.getPersistenceProvider().deleteSnapshots(new SnapshotSelectionCriteria(scala.Long.MaxValue(),
                    timeStamp - 1, 0L, 0L));

            context.getPersistenceProvider().deleteMessages(lastSequenceNumber);

            snapshotComplete();
        }

        @Override
        public void rollback() {
            // Nothing to rollback if we're applying a snapshot from the leader.
            if (applySnapshot == null) {
                context.getReplicatedLog().snapshotRollback();

                log.info("{}: Replicated Log rolled back. Snapshot will be attempted in the next cycle."
                        + "snapshotIndex:{}, snapshotTerm:{}, log-size:{}", persistenceId(),
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

    private interface TermInformationReader {
        long getIndex();

        long getTerm();
    }

    static class LastAppliedTermInformationReader implements TermInformationReader {
        private long index;
        private long term;

        LastAppliedTermInformationReader init(final ReplicatedLog log, final long originalIndex,
                final ReplicatedLogEntry lastLogEntry, final boolean hasFollowers) {
            ReplicatedLogEntry entry = log.get(originalIndex);
            this.index = -1L;
            this.term = -1L;
            if (!hasFollowers) {
                if (lastLogEntry != null) {
                    // since we have persisted the last-log-entry to persistent journal before the capture,
                    // we would want to snapshot from this entry.
                    index = lastLogEntry.getIndex();
                    term = lastLogEntry.getTerm();
                }
            } else if (entry != null) {
                index = entry.getIndex();
                term = entry.getTerm();
            } else if (log.getSnapshotIndex() > -1) {
                index = log.getSnapshotIndex();
                term = log.getSnapshotTerm();
            }
            return this;
        }

        @Override
        public long getIndex() {
            return this.index;
        }

        @Override
        public long getTerm() {
            return this.term;
        }
    }

    private static class ReplicatedToAllTermInformationReader implements TermInformationReader {
        private long index;
        private long term;

        ReplicatedToAllTermInformationReader init(final ReplicatedLog log, final long originalIndex) {
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
        public long getIndex() {
            return this.index;
        }

        @Override
        public long getTerm() {
            return this.term;
        }
    }
}
