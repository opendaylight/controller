/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.VerifyException;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader.SnapshotBytes;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.ImmutableRaftEntryMeta;
import org.opendaylight.controller.cluster.raft.spi.RaftEntryMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the capturing of snapshots for a RaftActor.
 *
 * @author Moiz Raja
 * @author Thomas Pantelis
 */
public final class SnapshotManager {
    /**
     * Internal message, issued by follower behavior to its actor, eventually routed to {@link SnapshotManager}.
     * Metadata matches information conveyed in {@link InstallSnapshot}.
     */
    @NonNullByDefault
    public record ApplyLeaderSnapshot(
            String leaderId,
            long term,
            ImmutableRaftEntryMeta lastEntry,
            ByteSource snapshot,
            @Nullable ClusterConfig serverConfig,
            ApplyLeaderSnapshot.Callback callback) {
        public ApplyLeaderSnapshot {
            requireNonNull(leaderId);
            requireNonNull(lastEntry);
            requireNonNull(snapshot);
            requireNonNull(callback);
            // TODO: sanity check term vs. lastEntry ?
        }

        public interface Callback {

            void onSuccess();

            void onFailure();
        }
    }

    @VisibleForTesting
    @NonNullByDefault
    public static final class CommitSnapshot {
        static final CommitSnapshot INSTANCE = new CommitSnapshot();

        private CommitSnapshot() {
            // Hidden on purpose
        }

        @Override
        public String toString() {
            return "commit_snapshot";
        }
    }

    /**
     * The task being executed by this instance.
     */
    private sealed interface Task {
        // Nothing else
    }

    /**
     * This instance is capturing current user state, for example to save a state snapshot prior to purging journal
     * entries.
     */
    @NonNullByDefault
    private record Capture(long lastSequenceNumber, CaptureSnapshot request) implements Task {
        Capture {
            requireNonNull(request);
        }
    }

    /**
     * This instance is just sitting here, doing nothing in particular.
     */
    @NonNullByDefault
    private static final class Idle implements Task {
        private static final Idle INSTANCE = new Idle();
    }

    /**
     * This instance is talking to persistence for some reason. We have started talking to persistence when it was
     * at {@linkplain #lastSequenceNumber}.
     */
    private sealed interface Persist extends Task {
        /**
         * Returns the last sequence number reported by persistence when this task started.
         *
         * @return persistence last sequence number
         */
        long lastSequenceNumber();
    }

    /**
     * This instance is persisting an {@link ApplyLeaderSnapshot}.
     */
    @NonNullByDefault
    private record PersistApply(
            long lastSequenceNumber,
            Snapshot snapshot,
            ApplyLeaderSnapshot.@Nullable Callback callback) implements Persist {
        PersistApply {
            requireNonNull(snapshot);
        }
    }

    /**
     * This instance is persisting a previously {@link Capture}d snapshot.
     */
    private record PersistCapture(long lastSequenceNumber, CaptureSnapshot request) implements Persist {
        PersistCapture {
            requireNonNull(request);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SnapshotManager.class);

    private final @NonNull RaftActorContext context;

    private @NonNull RaftActorSnapshotCohort<?> snapshotCohort = NoopRaftActorSnapshotCohort.INSTANCE;
    private @NonNull Task task = Idle.INSTANCE;

    /**
     * Constructs an instance.
     *
     * @param context the RaftActorContext
     */
    SnapshotManager(final RaftActorContext context) {
        this.context = requireNonNull(context);
    }

    @NonNull String memberId() {
        return context.getId();
    }

    public boolean isApplying() {
        return task instanceof PersistApply;
    }

    /**
     * Returns whether or not a capture is in progress.
     *
     * @return true when a snapshot is being captured, false otherwise
     */
    public boolean isCapturing() {
        return !(task instanceof Idle);
    }

