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
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.RaftEntryMeta;
import org.slf4j.Logger;

/**
 * Manages the capturing of snapshots for a RaftActor.
 *
 * @author Moiz Raja
 * @author Thomas Pantelis
 */
public class SnapshotManager implements SnapshotState {

    private sealed interface Task {
        // Nothing else
    }

    @NonNullByDefault
    private static final class Idle implements Task {
        private static final Idle INSTANCE = new Idle();
    }

    private sealed interface Persist extends Task {

        long lastSequenceNumber();
    }

    private record PersistApply(@NonNull ApplySnapshot applySnapshot, long lastSequenceNumber) implements Persist {

    }

    private record PersistCapture(@Nullable ApplySnapshot applySnapshot, long lastSequenceNumber) implements Persist {

    }

    @NonNullByDefault
    private record Capturing(CaptureSnapshot captureSnapshot, long lastSequenceNumber) implements Task {
        Capturing {
            requireNonNull(captureSnapshot);
        }
    }

    private final Logger log;
    private final RaftActorContext context;
    private final LastAppliedTermInformationReader lastAppliedTermInformationReader =
            new LastAppliedTermInformationReader();

    private @NonNull Task behavior = Idle.INSTANCE;
    private Consumer<Optional<OutputStream>> createSnapshotProcedure = null;
    private RaftActorSnapshotCohort snapshotCohort = NoopRaftActorSnapshotCohort.INSTANCE;

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
        return applySnapshot != null;
    }

    @Override
    public boolean isCapturing() {
        return !(behavior instanceof Idle);
    }

    @Override
    public boolean capture(final RaftEntryMeta lastLogEntry, final long replicatedToAllIndex) {
        if (behavior instanceof Idle) {
            return capture(lastLogEntry, replicatedToAllIndex, null, false);
        }
        log.debug("capture should not be called in state {}", behavior);
        return false;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private boolean capture(final RaftEntryMeta lastLogEntry, final long replicatedToAllIndex,
            final String targetFollower, final boolean mandatoryTrim) {
        final var captureSnapshot = newCaptureSnapshot(lastLogEntry, replicatedToAllIndex, mandatoryTrim);

        final OutputStream installSnapshotStream;
        if (targetFollower != null) {
            installSnapshotStream = context.getFileBackedOutputStreamFactory().newInstance();
            log.info("{}: Initiating snapshot capture {} to install on {}",
                    persistenceId(), captureSnapshot, targetFollower);
        } else {
            installSnapshotStream = null;
            log.info("{}: Initiating snapshot capture {}", persistenceId(), captureSnapshot);
        }

        final var lastSeq = context.getPersistenceProvider().getLastSequenceNumber();

        log.debug("{}: lastSequenceNumber prior to capture: {}", persistenceId(), lastSeq);

        behavior = new Capturing(captureSnapshot, lastSeq);

        try {
            createSnapshotProcedure.accept(Optional.ofNullable(installSnapshotStream));
        } catch (Exception e) {
            behavior = Idle.INSTANCE;
            log.error("Error creating snapshot", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean captureToInstall(final RaftEntryMeta lastLogEntry, final long replicatedToAllIndex,
            final String targetFollower) {
        if (behavior instanceof Idle) {
            return capture(lastLogEntry, replicatedToAllIndex, targetFollower, false);
        }
        log.debug("captureToInstall should not be called in state {}", behavior);
        return false;
    }

    @Override
    public boolean captureWithForcedTrim(final RaftEntryMeta lastLogEntry, final long replicatedToAllIndex) {
        if (behavior instanceof Idle) {
            return capture(lastLogEntry, replicatedToAllIndex, null, true);
        }
        log.debug("captureWithForcedTrim should not be called in state {}", behavior);
        return false;
    }

    @Override
    public void apply(final ApplySnapshot snapshot) {
        if (!(behavior instanceof Idle)) {
            log.debug("apply should not be called in state {}", behavior);
            return;
        }

        final var persistence = context.getPersistenceProvider();
        final var lastSeq = persistence.getLastSequenceNumber();
        final var persisting = new PersistApply(snapshot, lastSeq);

        behavior = persisting;
        log.debug("lastSequenceNumber prior to persisting applied snapshot: {}", lastSeq);
        context.getPersistenceProvider().saveSnapshot(persisting.applySnapshot.snapshot());
    }

    @Override
    public void persist(final Snapshot.State snapshotState, final Optional<OutputStream> installSnapshotStream,
            final long totalMemory) {
        if (!(behavior instanceof Capturing creating)) {
            log.debug("persist should not be called in state {}", this);
            return;
        }

        // create a snapshot object from the state provided and save it when snapshot is saved async,
        // SaveSnapshotSuccess is raised.
        final var captureSnapshot = creating.captureSnapshot;

        final var snapshot = Snapshot.create(snapshotState,
                captureSnapshot.getUnAppliedEntries(),
                captureSnapshot.getLastIndex(), captureSnapshot.getLastTerm(),
                captureSnapshot.getLastAppliedIndex(), captureSnapshot.getLastAppliedTerm(),
                context.termInfo(), context.getPeerServerInfo(true));

        context.getPersistenceProvider().saveSnapshot(snapshot);

        log.info("{}: Persisting of snapshot done: {}", persistenceId(), snapshot);

        final var config = context.getConfigParams();
        final long absoluteThreshold = config.getSnapshotDataThreshold();
        final long dataThreshold = absoluteThreshold != 0 ? absoluteThreshold * ConfigParams.MEGABYTE
                : totalMemory * config.getSnapshotDataThresholdPercentage() / 100;

        final boolean dataSizeThresholdExceeded = context.getReplicatedLog().dataSize() > dataThreshold;
        final boolean logSizeExceededSnapshotBatchCount =
                context.getReplicatedLog().size() >= config.getSnapshotBatchCount();

        final var currentBehavior = context.getCurrentBehavior();
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

        behavior = new PersistCapture( );
    }

    @Override
    public void commit(final long sequenceNumber, final long timeStamp) {
        if (!(behavior instanceof PersistApply persisting)) {
            log.debug("commit should not be called in state {}", behavior);
            return;
        }

        log.debug("{}: Snapshot success -  sequence number: {}", persistenceId(), sequenceNumber);
        final var applySnapshot = persisting.applySnapshot;

        if (applySnapshot != null) {
            try {
                Snapshot snapshot = applySnapshot.snapshot();

                //clears the followers log, sets the snapshot index to ensure adjusted-index works
                context.setReplicatedLog(ReplicatedLogImpl.newInstance(snapshot, context));
                context.setLastApplied(snapshot.getLastAppliedIndex());
                context.setCommitIndex(snapshot.getLastAppliedIndex());
                context.setTermInfo(snapshot.termInfo());

                if (snapshot.getServerConfiguration() != null) {
                    context.updatePeerIds(snapshot.getServerConfiguration());
                }

                if (!(snapshot.getState() instanceof EmptyState)) {
                    snapshotCohort.applySnapshot(snapshot.getState());
                }

                applySnapshot.callback().onSuccess();
            } catch (Exception e) {
                log.error("{}: Error applying snapshot", context.getId(), e);
            }
        } else {
            context.getReplicatedLog().snapshotCommit();
        }

        final var persistence = context.getPersistenceProvider();
        persistence.deleteSnapshots(new SnapshotSelectionCriteria(scala.Long.MaxValue(), timeStamp - 1, 0L, 0L));
        persistence.deleteMessages(persisting.lastSequenceNumber);

        snapshotComplete();
    }

    @Override
    public void rollback() {
        if (!(behavior instanceof PersistApply persisting)) {
            log.debug("rollback should not be called in state {}", this);
            return;
        }

        // Nothing to rollback if we're applying a snapshot from the leader.
        final var applySnapshot = persisting.applySnapshot;
        if (applySnapshot == null) {
            final var replLog = context.getReplicatedLog();
            replLog.snapshotRollback();
            log.info("{}: Replicated Log rolled back. Snapshot will be attempted in the next cycle. "
                    + "snapshotIndex:{}, snapshotTerm:{}, log-size:{}", persistenceId(), replLog.getSnapshotIndex(),
                    replLog.getSnapshotTerm(), replLog.size());
        } else {
            applySnapshot.callback().onFailure();
        }

        snapshotComplete();
    }

    private void snapshotComplete() {
        behavior = Idle.INSTANCE;
        context.getActor().tell(SnapshotComplete.INSTANCE, context.getActor());
    }

    @Override
    public long trimLog(final long desiredTrimIndex) {
        if (!(behavior instanceof Idle)) {
            log.debug("trimLog should not be called in state {}", behavior);
            return -1;
        }

        //  we would want to keep the lastApplied as its used while capturing snapshots
        long lastApplied = context.getLastApplied();
        long tempMin = Math.min(desiredTrimIndex, lastApplied > -1 ? lastApplied - 1 : -1);

        if (log.isTraceEnabled()) {
            log.trace("{}: performSnapshotWithoutCapture: desiredTrimIndex: {}, lastApplied: {}, tempMin: {}",
                    persistenceId(), desiredTrimIndex, lastApplied, tempMin);
        }

        if (tempMin > -1 && context.getReplicatedLog().isPresent(tempMin)) {
            log.debug("{}: fakeSnapshot purging log to {} for term {}", persistenceId(), tempMin,
                    context.currentTerm());

            //use the term of the temp-min, since we check for isPresent, entry will not be null
            ReplicatedLogEntry entry = context.getReplicatedLog().get(tempMin);
            context.getReplicatedLog().snapshotPreCommit(tempMin, entry.term());
            context.getReplicatedLog().snapshotCommit(false);
            return tempMin;
        }

        final var currentBehavior = context.getCurrentBehavior();
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
        return behavior instanceof Persist persist ? persist.lastSequenceNumber() : -1;
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
    public @NonNull CaptureSnapshot newCaptureSnapshot(final RaftEntryMeta lastLogEntry,
            final long replicatedToAllIndex, final boolean mandatoryTrim) {
        final var lastAppliedTermInfoReader = lastAppliedTermInformationReader.init(context.getReplicatedLog(),
            context.getLastApplied(), lastLogEntry, hasFollowers());

        long lastAppliedIndex = lastAppliedTermInfoReader.getIndex();
        long lastAppliedTerm = lastAppliedTermInfoReader.getTerm();

        final long newReplicatedToAllIndex;
        final long newReplicatedToAllTerm;
        final var entry = context.getReplicatedLog().get(replicatedToAllIndex);
        if (entry != null) {
            newReplicatedToAllIndex = entry.index();
            newReplicatedToAllTerm = entry.term();
        } else {
            newReplicatedToAllIndex = -1L;
            newReplicatedToAllTerm = -1L;
        }

        final var unAppliedEntries = context.getReplicatedLog().getFrom(lastAppliedIndex + 1);

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
            lastLogEntryIndex = lastLogEntry.index();
            lastLogEntryTerm = lastLogEntry.term();
        }

        return new CaptureSnapshot(lastLogEntryIndex, lastLogEntryTerm, lastAppliedIndex, lastAppliedTerm,
                newReplicatedToAllIndex, newReplicatedToAllTerm, unAppliedEntries, mandatoryTrim);
    }

    @VisibleForTesting
    static final class LastAppliedTermInformationReader {
        private long index;
        private long term;

        LastAppliedTermInformationReader init(final ReplicatedLog log, final long originalIndex,
                final @Nullable RaftEntryMeta lastLogEntry, final boolean hasFollowers) {
            RaftEntryMeta entry = log.lookupMeta(originalIndex);
            index = -1L;
            term = -1L;
            if (!hasFollowers) {
                if (lastLogEntry != null) {
                    // since we have persisted the last-log-entry to persistent journal before the capture,
                    // we would want to snapshot from this entry.
                    index = lastLogEntry.index();
                    term = lastLogEntry.term();
                }
            } else if (entry != null) {
                index = entry.index();
                term = entry.term();
            } else if (log.getSnapshotIndex() > -1) {
                index = log.getSnapshotIndex();
                term = log.getSnapshotTerm();
            }
            return this;
        }

        long getIndex() {
            return index;
        }

        long getTerm() {
            return term;
        }
    }
}
