/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.util.List;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.DeleteEntries;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.RaftEntryMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ReplicatedLog used by the RaftActor.
 */
final class ReplicatedLogImpl extends AbstractReplicatedLog {
    private static final Logger LOG = LoggerFactory.getLogger(ReplicatedLogImpl.class);

    private static final int DATA_SIZE_DIVIDER = 5;

    private final RaftActorContext context;
    private long dataSizeSinceLastSnapshot = 0L;

    private ReplicatedLogImpl(final RaftActorContext context, final long snapshotIndex, final long snapshotTerm,
            final List<ReplicatedLogEntry> unAppliedEntries) {
        super(context.getId(), snapshotIndex, snapshotTerm, unAppliedEntries);
        this.context = context;
    }

    ReplicatedLogImpl(final RaftActorContext context) {
        this(context, -1, -1, List.of());
    }

    ReplicatedLogImpl(final RaftActorContext context, final Snapshot snapshot) {
        this(context, snapshot.getLastAppliedIndex(), snapshot.getLastAppliedTerm(), snapshot.getUnAppliedEntries());
    }

    @Override
    public boolean removeFromAndPersist(final long logEntryIndex) {
        long adjustedIndex = removeFrom(logEntryIndex);
        if (adjustedIndex >= 0) {
            context.getPersistenceProvider().persist(new DeleteEntries(logEntryIndex), unused -> { });
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldCaptureSnapshot(final long logIndex) {
        final ConfigParams config = context.getConfigParams();
        if ((logIndex + 1) % config.getSnapshotBatchCount() == 0) {
            return true;
        }

        final long absoluteThreshold = config.getSnapshotDataThreshold();
        final long dataThreshold = absoluteThreshold != 0 ? absoluteThreshold * ConfigParams.MEGABYTE
                : context.getTotalMemory() * config.getSnapshotDataThresholdPercentage() / 100;
        return getDataSizeForSnapshotCheck() > dataThreshold;
    }

    @Override
    public void captureSnapshotIfReady(final RaftEntryMeta replicatedLogEntry) {
        if (shouldCaptureSnapshot(replicatedLogEntry.index())) {
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
    public <T extends ReplicatedLogEntry> boolean appendAndPersist(final T replicatedLogEntry,
            final Consumer<T> callback, final boolean doAsync)  {
        LOG.debug("{}: Append log entry and persist {} ", memberId, replicatedLogEntry);

        if (!append(replicatedLogEntry)) {
            return false;
        }

        final var provider = context.getPersistenceProvider();
        if (doAsync) {
            provider.persistAsync(replicatedLogEntry, entry -> persistCallback(entry, callback));
        } else {
            provider.persist(replicatedLogEntry, entry -> syncPersistCallback(entry, callback));
        }
        return true;
    }

    private <T extends ReplicatedLogEntry> void persistCallback(final @NonNull T persistedLogEntry,
            final @Nullable Consumer<T> callback) {
        context.getExecutor().execute(() -> syncPersistCallback(persistedLogEntry, callback));
    }

    private <T extends ReplicatedLogEntry> void syncPersistCallback(final @NonNull T persistedLogEntry,
            final @Nullable Consumer<T> callback) {
        LOG.debug("{}: persist complete {}", memberId, persistedLogEntry);

        dataSizeSinceLastSnapshot += persistedLogEntry.size();

        if (callback != null) {
            callback.accept(persistedLogEntry);
        }
    }
}
