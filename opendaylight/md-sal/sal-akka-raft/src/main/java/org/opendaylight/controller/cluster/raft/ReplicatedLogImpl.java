/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.japi.Procedure;
import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Implementation of ReplicatedLog used by the RaftActor.
 */
class ReplicatedLogImpl extends AbstractReplicatedLogImpl {
    private static final int DATA_SIZE_DIVIDER = 5;

    private final RaftActorContext context;
    private long dataSizeSinceLastSnapshot = 0L;

    private ReplicatedLogImpl(final long snapshotIndex, final long snapshotTerm,
            final List<ReplicatedLogEntry> unAppliedEntries,
            final RaftActorContext context) {
        super(snapshotIndex, snapshotTerm, unAppliedEntries, context.getId());
        this.context = Preconditions.checkNotNull(context);
    }

    static ReplicatedLog newInstance(final Snapshot snapshot, final RaftActorContext context) {
        return new ReplicatedLogImpl(snapshot.getLastAppliedIndex(), snapshot.getLastAppliedTerm(),
                snapshot.getUnAppliedEntries(), context);
    }

    static ReplicatedLog newInstance(final RaftActorContext context) {
        return new ReplicatedLogImpl(-1L, -1L, Collections.<ReplicatedLogEntry>emptyList(), context);
    }

    @Override
    public boolean removeFromAndPersist(final long logEntryIndex) {
        // FIXME: Maybe this should be done after the command is saved
        long adjustedIndex = removeFrom(logEntryIndex);
        if (adjustedIndex >= 0) {
            context.getPersistenceProvider().persist(new DeleteEntries(adjustedIndex), NoopProcedure.instance());
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldCaptureSnapshot(long logIndex) {
        final ConfigParams config = context.getConfigParams();
        final long journalSize = logIndex + 1;
        final long dataThreshold = context.getTotalMemory() * config.getSnapshotDataThresholdPercentage() / 100;

        return journalSize % config.getSnapshotBatchCount() == 0 || getDataSizeForSnapshotCheck() > dataThreshold;
    }

    @Override
    public void captureSnapshotIfReady(final ReplicatedLogEntry replicatedLogEntry) {
        if (shouldCaptureSnapshot(replicatedLogEntry.getIndex())) {
            boolean started = context.getSnapshotManager().capture(replicatedLogEntry,
                    context.getCurrentBehavior().getReplicatedToAllIndex());
            if (started && !context.hasFollowers()) {
                dataSizeSinceLastSnapshot = 0;
            }
        }
    }

    private long getDataSizeForSnapshotCheck() {
        if (!context.hasFollowers()) {
            // When we do not have followers we do not maintain an in-memory log
            // due to this the journalSize will never become anything close to the
            // snapshot batch count. In fact will mostly be 1.
            // Similarly since the journal's dataSize depends on the entries in the
            // journal the journal's dataSize will never reach a value close to the
            // memory threshold.
            // By maintaining the dataSize outside the journal we are tracking essentially
            // what we have written to the disk however since we no longer are in
            // need of doing a snapshot just for the sake of freeing up memory we adjust
            // the real size of data by the DATA_SIZE_DIVIDER so that we do not snapshot as often
            // as if we were maintaining a real snapshot
            return dataSizeSinceLastSnapshot / DATA_SIZE_DIVIDER;
        } else {
            return dataSize();
        }
    }

    @Override
    public boolean appendAndPersist(@Nonnull final ReplicatedLogEntry replicatedLogEntry,
            @Nullable final Procedure<ReplicatedLogEntry> callback, boolean doAsync)  {

        context.getLogger().debug("{}: Append log entry and persist {} ", context.getId(), replicatedLogEntry);

        if (!append(replicatedLogEntry)) {
            return false;
        }

        Procedure<ReplicatedLogEntry> persistCallback = persistedLogEntry -> {
            context.getLogger().debug("{}: persist complete {}", context.getId(), persistedLogEntry);

            dataSizeSinceLastSnapshot += persistedLogEntry.size();

            if (callback != null) {
                callback.apply(persistedLogEntry);
            }
        };

        if (doAsync) {
            context.getPersistenceProvider().persistAsync(replicatedLogEntry, persistCallback);
        } else {
            context.getPersistenceProvider().persist(replicatedLogEntry, persistCallback);
        }

        return true;
    }
}
