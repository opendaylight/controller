/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Throwables;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;
import org.opendaylight.raft.api.EntryMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ReplicatedLog used by the RaftActor.
 */
final class ReplicatedLogImpl extends AbstractReplicatedLog<JournaledLogEntry> {
    /**
     * A simplified {@link RaftCallback} implementation rethrowing any errors as unchecked exceptions.
     */
    @NonNullByDefault
    private abstract static class UncheckedPersistCallback extends RaftCallback<Long> {
        @Override
        public final void invoke(final @Nullable Exception failure, final Long success) {
            if (failure == null) {
                invoke(success);
                return;
            }

            Throwables.throwIfUnchecked(failure);
            throw failure instanceof IOException e ? new UncheckedIOException("Failed to store entry", e)
                : new IllegalStateException("Failed to store entry", failure);
        }

        /**
         * Invoke the callback.
         *
         * @param serializedCommandSize the serialized size of the command
         */
        abstract void invoke(long serializedCommandSize);
    }

    @NonNullByDefault
    private abstract static class AppendCallback<T extends LogEntry> extends UncheckedPersistCallback {
        final Consumer<T> callback;
        final JournaledLogEntry entry;

        AppendCallback(final JournaledLogEntry entry, final Consumer<T> callback) {
            this.entry = requireNonNull(entry);
            this.callback = requireNonNull(callback);
        }

        @Override
        protected ToStringHelper addToStringAttributes(final ToStringHelper helper) {
            return helper.add("entry", entry).add("callback", callback);
        }
    }

    @NonNullByDefault
    private final class AppendReceivedCallback extends AppendCallback<LogEntry> {
        private final LogEntry userEntry;

        AppendReceivedCallback(final JournaledLogEntry entry, final Consumer<LogEntry> callback,
                final LogEntry userEntry) {
            super(entry, callback);
            this.userEntry = requireNonNull(userEntry);
        }

        @Override
        void invoke(final long serializedCommandSize) {
            invokeSync(entry, () -> callback.accept(userEntry));
        }
    }

    @NonNullByDefault
    private final class AppendSubmittedCallback extends AppendCallback<ReplicatedLogEntry> {
        AppendSubmittedCallback(final JournaledLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
            super(entry, callback);
        }

        @Override
        void invoke(final long serializedCommandSize) {
            invokeSync(entry, () -> {
                entry.clearPersistencePending();
                callback.accept(entry);
            });
        }
    }

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
            context.entryStore().discardTail(adjustedIndex + firstJournalIndex());
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
        }
        return dataSize();
    }

    @Override
    public boolean appendReceived(final LogEntry entry, final Consumer<LogEntry> callback) {
        requireNonNull(callback);
        LOG.debug("{}: Append log entry and persist {} ", memberId, entry);

        // FIXME: When can 'false' happen? Wouldn't that be an indication that Follower.handleAppendEntries() is doing
        //        something wrong?
        final var adopted = adoptEntry(entry);

        if (appendImpl(adopted)) {
            // FIXME: do not pass 'entry' when Follower behavior does not need 'entry' identity
            context.entryStore().persistEntry(adopted, new AppendReceivedCallback(adopted, callback, entry));
        }
        return shouldCaptureSnapshot(adopted.index());
    }

    @Override
    public boolean appendSubmitted(final long index, final long term, final Payload command,
            final Consumer<ReplicatedLogEntry> callback)  {
        requireNonNull(callback);
        final var entry = JournaledLogEntry.pendingOf(index, term, command);
        LOG.debug("{}: Append log entry and persist {} ", memberId, entry);

        final var ret = appendImpl(entry);
        if (ret) {
            context.entryStore().startPersistEntry(entry, new AppendSubmittedCallback(entry, callback));
        }
        return ret;
    }

    @NonNullByDefault
    private void invokeSync(final ReplicatedLogEntry entry, final @Nullable Runnable callback) {
        LOG.debug("{}: persist complete {}", memberId, entry);

        dataSizeSinceLastSnapshot += entry.size();

        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public void markLastApplied() {
        context.entryStore().checkpointLastApplied(lastAppliedJournalIndex());
    }

    @Override
    protected JournaledLogEntry adoptEntry(final LogEntry entry) {
        return JournaledLogEntry.of(entry);
    }
}
