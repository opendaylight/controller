/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.raft.api.EntryMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ReplicatedLog used by the RaftActor.
 */
final class ReplicatedLogImpl extends AbstractReplicatedLog<SimpleReplicatedLogEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(ReplicatedLogImpl.class);

    private static final int DATA_SIZE_DIVIDER = 5;

    private final RaftActorContext context;

    private long dataSizeSinceLastSnapshot = 0L;

    ReplicatedLogImpl(final RaftActorContext context) {
        super(context.getId());
        this.context = context;
    }

    @Override
    public boolean trimToReceive(final long fromIndex) {
        long adjustedIndex = removeFrom(fromIndex);
        if (adjustedIndex >= 0) {
            context.entryStore().deleteEntries(fromIndex);
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldCaptureSnapshot(final long logIndex) {
        final var config = context.getConfigParams();
        if ((logIndex + 1) % config.getSnapshotBatchCount() == 0) {
            return true;
        }

        final long absoluteThreshold = config.getSnapshotDataThreshold();
        final long dataThreshold = absoluteThreshold != 0 ? absoluteThreshold * 1_048_576
                : context.getTotalMemory() * config.getSnapshotDataThresholdPercentage() / 100;
        return getDataSizeForSnapshotCheck() > dataThreshold;
    }

    @Override
    public void captureSnapshotIfReady(final EntryMeta lastEntry) {
        if (shouldCaptureSnapshot(lastEntry.index())) {
            boolean started = context.getSnapshotManager().capture(lastEntry,
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
    public boolean appendReceived(final LogEntry entry, final Consumer<LogEntry> callback) {
        LOG.debug("{}: Append log entry and persist {} ", memberId, entry);

        // FIXME: When can 'false' happen? Wouldn't that be an indication that Follower.handleAppendEntries() is doing
        //        something wrong?
        final var adopted = adoptEntry(entry);
        if (appendImpl(adopted)) {
            context.entryStore().persistEntry(adopted, persisted -> invokeSync(persisted, callback));
        }
        return shouldCaptureSnapshot(adopted.index());
    }

    @Override
    public boolean appendSubmitted(final long index, final long term, final Payload command,
            final Consumer<ReplicatedLogEntry> callback)  {
        final var entry = new SimpleReplicatedLogEntry(index, term, command);
        entry.setPersistencePending(true);
        LOG.debug("{}: Append log entry and persist {} ", memberId, entry);

        final var ret = appendImpl(entry);
        if (ret) {
            context.entryStore().startPersistEntry(entry, unused -> {
                entry.setPersistencePending(false);
                invokeAsync(entry, callback);
            });
        }
        return ret;
    }

    private <T extends ReplicatedLogEntry> void invokeAsync(final @NonNull T entry,
            final @Nullable Consumer<T> callback) {
        context.getExecutor().execute(() -> invokeSync(entry, callback));
    }

    private <T extends ReplicatedLogEntry> void invokeSync(final @NonNull T entry,
            final @Nullable Consumer<? super T> callback) {
        LOG.debug("{}: persist complete {}", memberId, entry);

        dataSizeSinceLastSnapshot += entry.size();

        if (callback != null) {
            callback.accept(entry);
        }
    }

    @Override
    public void markLastApplied() {
        context.entryStore().markLastApplied(getLastApplied());
    }

    @Override
    protected SimpleReplicatedLogEntry adoptEntry(final LogEntry entry) {
        return SimpleReplicatedLogEntry.of(entry);
    }
}