    /**
     * Initiates a capture snapshot for the purposing of installing the snapshot on a follower.
     *
     * @param lastLogEntry the last entry in the replicated log
     * @param replicatedToAllIndex the current replicatedToAllIndex
     * @param targetFollower the id of the follower on which to install
     * @return true if capture was started
     */
    public boolean captureToInstall(final RaftEntryMeta lastLogEntry, final long replicatedToAllIndex,
            final String targetFollower) {
        requireNonNull(targetFollower);
        if (!(task instanceof Idle)) {
            LOG.debug("{}: captureToInstall should not be called in state {}", memberId(), task);
            return false;
        }

        final var request = newCaptureSnapshot(lastLogEntry, replicatedToAllIndex, false);
        LOG.info("{}: Initiating snapshot capture {} to install on {}", memberId(), request, targetFollower);

        final var lastSeq = context.getPersistenceProvider().getLastSequenceNumber();
        LOG.debug("{}: lastSequenceNumber prior to capture: {}", memberId(), lastSeq);

        task = new Capture(lastSeq, request);
        return captureToInstall(context.getFileBackedOutputStreamFactory().newInstance());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private boolean captureToInstall(final @NonNull OutputStream outputStream) {
        try {
            snapshotCohort.createSnapshot(context.getActor(), outputStream);
        } catch (Exception e) {
            task = Idle.INSTANCE;
            LOG.error("{}: Error creating snapshot", memberId(), e);
            return false;
        }
        return true;
    }

    /**
     * Initiates a capture snapshot, while enforcing trimming of the log up to lastAppliedIndex.
     *
     * @param lastLogEntry the last entry in the replicated log
     * @param replicatedToAllIndex the current replicatedToAllIndex
     * @return true if capture was started
     */
    boolean captureWithForcedTrim(final RaftEntryMeta lastLogEntry, final long replicatedToAllIndex) {
        if (!(task instanceof Idle)) {
            LOG.debug("{}: captureWithForcedTrim should not be called in state {}", memberId(), task);
            return false;
        }
        return capture(newCaptureSnapshot(lastLogEntry, replicatedToAllIndex, true));
    }

    /**
     * Initiates a capture snapshot.
     *
     * @param lastLogEntry the last entry in the replicated log
     * @param replicatedToAllIndex the current replicatedToAllIndex
     * @return true if capture was started
     */
    public boolean capture(final RaftEntryMeta lastLogEntry, final long replicatedToAllIndex) {
        if (!(task instanceof Idle)) {
            LOG.debug("{}: capture should not be called in state {}", memberId(), task);
            return false;
        }
        return capture(newCaptureSnapshot(lastLogEntry, replicatedToAllIndex, false));
    }

    private boolean capture(final @NonNull CaptureSnapshot request) {
        LOG.info("{}: Initiating snapshot capture {}", memberId(), request);
        final var lastSeq = context.getPersistenceProvider().getLastSequenceNumber();
        final var snapshotState = snapshotCohort.takeSnapshot();
        LOG.debug("{}: captured snapshot at lastSequenceNumber: {}", memberId(), lastSeq);
        persist(lastSeq, request, snapshotState, null);
        return true;
    }

    /**
     * Applies a snapshot on a follower that was installed by the leader.
     *
     * @param snapshot the {@link ApplyLeaderSnapshot} to apply.
     */
    @NonNullByDefault
    void applyFromLeader(final ApplyLeaderSnapshot snapshot) {
        if (!(task instanceof Idle)) {
            LOG.debug("{}: applySnapshot should not be called in state {}", memberId(), task);
            return;
        }

        final var snapshotBytes = snapshot.snapshot();
        LOG.info("{}: Applying snapshot on follower: {}", memberId(), snapshotBytes);

        final Snapshot.State snapshotState;
        try {
            snapshotState = snapshotCohort.deserializeSnapshot(snapshotBytes);
        } catch (IOException e) {
            LOG.debug("{}: failed to convert InstallSnapshot to state", memberId(), e);
            snapshot.callback().onFailure();
            return;
        }

        LOG.debug("{}: Converted InstallSnapshot from leader: {} to state{}", memberId(), snapshot.leaderId(),
            snapshotState.needsMigration() ? " (needs migration)" : "");
        persistSnapshot(
            Snapshot.ofTermLeader(snapshotState, snapshot.lastEntry(), context.termInfo(), snapshot.serverConfig()),
            snapshot.callback());
    }

    /**
     * Applies a snapshot from recovery.
     *
     * @param snapshot the {@link Snapshot} to apply.
     */
    @NonNullByDefault
    public void applyFromRecovery(final Snapshot snapshot) {
        if (task instanceof Idle) {
            persistSnapshot(requireNonNull(snapshot), null);
        } else {
            LOG.debug("{}: apply should not be called in state {}", memberId(), task);
        }
    }

    @NonNullByDefault
    private void persistSnapshot(final Snapshot snapshot, final ApplyLeaderSnapshot.@Nullable Callback callback) {
        final var persistence = context.getPersistenceProvider();
        final var lastSeq = persistence.getLastSequenceNumber();
        final var persisting = new PersistApply(lastSeq, snapshot, callback);

        task = persisting;
        LOG.debug("{}: lastSequenceNumber prior to persisting applied snapshot: {}", memberId(), lastSeq);
        persistence.saveSnapshot(persisting.snapshot);
    }

    /**
     * Persists a snapshot.
     *
     * @param snapshotState the snapshot State
     * @param installSnapshotStream Optional OutputStream that is present if the snapshot is to also be installed
     *        on a follower.
     */
    @VisibleForTesting
    public void persist(final Snapshot.State snapshotState, final @Nullable OutputStream installSnapshotStream) {
        if (task instanceof Capture(var lastSeq, var request)) {
            persist(lastSeq, request, snapshotState, installSnapshotStream);
        } else {
            LOG.debug("{}: persist should not be called in state {}", memberId(), task);
        }
    }

    private void persist(final long lastSeq, final CaptureSnapshot request, final Snapshot.State snapshotState,
            final @Nullable OutputStream installSnapshotStream) {
        // create a snapshot object from the state provided and save it when snapshot is saved async,
        // SaveSnapshotSuccess is raised.
        final var snapshot = Snapshot.create(snapshotState, request.getUnAppliedEntries(),
                request.getLastIndex(), request.getLastTerm(),
                request.getLastAppliedIndex(), request.getLastAppliedTerm(),
                context.termInfo(), context.getPeerServerInfo(true));

        context.getPersistenceProvider().saveSnapshot(snapshot);

        LOG.info("{}: Persisting of snapshot done: {}", memberId(), snapshot);

        final var config = context.getConfigParams();
        final long absoluteThreshold = config.getSnapshotDataThreshold();
        final long dataThreshold = absoluteThreshold != 0 ? absoluteThreshold * ConfigParams.MEGABYTE
                : context.getTotalMemory() * config.getSnapshotDataThresholdPercentage() / 100;

        final var replLog = context.getReplicatedLog();
        final boolean dataSizeThresholdExceeded = replLog.dataSize() > dataThreshold;
        final boolean logSizeExceededSnapshotBatchCount = replLog.size() >= config.getSnapshotBatchCount();

        final var currentBehavior = context.getCurrentBehavior();
        if (dataSizeThresholdExceeded || logSizeExceededSnapshotBatchCount || request.isMandatoryTrim()) {
            if (LOG.isDebugEnabled()) {
                final var lastAppliedIndex = request.getLastAppliedIndex();
                if (dataSizeThresholdExceeded) {
                    LOG.debug("{}: log data size {} exceeds the memory threshold {} - doing snapshotPreCommit "
                            + "with index {}", memberId(), replLog.dataSize(), dataThreshold, lastAppliedIndex);
                } else if (logSizeExceededSnapshotBatchCount) {
                    LOG.debug(
                        "{}: log size {} exceeds the snapshot batch count {} - doing snapshotPreCommit with index {}",
                        memberId(), replLog.size(), config.getSnapshotBatchCount(), lastAppliedIndex);
                } else {
                    LOG.debug("{}: user triggered or root overwrite snapshot encountered, trimming log up to "
                            + "last applied index {}", memberId(), lastAppliedIndex);
                }
            }

            // We either exceeded the memory threshold or the log size exceeded the snapshot batch
            // count so, to keep the log memory footprint in check, clear the log based on lastApplied.
            // This could/should only happen if one of the followers is down as normally we keep
            // removing from the log as entries are replicated to all.
            replLog.snapshotPreCommit(request.getLastAppliedIndex(), request.getLastAppliedTerm());

            // Don't reset replicatedToAllIndex to -1 as this may prevent us from trimming the log after an
            // install snapshot to a follower.
            if (request.getReplicatedToAllIndex() >= 0) {
                currentBehavior.setReplicatedToAllIndex(request.getReplicatedToAllIndex());
            }

        } else if (request.getReplicatedToAllIndex() != -1) {
            // clear the log based on replicatedToAllIndex
            replLog.snapshotPreCommit(request.getReplicatedToAllIndex(),
                request.getReplicatedToAllTerm());

            currentBehavior.setReplicatedToAllIndex(request.getReplicatedToAllIndex());
        } else {
            // The replicatedToAllIndex was not found in the log
            // This means that replicatedToAllIndex never moved beyond -1 or that it is already in the snapshot.
            // In this scenario we may need to save the snapshot to the akka persistence
            // snapshot for recovery but we do not need to do the replicated log trimming.
            replLog.snapshotPreCommit(replLog.getSnapshotIndex(), replLog.getSnapshotTerm());
        }

        LOG.info("{}: Removed in-memory snapshotted entries, adjusted snaphsotIndex: {} and term: {}", memberId(),
            replLog.getSnapshotIndex(), replLog.getSnapshotTerm());

        if (installSnapshotStream != null) {
            // FIXME: this should not be necessary
            if (!(installSnapshotStream instanceof FileBackedOutputStream snapshotStream)) {
                throw new VerifyException("Unexpected stream " + installSnapshotStream);
            }

            if (memberId().equals(currentBehavior.getLeaderId())) {
                try {
                    final var bytes = snapshotStream.asByteSource();
                    currentBehavior.handleMessage(context.getActor(),
                        new SnapshotBytes(request.getLastAppliedIndex(), request.getLastAppliedTerm(), bytes));
                } catch (IOException e) {
                    LOG.error("{}: Snapshot install failed due to an unrecoverable streaming error", memberId(), e);
                }
            } else {
                snapshotStream.cleanup();
            }
        }

        task = new PersistCapture(lastSeq, request);
    }

    /**
     * Commit the snapshot by trimming the log.
     *
     * @param sequenceNumber the sequence number of the persisted snapshot
     * @param timeStamp the time stamp of the persisted snapshot
     */
    void commit(final long sequenceNumber, final long timeStamp) {
        if (!(task instanceof Persist persist)) {
            LOG.debug("{}: commit should not be called in state {}", memberId(), task);
            return;
        }

        LOG.debug("{}: Snapshot success -  sequence number: {}", memberId(), sequenceNumber);
        final var lastSequenceNumber = commit(persist);

        final var persistence = context.getPersistenceProvider();
        persistence.deleteSnapshots(new SnapshotSelectionCriteria(scala.Long.MaxValue(), timeStamp - 1, 0L, 0L));
        persistence.deleteMessages(lastSequenceNumber);

        snapshotComplete();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private long commit(final Persist persist) {
        return switch (persist) {
            case PersistApply(var lastSeq, var snapshot, var callback) -> {
                try {
                    // clears the followers log, sets the snapshot index to ensure adjusted-index works
                    context.setReplicatedLog(new ReplicatedLogImpl(context, snapshot));
                    context.setLastApplied(snapshot.getLastAppliedIndex());
                    context.setCommitIndex(snapshot.getLastAppliedIndex());
                    // FIXME: This may be coming from the leader: we do not want to pollute our TermInfo if it is for
                    //        this term: we may need to know who we voted for in the next elections.
                    //        This behavior means we report as if we voted for the leader.
                    context.setTermInfo(snapshot.termInfo());

                    final var serverConfig = snapshot.getServerConfiguration();
                    if (serverConfig != null) {
                        context.updatePeerIds(serverConfig);
                    }

                    final var state = snapshot.getState();
                    if (state != null && !(state instanceof EmptyState)) {
                        applySnapshotState(snapshotCohort, state);
                    }

                    if (callback != null) {
                        callback.onSuccess();
                    }
                } catch (Exception e) {
                    LOG.error("{}: Error applying snapshot", memberId(), e);
                }
                yield lastSeq;
            }
            case PersistCapture capture -> {
                context.getReplicatedLog().snapshotCommit();
                yield capture.lastSequenceNumber;
            }
        };
    }

    private <T extends Snapshot.State> void applySnapshotState(final @NonNull RaftActorSnapshotCohort<T> cohort,
            final Snapshot.@NonNull State state) {
        final T casted;
        try {
            casted = cohort.stateClass().cast(state);
        } catch (ClassCastException e) {
            LOG.warn("{}: not applyling state {}", memberId(), state, e);
            return;
        }
        cohort.applySnapshot(casted);
    }

    /**
     * Rolls back the snapshot on failure.
     */
    void rollback() {
        switch (task) {
            case PersistApply persist -> {
                // Nothing to rollback if we're applying a snapshot from the leader.
                final var callback = persist.callback;
                if (callback != null)  {
                    callback.onFailure();
                }
                snapshotComplete();
            }
            case PersistCapture persist -> {
                final var replLog = context.getReplicatedLog();
                replLog.snapshotRollback();
                LOG.info("{}: Replicated Log rolled back. Snapshot will be attempted in the next cycle. "
                        + "snapshotIndex:{}, snapshotTerm:{}, log-size:{}", memberId(), replLog.getSnapshotIndex(),
                        replLog.getSnapshotTerm(), replLog.size());
                snapshotComplete();
            }
            default -> LOG.debug("{}: rollback should not be called in state {}", memberId(), task);
        }
    }

    private void snapshotComplete() {
        task = Idle.INSTANCE;
        context.getActor().tell(SnapshotComplete.INSTANCE, context.getActor());
    }

    /**
     * Trims the in-memory log.
     *
     * @param desiredTrimIndex the desired index to trim from
     * @return the actual trim index
     */
    public long trimLog(final long desiredTrimIndex) {
        if (!(task instanceof Idle)) {
            LOG.debug("{}: trimLog should not be called in state {}", memberId(), task);
            return -1;
        }

        //  we would want to keep the lastApplied as its used while capturing snapshots
        long lastApplied = context.getLastApplied();
        long tempMin = Math.min(desiredTrimIndex, lastApplied > -1 ? lastApplied - 1 : -1);

        if (LOG.isTraceEnabled()) {
            LOG.trace("{}: performSnapshotWithoutCapture: desiredTrimIndex: {}, lastApplied: {}, tempMin: {}",
                    memberId(), desiredTrimIndex, lastApplied, tempMin);
        }

        if (tempMin > -1) {
            final var replLog = context.getReplicatedLog();
            if (replLog.isPresent(tempMin)) {
                LOG.debug("{}: fakeSnapshot purging log to {} for term {}", memberId(), tempMin, context.currentTerm());

                //use the term of the temp-min, since we check for isPresent, entry will not be null
                final var entry = replLog.get(tempMin);
                replLog.snapshotPreCommit(tempMin, entry.term());
                replLog.snapshotCommit(false);
                return tempMin;
            }
        }

        final var currentBehavior = context.getCurrentBehavior();
        if (tempMin > currentBehavior.getReplicatedToAllIndex()) {
            // It is possible a follower was lagging and an install snapshot advanced its match index past the current
            // replicatedToAllIndex. Since the follower is now caught up we should advance the replicatedToAllIndex
            // (to tempMin). The fact that tempMin was not found in the log is likely due to a previous snapshot
            // triggered by the memory threshold exceeded, in that case we trim the log to the last applied index even
            // if previous entries weren't replicated to all followers.
            currentBehavior.setReplicatedToAllIndex(tempMin);
        }
        return -1;
    }

    @VisibleForTesting
    public void setSnapshotCohort(final RaftActorSnapshotCohort<?> snapshotCohort) {
        this.snapshotCohort = requireNonNull(snapshotCohort);
    }

    long getLastSequenceNumber() {
        return task instanceof Persist persist ? persist.lastSequenceNumber() : -1;
    }

    @VisibleForTesting
    public @Nullable CaptureSnapshot getCaptureSnapshot() {
        return switch (task) {
            case Capture capture -> capture.request;
            case PersistCapture persist -> persist.request;
            default -> null;
        };
    }

    /**
     * Constructs a CaptureSnapshot instance.
     *
     * @param lastLogEntry the last log entry for the snapshot.
     * @param replicatedToAllIndex the index of the last entry replicated to all followers.
     * @return a new CaptureSnapshot instance.
     */
    @NonNull CaptureSnapshot newCaptureSnapshot(final RaftEntryMeta lastLogEntry, final long replicatedToAllIndex,
            final boolean mandatoryTrim) {
        final var replLog = context.getReplicatedLog();
        final var lastAppliedEntry = AbstractReplicatedLog.computeLastAppliedEntry(replLog, context.getLastApplied(),
            lastLogEntry, context.hasFollowers());

        final var entry = replLog.get(replicatedToAllIndex);
        final var replicatedToAllEntry = entry != null ? entry : ImmutableRaftEntryMeta.of(-1, -1);

        long lastAppliedIndex = lastAppliedEntry.index();
        long lastAppliedTerm = lastAppliedEntry.term();

        final var unAppliedEntries = replLog.getFrom(lastAppliedIndex + 1);

        final long lastLogEntryIndex;
        final long lastLogEntryTerm;
        if (lastLogEntry == null) {
            // When we don't have journal present, for example two captureSnapshots executed right after another with no
            // new journal we still want to preserve the index and term in the snapshot.
            lastAppliedIndex = lastLogEntryIndex = replLog.getSnapshotIndex();
            lastAppliedTerm = lastLogEntryTerm = replLog.getSnapshotTerm();

            LOG.debug("{}: Capturing Snapshot : lastLogEntry is null. Using snapshot values lastAppliedIndex {} and "
                    + "lastAppliedTerm {} instead.", memberId(), lastAppliedIndex, lastAppliedTerm);
        } else {
            lastLogEntryIndex = lastLogEntry.index();
            lastLogEntryTerm = lastLogEntry.term();
        }

        return new CaptureSnapshot(lastLogEntryIndex, lastLogEntryTerm, lastAppliedIndex, lastAppliedTerm,
            replicatedToAllEntry.index(), replicatedToAllEntry.term(), unAppliedEntries, mandatoryTrim);
    }
}
