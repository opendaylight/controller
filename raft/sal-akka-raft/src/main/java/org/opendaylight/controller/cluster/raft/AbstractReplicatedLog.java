/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.AbstractBaseLog;
import org.opendaylight.controller.cluster.raft.spi.BaseLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class handling the mapping of
 * logical LogEntry Index and the physical list index.
 */
public abstract class AbstractReplicatedLog<T extends ReplicatedLogEntry> extends AbstractBaseLog<@NonNull T>
        implements ReplicatedLog {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractReplicatedLog.class);

    // to be used for rollback during save snapshot failure
    private ArrayList<T> snapshottedJournal;
    private long previousSnapshotIndex = -1;
    private long previousSnapshotTerm = -1;

    protected AbstractReplicatedLog(final @NonNull String memberId) {
        super(memberId);
    }

    private void clearRollback() {
        previousSnapshotTerm = previousSnapshotIndex = -1;
        snapshottedJournal = null;
    }

    @Override
    protected final void resetToLog(final BaseLog prev) {
        clearRollback();
        super.resetToLog(prev);
    }

    @Override
    public final void resetToSnapshot(final Snapshot snapshot) {
        clearRollback();
        super.resetToSnapshot(snapshot);
    }

    @Override
    public final T lookup(final long logEntryIndex) {
        final int adjustedIndex = adjustedIndex(logEntryIndex);

        if (adjustedIndex < 0 || adjustedIndex >= journal.size()) {
            // physical index should be less than list size and >= 0
            return null;
        }

        return journal.get(adjustedIndex);
    }

    @Override
    public final StoredEntryMeta lookupStoredMeta(final long index) {
        final var entry = lookup(index);
        return entry == null ? null : new StoredEntryMeta(entry, !entry.isPersistencePending());
    }

    @Override
    public final void increaseJournalLogCapacity(final int amount) {
        journal.ensureCapacity(journal.size() + amount);
    }

    @Override
    public final List<ReplicatedLogEntry> getFrom(final long logEntryIndex) {
        return getFrom(logEntryIndex, journal.size(), -1);
    }

    @Override
    public final List<ReplicatedLogEntry> getFrom(final long logEntryIndex, final int maxEntries,
            final long maxDataSize) {
        int adjustedIndex = adjustedIndex(logEntryIndex);
        int size = journal.size();
        if (adjustedIndex < 0 || adjustedIndex >= size) {
            return List.of();
        }

        // physical index should be less than list size and >= 0
        int maxIndex = adjustedIndex + maxEntries;
        if (maxIndex > size) {
            maxIndex = size;
        }

        return maxDataSize < 0 ? new ArrayList<>(journal.subList(adjustedIndex, maxIndex))
            : copyJournalEntries(adjustedIndex, maxIndex, maxDataSize);
    }

    private @NonNull List<ReplicatedLogEntry> copyJournalEntries(final int fromIndex, final int toIndex,
            final long maxDataSize) {
        final var retList = new ArrayList<ReplicatedLogEntry>(toIndex - fromIndex);
        long totalSize = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            final var entry = journal.get(i);
            totalSize += entry.serializedSize();
            if (totalSize > maxDataSize) {
                if (retList.isEmpty()) {
                    // Edge case - the first entry's size exceeds the threshold. We need to return
                    // at least the first entry so add it here.
                    retList.add(entry);
                }
                break;
            }

            retList.add(entry);
        }

        return retList;
    }

    // Non-final for testing
    @Override
    public int dataSize() {
        return dataSize;
    }

    @Override
    public final boolean isPresent(final long logEntryIndex) {
        if (logEntryIndex > lastIndex()) {
            // if the request logical index is less than the last present in the list
            return false;
        }
        return adjustedIndex(logEntryIndex) >= 0;
    }

    @Override
    public final boolean isInSnapshot(final long logEntryIndex) {
        return logEntryIndex >= 0 && logEntryIndex <= snapshotIndex && snapshotIndex != -1;
    }

    @Override
    public final void clear(final int startIndex, final int endIndex) {
        journal.subList(startIndex, endIndex).clear();
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public final void clear() {
        // Note: this could be optimized, but it's going away anyway
        removeFrom(0);
    }

    @Override
    public final void snapshotPreCommit(final long snapshotCapturedIndex, final long snapshotCapturedTerm) {
        if (snapshotCapturedIndex < snapshotIndex) {
            throw new IllegalArgumentException("snapshotCapturedIndex must be greater than or equal to snapshotIndex");
        }

        snapshottedJournal = new ArrayList<>(journal.size());

        final var snapshotJournalEntries = journal.subList(0, (int) (snapshotCapturedIndex - snapshotIndex));

        snapshottedJournal.addAll(snapshotJournalEntries);
        snapshotJournalEntries.clear();

        previousSnapshotIndex = snapshotIndex;
        setSnapshotIndex(snapshotCapturedIndex);

        previousSnapshotTerm = snapshotTerm;
        setSnapshotTerm(snapshotCapturedTerm);
    }

    @Override
    public final void snapshotCommit(final boolean updateDataSize) {
        snapshottedJournal = null;
        previousSnapshotIndex = -1;
        previousSnapshotTerm = -1;

        if (updateDataSize) {
            // need to recalc the datasize based on the entries left after precommit.
            int newDataSize = 0;
            for (var logEntry : journal) {
                newDataSize += logEntry.size();
            }
            LOG.trace("{}: Updated dataSize from {} to {}", memberId, dataSize, newDataSize);
            dataSize = newDataSize;
        }
    }

    @Override
    public final void snapshotRollback() {
        snapshottedJournal.addAll(journal);
        journal = snapshottedJournal;
        snapshottedJournal = null;

        snapshotIndex = previousSnapshotIndex;
        previousSnapshotIndex = -1;

        snapshotTerm = previousSnapshotTerm;
        previousSnapshotTerm = -1;
    }

    @VisibleForTesting
    final ReplicatedLogEntry getAtPhysicalIndex(final int index) {
        return journal.get(index);
    }
}
