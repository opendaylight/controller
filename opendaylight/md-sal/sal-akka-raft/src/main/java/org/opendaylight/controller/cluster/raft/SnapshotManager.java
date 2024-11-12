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
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLeaderSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.ImmutableRaftEntryMeta;
import org.opendaylight.controller.cluster.raft.spi.RaftEntryMeta;
import org.slf4j.Logger;

/**
 * Manages the capturing of snapshots for a RaftActor.
 *
 * @author Moiz Raja
 * @author Thomas Pantelis
 */
public class SnapshotManager implements SnapshotState {
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
     * This instance is persisting an {@link ApplySnapshot}.
     */
    @NonNullByDefault
    private record PersistApply(
            long lastSequenceNumber,
            ApplySnapshot request,
            ApplyLeaderSnapshot.@Nullable Callback callback) implements Persist {
        PersistApply {
            requireNonNull(request);
            requireNonNull(callback);
        }
    }

    /**
     * This instance is persisting a previously {@link Capture}d snapshot.
     */
    private record PersistCapture(long lastSequenceNumber) implements Persist {
        // Nothing else
    }

    private final RaftActorContext context;
    private final Logger log;

    private RaftActorSnapshotCohort snapshotCohort = NoopRaftActorSnapshotCohort.INSTANCE;
    private Consumer<Optional<OutputStream>> createSnapshotProcedure = null;
    private @NonNull Task task = Idle.INSTANCE;

    /**
     * Constructs an instance.
     *
     * @param context the RaftActorContext
     * @param logger the Logger
     */
    public SnapshotManager(final RaftActorContext context, final Logger logger) {
        this.context = context;
        log = logger;
    }

    public boolean isApplying() {
        return task instanceof PersistApply;
    }

    @Override
    public boolean isCapturing() {
        return !(task instanceof Idle);
    }

