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
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.spi.ImmutableRaftEntryMeta;
import org.opendaylight.controller.cluster.raft.spi.RaftEntryMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class handling the mapping of
 * logical LogEntry Index and the physical list index.
 */
public abstract class AbstractReplicatedLog implements ReplicatedLog {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractReplicatedLog.class);

    final @NonNull String memberId;

    // We define this as ArrayList so we can use ensureCapacity.
    private ArrayList<ReplicatedLogEntry> journal;
    private ImmutableRaftEntryMeta snapshotEntry;

    // to be used for rollback during save snapshot failure
    private ArrayList<ReplicatedLogEntry> snapshottedJournal;
    private ImmutableRaftEntryMeta previousSnapshotEntry;
    private int dataSize = 0;

    protected AbstractReplicatedLog(final @NonNull String memberId, final @Nullable RaftEntryMeta snapshotEntry,
            final List<ReplicatedLogEntry> unAppliedEntries) {
        this.memberId = requireNonNull(memberId);
        this.snapshotEntry = snapshotEntry != null ? ImmutableRaftEntryMeta.copyOf(snapshotEntry) : null;

        journal = new ArrayList<>(unAppliedEntries.size());
        for (var entry : unAppliedEntries) {
            append(entry);
        }
    }

    protected final int adjustedIndex(final long logEntryIndex) {
        final var local = snapshotEntry;
        return (int) (local == null ? logEntryIndex : logEntryIndex - local.index() - 1);
    }

    @Override
    public final ReplicatedLogEntry get(final long logEntryIndex) {
        int adjustedIndex = adjustedIndex(logEntryIndex);

        if (adjustedIndex < 0 || adjustedIndex >= journal.size()) {
            // physical index should be less than list size and >= 0
            return null;
        }

        return journal.get(adjustedIndex);
    }

    @Override
    public final ReplicatedLogEntry last() {
        if (journal.isEmpty()) {
            return null;
        }
        // get the last entry directly from the physical index
        return journal.get(journal.size() - 1);
    }

    @Override
    public final RaftEntryMeta lastMeta() {
        return last();
    }

    @Override
    public final long lastIndex() {
        final var last = last();
        // it can happen that after snapshot, all the entries of the journal are trimmed till lastApplied,
        // so lastIndex = snapshotIndex
        return last != null ? last.index() : getSnapshotIndex();
    }

    @Override
    public final long lastTerm() {
        final var last = last();
        // it can happen that after snapshot, all the entries of the journal are trimmed till lastApplied,
        // so lastTerm = snapshotTerm
        return last != null ? last.term() : getSnapshotTerm();
    }

    @Override
    public long removeFrom(final long logEntryIndex) {
        int adjustedIndex = adjustedIndex(logEntryIndex);
        if (adjustedIndex < 0 || adjustedIndex >= journal.size()) {
            // physical index should be less than list size and >= 0
            return -1;
        }

        for (int i = adjustedIndex; i < journal.size(); i++) {
            dataSize -= journal.get(i).size();
        }

        journal.subList(adjustedIndex , journal.size()).clear();

        return adjustedIndex;
    }

    @Override
    public boolean append(final ReplicatedLogEntry replicatedLogEntry) {
        final var entryIndex = replicatedLogEntry.index();
        final var lastIndex = lastIndex();
        if (entryIndex > lastIndex) {
            journal.add(replicatedLogEntry);
            dataSize += replicatedLogEntry.size();
            return true;
        }

        LOG.warn("{}: Cannot append new entry - new index {} is not greater than the last index {}", memberId,
            entryIndex, lastIndex, new Exception("stack trace"));
        return false;
    }

    @Override
    public void increaseJournalLogCapacity(final int amount) {
        journal.ensureCapacity(journal.size() + amount);
    }

    @Override
    public List<ReplicatedLogEntry> getFrom(final long logEntryIndex) {
        return getFrom(logEntryIndex, journal.size(), NO_MAX_SIZE);
    }

    @Override
    public List<ReplicatedLogEntry> getFrom(final long logEntryIndex, final int maxEntries, final long maxDataSize) {
        int adjustedIndex = adjustedIndex(logEntryIndex);
        int size = journal.size();
        if (adjustedIndex >= 0 && adjustedIndex < size) {
            // physical index should be less than list size and >= 0
            int maxIndex = adjustedIndex + maxEntries;
            if (maxIndex > size) {
                maxIndex = size;
            }

            if (maxDataSize == NO_MAX_SIZE) {
                return new ArrayList<>(journal.subList(adjustedIndex, maxIndex));
            } else {
                return copyJournalEntries(adjustedIndex, maxIndex, maxDataSize);
            }
        } else {
            return List.of();
        }
    }

    private @NonNull List<ReplicatedLogEntry> copyJournalEntries(final int fromIndex, final int toIndex,
            final long maxDataSize) {
        final var retList = new ArrayList<ReplicatedLogEntry>(toIndex - fromIndex);
        long totalSize = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            final var entry = journal.get(i);
            totalSize += entry.serializedSize();
            if (totalSize <= maxDataSize) {
                retList.add(entry);
            } else {
                if (retList.isEmpty()) {
                    // Edge case - the first entry's size exceeds the threshold. We need to return
                    // at least the first entry so add it here.
                    retList.add(entry);
                }

                break;
            }
        }

        return retList;
    }

    @Override
    public final long size() {
        return journal.size();
    }

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
        int adjustedIndex = adjustedIndex(logEntryIndex);
        return adjustedIndex >= 0;
    }

    @Override
    public final boolean isInSnapshot(final long logEntryIndex) {
        if (logEntryIndex < 0) {
            return false;
        }
        final var local = snapshotEntry;
        return snapshotEntry != null && logEntryIndex <= local.index();
    }

    @Override
    public final @Nullable RaftEntryMeta snapshotEntry() {
        return snapshotEntry;
    }

    @Override
    public final void setSnapshotMeta(final RaftEntryMeta newSnapshotMeta) {
        snapshotEntry = newSnapshotMeta != null ? ImmutableRaftEntryMeta.copyOf(newSnapshotMeta) : null;
    }

    @Override
    public void clear(final int startIndex, final int endIndex) {
        journal.subList(startIndex, endIndex).clear();
    }

    @Override
    public void snapshotPreCommit(final long snapshotCapturedIndex, final long snapshotCapturedTerm) {
        Preconditions.checkArgument(snapshotCapturedIndex >= snapshotIndex,
                "snapshotCapturedIndex must be greater than or equal to snapshotIndex");

        snapshottedJournal = new ArrayList<>(journal.size());

        final var snapshotJournalEntries = journal.subList(0, (int) (snapshotCapturedIndex - snapshotIndex));

        snapshottedJournal.addAll(snapshotJournalEntries);
        snapshotJournalEntries.clear();

        previousSnapshotEntry = snapshotEntry;
        snapshotEntry = ImmutableRaftEntryMeta.of(snapshotCapturedIndex, snapshotCapturedTerm);
    }

    @Override
    public void snapshotCommit(final boolean updateDataSize) {
        snapshottedJournal = null;
        previousSnapshotEntry = null;

        if (updateDataSize) {
            // need to recalc the datasize based on the entries left after precommit.
            int newDataSize = 0;
            for (ReplicatedLogEntry logEntry : journal) {
                newDataSize += logEntry.size();
            }
            LOG.trace("{}: Updated dataSize from {} to {}", memberId, dataSize, newDataSize);
            dataSize = newDataSize;
        }
    }

    @Override
    public void snapshotRollback() {
        snapshottedJournal.addAll(journal);
        journal = snapshottedJournal;
        snapshottedJournal = null;
        snapshotEntry = previousSnapshotEntry;
        previousSnapshotEntry = null;
    }

    @VisibleForTesting
    ReplicatedLogEntry getAtPhysicalIndex(final int index) {
        return journal.get(index);
    }

    @NonNullByDefault
    static final RaftEntryMeta computeLastAppliedEntry(final ReplicatedLog log, final long originalIndex,
            final @Nullable RaftEntryMeta lastLogEntry, final boolean hasFollowers) {
        return hasFollowers ? compulateLastAppliedEntry(log, originalIndex)
            : compulateLastAppliedEntry(log, lastLogEntry);
    }

    @NonNullByDefault
    static final RaftEntryMeta compulateLastAppliedEntry(final ReplicatedLog log, final long originalIndex) {
        final var entry = log.lookupMeta(originalIndex);
        if (entry != null) {
            return entry;
        }

        final var snapshotIndex = log.getSnapshotIndex();
        return snapshotIndex > -1 ? ImmutableRaftEntryMeta.of(snapshotIndex, log.getSnapshotTerm())
            : ImmutableRaftEntryMeta.of(-1, -1);
    }

    @NonNullByDefault
    static final RaftEntryMeta compulateLastAppliedEntry(final ReplicatedLog log,
            final @Nullable RaftEntryMeta lastLogEntry) {
        if (lastLogEntry != null) {
            // since we have persisted the last-log-entry to persistent journal before the capture, we would want
            // to snapshot from this entry.
            return lastLogEntry;
        }
        return ImmutableRaftEntryMeta.of(-1, -1);
    }
}
