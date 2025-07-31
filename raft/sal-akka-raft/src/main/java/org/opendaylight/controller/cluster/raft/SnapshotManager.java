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
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.apache.pekko.dispatch.ControlMessage;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.api.EntryMeta;
import org.opendaylight.raft.spi.InstallableSnapshot;
import org.opendaylight.raft.spi.SnapshotSource;
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
            EntryInfo lastEntry,
            SnapshotSource snapshot,
            @Nullable VotingConfig serverConfig,
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
    public static final class CaptureSnapshot {
        private final long lastAppliedIndex;
        private final long lastAppliedTerm;
        private final long lastIndex;
        private final long lastTerm;
        private final long replicatedToAllIndex;
        private final long replicatedToAllTerm;
        private final List<@NonNull LogEntry> unAppliedEntries;
        private final boolean mandatoryTrim;

        CaptureSnapshot(final long lastIndex, final long lastTerm, final long lastAppliedIndex,
                final long lastAppliedTerm, final long replicatedToAllIndex, final long replicatedToAllTerm,
                final List<? extends @NonNull LogEntry> unAppliedEntries, final boolean mandatoryTrim) {
            this.lastIndex = lastIndex;
            this.lastTerm = lastTerm;
            this.lastAppliedIndex = lastAppliedIndex;
            this.lastAppliedTerm = lastAppliedTerm;
            this.replicatedToAllIndex = replicatedToAllIndex;
            this.replicatedToAllTerm = replicatedToAllTerm;
            this.unAppliedEntries = List.copyOf(unAppliedEntries);
            this.mandatoryTrim = mandatoryTrim;
        }

        public long getLastAppliedIndex() {
            return lastAppliedIndex;
        }

        public long getLastAppliedTerm() {
            return lastAppliedTerm;
        }

        public @NonNull EntryInfo lastApplied() {
            return EntryInfo.of(lastAppliedIndex, lastAppliedTerm);
        }

        public long getLastIndex() {
            return lastIndex;
        }

        public long getLastTerm() {
            return lastTerm;
        }

        public @NonNull EntryInfo lastEntry() {
            return EntryInfo.of(lastIndex, lastTerm);
        }

        long getReplicatedToAllIndex() {
            return replicatedToAllIndex;
        }

        long getReplicatedToAllTerm() {
            return replicatedToAllTerm;
        }

        public @NonNull EntryInfo replicatedToAll() {
            return EntryInfo.of(replicatedToAllIndex, replicatedToAllTerm);
        }

        List<LogEntry> getUnAppliedEntries() {
            return unAppliedEntries;
        }

        boolean isMandatoryTrim() {
            return mandatoryTrim;
        }

        @Override
        public String toString() {
            return "CaptureSnapshot [lastAppliedIndex=" + lastAppliedIndex
                + ", lastAppliedTerm=" + lastAppliedTerm
                + ", lastIndex=" + lastIndex
                + ", lastTerm=" + lastTerm
                + ", installSnapshotInitiated="
                + ", replicatedToAllIndex=" + replicatedToAllIndex
                + ", replicatedToAllTerm=" + replicatedToAllTerm
                + ", unAppliedEntries size=" + unAppliedEntries.size()
                + ", mandatoryTrim=" + mandatoryTrim + "]";
        }
    }

    /**
     * Internal message sent when a snapshot capture is complete.
     *
     * @author Thomas Pantelis
     */
    @NonNullByDefault
    static final class SnapshotComplete implements ControlMessage {
        static final SnapshotComplete INSTANCE = new SnapshotComplete();

        private SnapshotComplete() {
            // Hidden on purpose
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
    private record Capture(long lastJournalIndex, CaptureSnapshot request) implements Task {
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
     * This instance is talking to persistence for some reason.
     */
    private sealed interface Persist extends Task {
        /**
         * {@return the {@code journalIndex} of the last applied entry}
         */
        long lastJournalIndex();
    }

    /**
     * This instance is persisting an {@link ApplyLeaderSnapshot}.
     */
    @NonNullByDefault
    private record PersistApply(
            long lastJournalIndex,
            Snapshot snapshot,
            ApplyLeaderSnapshot.@Nullable Callback callback) implements Persist {
        PersistApply {
            requireNonNull(snapshot);
        }
    }

    /**
     * This instance is persisting a previously {@link Capture}d snapshot.
     */
    private record PersistCapture(long lastJournalIndex, CaptureSnapshot request) implements Persist {
        PersistCapture {
            requireNonNull(request);
        }
    }

    @NonNullByDefault
    private final class CaptureToInstallCallback<T extends Snapshot.State> extends RaftCallback<InstallableSnapshot> {
        private final T snapshotState;

        CaptureToInstallCallback(final T snapshotState) {
            this.snapshotState = requireNonNull(snapshotState);
        }

        @Override
        public void invoke(final @Nullable Exception failure, final InstallableSnapshot success) {
            if (failure != null) {
                task = Idle.INSTANCE;
                LOG.error("{}: Error creating snapshot", memberId(), failure);
                // FIXME: somehow route to leader, or something ...
            } else {
                persist(snapshotState, success);
            }
        }

        @Override
        protected ToStringHelper addToStringAttributes(final ToStringHelper helper) {
            return helper.add("state", snapshotState);
        }
    }

    @NonNullByDefault
    private final class SaveSnapshotCallback extends RaftCallback<Instant> {
        private final long lastJournalIndex;

        SaveSnapshotCallback(final long lastJournalIndex) {
            this.lastJournalIndex = lastJournalIndex;
        }

        @Override
        public void invoke(final @Nullable Exception failure, final Instant success) {
            if (failure != null) {
                LOG.error("{}: snapshot is not durable", memberId(), failure);
                rollback();
            } else {
                LOG.info("{}: snapshot is durable as of {}", memberId(), success);
                commit(success);
            }
        }

        @Override
        protected ToStringHelper addToStringAttributes(final ToStringHelper helper) {
            return helper.add("lastJournalIndex", lastJournalIndex);
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

    @NonNullByDefault
    @SuppressWarnings("unchecked")
    <T extends Snapshot.State> RaftActorSnapshotCohort<T> snapshotCohort() {
        return (RaftActorSnapshotCohort<T>) snapshotCohort;
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
    public boolean captureToInstall(final EntryMeta lastLogEntry, final long replicatedToAllIndex,
            final String targetFollower) {
        requireNonNull(targetFollower);
        if (!(task instanceof Idle)) {
            LOG.debug("{}: captureToInstall should not be called in state {}", memberId(), task);
            return false;
        }

        final var request = newCaptureSnapshot(lastLogEntry, replicatedToAllIndex, false);
        LOG.info("{}: Initiating snapshot capture {} to install on {}", memberId(), request, targetFollower);

        final var lastJournalIndex = context.getReplicatedLog().lastAppliedJournalIndex();
        LOG.debug("{}: last applied journal index prior to capture: {}", memberId(), lastJournalIndex);

        task = new Capture(lastJournalIndex, request);
        return captureToInstall(snapshotCohort, request);
    }

    @NonNullByDefault
    private <T extends Snapshot.State> boolean captureToInstall(final RaftActorSnapshotCohort<T> typedCohort,
            final CaptureSnapshot request) {
        final var snapshot = typedCohort.takeSnapshot();
        context.snapshotStore().streamToInstall(request.lastApplied(),
            ToStorage.of(typedCohort.support().writer(), snapshot), new CaptureToInstallCallback<>(snapshot));
        return true;
    }

    /**
     * Initiates a capture snapshot, while enforcing trimming of the log up to lastAppliedIndex.
     *
     * @param lastLogEntry the last entry in the replicated log
     * @param replicatedToAllIndex the current replicatedToAllIndex
     * @return true if capture was started
     */
    boolean captureWithForcedTrim(final EntryMeta lastLogEntry, final long replicatedToAllIndex) {
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
    public boolean capture(final EntryMeta lastLogEntry, final long replicatedToAllIndex) {
        if (!(task instanceof Idle)) {
            LOG.debug("{}: capture should not be called in state {}", memberId(), task);
            return false;
        }
        return capture(newCaptureSnapshot(lastLogEntry, replicatedToAllIndex, false));
    }

    private boolean capture(final @NonNull CaptureSnapshot request) {
        LOG.info("{}: Initiating snapshot capture {}", memberId(), request);
        final var snapshotState = snapshotCohort.takeSnapshot();
        final var lastJournalIndex = context.getReplicatedLog().lastAppliedJournalIndex();
        LOG.debug("{}: captured snapshot at lastSequenceNumber: {}", memberId(), lastJournalIndex);
        persist(lastJournalIndex, request, snapshotState);
        return true;
    }

    @NonNullByDefault
    private CaptureSnapshot newCaptureSnapshot(final @Nullable EntryMeta lastLogEntry, final long replicatedToAllIndex,
            final boolean mandatoryTrim) {
        return context.getReplicatedLog().newCaptureSnapshot(lastLogEntry, replicatedToAllIndex, mandatoryTrim,
            context.hasFollowers());
    }

    /**
     * Applies a snapshot on a follower that was installed by the leader.
     *
     * @param leaderSnapshot the {@link ApplyLeaderSnapshot} to apply.
     */
    @NonNullByDefault
    void applyFromLeader(final ApplyLeaderSnapshot leaderSnapshot) {
        if (!(task instanceof Idle)) {
            LOG.debug("{}: applySnapshot should not be called in state {}", memberId(), task);
            return;
        }

        final var source = leaderSnapshot.snapshot();
        LOG.info("{}: Applying snapshot on follower: {}", memberId(), source);

        final Snapshot.State snapshotState;
        try (var in = source.toPlainSource().io().openBufferedStream()) {
            snapshotState = snapshotCohort().support().reader().readSnapshot(in);
        } catch (IOException e) {
            LOG.debug("{}: failed to convert InstallSnapshot to state", memberId(), e);
            leaderSnapshot.callback().onFailure();
            return;
        }

        LOG.debug("{}: Converted InstallSnapshot from leader: {} to state{}", memberId(), leaderSnapshot.leaderId(),
            snapshotState.needsMigration() ? " (needs migration)" : "");

        final var snapshot = Snapshot.ofTermLeader(snapshotState, leaderSnapshot.lastEntry(), context.termInfo(),
            leaderSnapshot.serverConfig());
        final var callback = leaderSnapshot.callback;
        final var lastJournalIndex = context.getReplicatedLog().lastAppliedJournalIndex();
        task = new PersistApply(lastJournalIndex, snapshot, callback);
        LOG.debug("{}: last applied journal index prior to persisting applied snapshot: {}", memberId(),
            lastJournalIndex);
        saveSnapshot(new RaftSnapshot(snapshot.votingConfig()), snapshot.lastApplied(), snapshot.state(),
            lastJournalIndex);
    }

    @NonNullByDefault
    private <T extends StateSnapshot> void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final Snapshot.@Nullable State snapshot, final long lastJournalIndex) {
        context.snapshotStore().saveSnapshot(raftSnapshot, lastIncluded,
            ToStorage.ofNullable(snapshotCohort().support().writer(), snapshot),
            new SaveSnapshotCallback(lastJournalIndex));
    }

    /**
     * Persists a snapshot.
     *
     * @param snapshotState the snapshot State
     * @param installable the {@link InstallableSnapshot}
     */
    @NonNullByDefault
    @VisibleForTesting
    public void persist(final Snapshot.State snapshotState, final InstallableSnapshot installable) {
        if (!(task instanceof Capture(var lastJournalIndex, var request))) {
            LOG.debug("{}: persist should not be called in state {}", memberId(), task);
            return;
        }

        persist(lastJournalIndex, request, snapshotState);

        if (context.getCurrentBehavior() instanceof AbstractLeader leader) {
            leader.sendInstallSnapshot(installable);
        }
    }

    private void persist(final long lastJournalIndex, final CaptureSnapshot request,
            final Snapshot.State snapshotState) {
        // create a snapshot object from the state provided and save it when snapshot is saved async,

        LOG.info("{}: Persising snapshot at {}/{}", memberId(), request.lastApplied(), request.lastEntry());

        // Note: we ignore unapplied entries, as that is not what we want to trim
        saveSnapshot(new RaftSnapshot(context.getPeerServerInfo(true)), request.lastApplied(), snapshotState,
            lastJournalIndex);

        final var config = context.getConfigParams();
        final long absoluteThreshold = config.getSnapshotDataThreshold();
        final long dataThreshold = absoluteThreshold != 0 ? absoluteThreshold * 1_048_576
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

        LOG.info("{}: Removed in-memory snapshotted entries, adjusted snapshotIndex: {} and term: {}", memberId(),
            replLog.getSnapshotIndex(), replLog.getSnapshotTerm());

        task = new PersistCapture(lastJournalIndex, request);
    }

    /**
     * Commit the snapshot by trimming the log.
     *
     * @param timestamp the time stamp of the persisted snapshot
     */
    @NonNullByDefault
    @VisibleForTesting
    void commit(final Instant timestamp) {
        if (!(task instanceof Persist persist)) {
            LOG.debug("{}: commit should not be called in state {}", memberId(), task);
            return;
        }

        final var lastJournalIndex = commit(persist);
        LOG.debug("{}: Snapshot success, discarding journal entries up to {}", memberId(), lastJournalIndex);
        context.entryStore().discardHead(lastJournalIndex + 1);

        snapshotComplete();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private long commit(final Persist persist) {
        return switch (persist) {
            case PersistApply(var lastJournalIndex, var snapshot, var callback) -> {
                try {
                    // clears the followers log, sets the snapshot index to ensure adjusted-index works
                    context.getReplicatedLog().resetToSnapshot(snapshot);

                    final var serverConfig = snapshot.votingConfig();
                    if (serverConfig != null) {
                        context.updateVotingConfig(serverConfig);
                    }

                    final var state = snapshot.state();
                    if (state != null) {
                        applySnapshotState(snapshotCohort, state);
                    }

                    if (callback != null) {
                        callback.onSuccess();
                    }
                } catch (Exception e) {
                    LOG.error("{}: Error applying snapshot", memberId(), e);
                }
                yield lastJournalIndex;
            }
            case PersistCapture capture -> {
                context.getReplicatedLog().snapshotCommit();
                yield capture.lastJournalIndex;
            }
        };
    }

    @NonNullByDefault
    private <T extends Snapshot.State> void applySnapshotState(final RaftActorSnapshotCohort<T> cohort,
            final Snapshot.State state) {
        final T casted;
        try {
            casted = cohort.support().snapshotType().cast(state);
        } catch (ClassCastException e) {
            LOG.warn("{}: not applyling state {}", memberId(), state, e);
            return;
        }
        cohort.applySnapshot(casted);
    }

    /**
     * Rolls back the snapshot on failure.
     */
    @VisibleForTesting
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
        final var replLog = context.getReplicatedLog();
        long lastApplied = replLog.getLastApplied();
        long tempMin = Math.min(desiredTrimIndex, lastApplied > -1 ? lastApplied - 1 : -1);

        if (LOG.isTraceEnabled()) {
            LOG.trace("{}: performSnapshotWithoutCapture: desiredTrimIndex: {}, lastApplied: {}, tempMin: {}",
                    memberId(), desiredTrimIndex, lastApplied, tempMin);
        }

        if (tempMin > -1) {
            if (replLog.isPresent(tempMin)) {
                LOG.debug("{}: fakeSnapshot purging log to {} for term {}", memberId(), tempMin, context.currentTerm());

                //use the term of the temp-min, since we check for isPresent, entry will not be null
                final var entry = replLog.lookupMeta(tempMin);
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

    @VisibleForTesting
    public @Nullable CaptureSnapshot getCaptureSnapshot() {
        return switch (task) {
            case Capture capture -> capture.request;
            case PersistCapture persist -> persist.request;
            default -> null;
        };
    }
}