    @Override
    public boolean capture(final RaftEntryMeta lastLogEntry, final long replicatedToAllIndex) {
        if (task instanceof Idle) {
            return capture(lastLogEntry, replicatedToAllIndex, null, false);
        }
        log.debug("capture should not be called in state {}", task);
        return false;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private boolean capture(final RaftEntryMeta lastLogEntry, final long replicatedToAllIndex,
            final String targetFollower, final boolean mandatoryTrim) {
        final var request = newCaptureSnapshot(lastLogEntry, replicatedToAllIndex, mandatoryTrim);

        final OutputStream installSnapshotStream;
        if (targetFollower != null) {
            installSnapshotStream = context.getFileBackedOutputStreamFactory().newInstance();
            log.info("{}: Initiating snapshot capture {} to install on {}",
                    persistenceId(), request, targetFollower);
        } else {
            installSnapshotStream = null;
            log.info("{}: Initiating snapshot capture {}", persistenceId(), request);
        }

        final var lastSeq = context.getPersistenceProvider().getLastSequenceNumber();

        log.debug("{}: lastSequenceNumber prior to capture: {}", persistenceId(), lastSeq);

        task = new Capture(lastSeq, request);

        try {
            createSnapshotProcedure.accept(Optional.ofNullable(installSnapshotStream));
        } catch (Exception e) {
            task = Idle.INSTANCE;
            log.error("Error creating snapshot", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean captureToInstall(final RaftEntryMeta lastLogEntry, final long replicatedToAllIndex,
            final String targetFollower) {
        if (task instanceof Idle) {
            return capture(lastLogEntry, replicatedToAllIndex, targetFollower, false);
        }
        log.debug("captureToInstall should not be called in state {}", task);
        return false;
    }

    @Override
    public boolean captureWithForcedTrim(final RaftEntryMeta lastLogEntry, final long replicatedToAllIndex) {
        if (task instanceof Idle) {
            return capture(lastLogEntry, replicatedToAllIndex, null, true);
        }
        log.debug("captureWithForcedTrim should not be called in state {}", task);
        return false;
    }


    @Override
    public void applySnapshot(final ApplyLeaderSnapshot snapshot) {
        if (!(task instanceof Idle)) {
            log.debug("installSnapshot should not be called in state {}", task);
            return;
        }

        // FIXME: implement this

    }

    @Override
    public void apply(final ApplySnapshot snapshot) {
        if (!(task instanceof Idle)) {
            log.debug("apply should not be called in state {}", task);
            return;
        }

        final var persistence = context.getPersistenceProvider();
        final var lastSeq = persistence.getLastSequenceNumber();
        final var persisting = new PersistApply(lastSeq, snapshot, null);

        task = persisting;
        log.debug("lastSequenceNumber prior to persisting applied snapshot: {}", lastSeq);
        context.getPersistenceProvider().saveSnapshot(persisting.request.snapshot());
    }

    @Override
    public void persist(final Snapshot.State snapshotState, final Optional<OutputStream> installSnapshotStream,
            final long totalMemory) {
        if (!(task instanceof Capture(final var lastSeq, final var request))) {
            log.debug("persist should not be called in state {}", this);
            return;
        }

        // create a snapshot object from the state provided and save it when snapshot is saved async,
        // SaveSnapshotSuccess is raised.
        final var snapshot = Snapshot.create(snapshotState, request.getUnAppliedEntries(),
                request.getLastIndex(), request.getLastTerm(),
                request.getLastAppliedIndex(), request.getLastAppliedTerm(),
                context.termInfo(), context.getPeerServerInfo(true));

        context.getPersistenceProvider().saveSnapshot(snapshot);

        log.info("{}: Persisting of snapshot done: {}", persistenceId(), snapshot);

        final var config = context.getConfigParams();
        final long absoluteThreshold = config.getSnapshotDataThreshold();
        final long dataThreshold = absoluteThreshold != 0 ? absoluteThreshold * ConfigParams.MEGABYTE
                : totalMemory * config.getSnapshotDataThresholdPercentage() / 100;

        final var replLog = context.getReplicatedLog();
        final boolean dataSizeThresholdExceeded = replLog.dataSize() > dataThreshold;
        final boolean logSizeExceededSnapshotBatchCount = replLog.size() >= config.getSnapshotBatchCount();

        final var currentBehavior = context.getCurrentBehavior();
        if (dataSizeThresholdExceeded || logSizeExceededSnapshotBatchCount || request.isMandatoryTrim()) {
            if (log.isDebugEnabled()) {
                if (dataSizeThresholdExceeded) {
                    log.debug("{}: log data size {} exceeds the memory threshold {} - doing snapshotPreCommit "
                            + "with index {}", context.getId(), replLog.dataSize(), dataThreshold,
                            request.getLastAppliedIndex());
                } else if (logSizeExceededSnapshotBatchCount) {
                    log.debug("{}: log size {} exceeds the snapshot batch count {} - doing snapshotPreCommit with "
                            + "index {}", context.getId(), replLog.size(), config.getSnapshotBatchCount(),
                            request.getLastAppliedIndex());
                } else {
                    log.debug("{}: user triggered or root overwrite snapshot encountered, trimming log up to "
                            + "last applied index {}", context.getId(), request.getLastAppliedIndex());
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

        log.info("{}: Removed in-memory snapshotted entries, adjusted snaphsotIndex: {} and term: {}",
                context.getId(), replLog.getSnapshotIndex(), replLog.getSnapshotTerm());

        if (installSnapshotStream.isPresent()) {
            // FIXME: ugly cast
            final var snapshotStream = (FileBackedOutputStream) installSnapshotStream.orElseThrow();

            if (context.getId().equals(currentBehavior.getLeaderId())) {
                try {
                    ByteSource snapshotBytes = snapshotStream.asByteSource();
                    currentBehavior.handleMessage(context.getActor(), new SendInstallSnapshot(snapshot, snapshotBytes));
                } catch (IOException e) {
                    log.error("{}: Snapshot install failed due to an unrecoverable streaming error",
                            context.getId(), e);
                }
            } else {
                snapshotStream.cleanup();
            }
        }

        task = new PersistCapture(lastSeq);
    }

    @Override
    public void commit(final long sequenceNumber, final long timeStamp) {
        if (!(task instanceof Persist persist)) {
            log.debug("commit should not be called in state {}", task);
            return;
        }

        log.debug("{}: Snapshot success -  sequence number: {}", persistenceId(), sequenceNumber);
        final var lastSequenceNumber = commit(persist);

        final var persistence = context.getPersistenceProvider();
        persistence.deleteSnapshots(new SnapshotSelectionCriteria(scala.Long.MaxValue(), timeStamp - 1, 0L, 0L));
        persistence.deleteMessages(lastSequenceNumber);

        snapshotComplete();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private long commit(final Persist persist) {
        return switch (persist) {
            case PersistApply(var lastSeq, var apply, var callback) -> {
                // not a nested record pattern to side-step https://github.com/spotbugs/spotbugs/issues/3196
                final var snapshot = apply.snapshot();

                try {
                    // clears the followers log, sets the snapshot index to ensure adjusted-index works
                    context.setReplicatedLog(ReplicatedLogImpl.newInstance(snapshot, context));
                    context.setLastApplied(snapshot.getLastAppliedIndex());
                    context.setCommitIndex(snapshot.getLastAppliedIndex());
                    // FIXME: This may be coming from the leader: we do not want to pollute our TermInfo if it is for
                    //        this term: we may need to know who we voted for in the next elections.
                    //        This behavior means we report as if we voted for the leader.
                    context.setTermInfo(snapshot.termInfo());

                    if (snapshot.getServerConfiguration() != null) {
                        context.updatePeerIds(snapshot.getServerConfiguration());
                    }

                    final var state = snapshot.getState();
                    if (state != null && !(state instanceof EmptyState)) {
                        snapshotCohort.applySnapshot(state);
                    }

                    if (callback != null) {
                        callback.onSuccess();
                    }
                } catch (Exception e) {
                    log.error("{}: Error applying snapshot", context.getId(), e);
                }
                yield lastSeq;
            }
            case PersistCapture(final var lastSeq) -> {
                context.getReplicatedLog().snapshotCommit();
                yield lastSeq;
            }
        };
    }

    @Override
    public void rollback() {
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
                log.info("{}: Replicated Log rolled back. Snapshot will be attempted in the next cycle. "
                        + "snapshotIndex:{}, snapshotTerm:{}, log-size:{}", persistenceId(), replLog.getSnapshotIndex(),
                        replLog.getSnapshotTerm(), replLog.size());
                snapshotComplete();
            }
            default -> log.debug("rollback should not be called in state {}", task);
        }
    }

    private void snapshotComplete() {
        task = Idle.INSTANCE;
        context.getActor().tell(SnapshotComplete.INSTANCE, context.getActor());
    }

    @Override
    public long trimLog(final long desiredTrimIndex) {
        if (!(task instanceof Idle)) {
            log.debug("trimLog should not be called in state {}", task);
            return -1;
        }

        //  we would want to keep the lastApplied as its used while capturing snapshots
        long lastApplied = context.getLastApplied();
        long tempMin = Math.min(desiredTrimIndex, lastApplied > -1 ? lastApplied - 1 : -1);

        if (log.isTraceEnabled()) {
            log.trace("{}: performSnapshotWithoutCapture: desiredTrimIndex: {}, lastApplied: {}, tempMin: {}",
                    persistenceId(), desiredTrimIndex, lastApplied, tempMin);
        }

        if (tempMin > -1) {
            final var replLog = context.getReplicatedLog();
            if (replLog.isPresent(tempMin)) {
                log.debug("{}: fakeSnapshot purging log to {} for term {}", persistenceId(), tempMin,
                    context.currentTerm());

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
        return task instanceof Persist persist ? persist.lastSequenceNumber() : -1;
    }

    @VisibleForTesting
    public @Nullable CaptureSnapshot getCaptureSnapshot() {
        return task instanceof Capture capture ? capture.request : null;
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
    public @NonNull CaptureSnapshot newCaptureSnapshot(final RaftEntryMeta lastLogEntry,
            final long replicatedToAllIndex, final boolean mandatoryTrim) {
        final var replLog = context.getReplicatedLog();
        final var lastAppliedEntry = computeLastAppliedEntry(lastLogEntry);

        final var entry = replLog.get(replicatedToAllIndex);
        final var replicatedToAllEntry = entry != null ? entry : new ImmutableRaftEntryMeta(-1, -1);

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

            log.debug("{}: Capturing Snapshot : lastLogEntry is null. Using snapshot values lastAppliedIndex {} and "
                    + "lastAppliedTerm {} instead.", persistenceId(), lastAppliedIndex, lastAppliedTerm);
        } else {
            lastLogEntryIndex = lastLogEntry.index();
            lastLogEntryTerm = lastLogEntry.term();
        }

        return new CaptureSnapshot(lastLogEntryIndex, lastLogEntryTerm, lastAppliedIndex, lastAppliedTerm,
            replicatedToAllEntry.index(), replicatedToAllEntry.term(), unAppliedEntries, mandatoryTrim);
    }

    @NonNullByDefault
    private RaftEntryMeta computeLastAppliedEntry(final @Nullable RaftEntryMeta lastLogEntry) {
        return computeLastAppliedEntry(context.getReplicatedLog(), context.getLastApplied(), lastLogEntry,
            context.hasFollowers());
    }

    @VisibleForTesting
    @NonNullByDefault
    static RaftEntryMeta computeLastAppliedEntry(final ReplicatedLog log, final long originalIndex,
            final @Nullable RaftEntryMeta lastLogEntry, final boolean hasFollowers) {
        if (hasFollowers) {
            final var entry = log.lookupMeta(originalIndex);
            if (entry != null) {
                return entry;
            }

            final var snapshotIndex = log.getSnapshotIndex();
            if (snapshotIndex > -1) {
                return new ImmutableRaftEntryMeta(snapshotIndex, log.getSnapshotTerm());
            }
        } else if (lastLogEntry != null) {
            // since we have persisted the last-log-entry to persistent journal before the capture, we would want
            // to snapshot from this entry.
            return lastLogEntry;
        }

        return new ImmutableRaftEntryMeta(-1, -1);
    }
}
