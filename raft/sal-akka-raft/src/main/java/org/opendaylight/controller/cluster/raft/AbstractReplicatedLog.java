/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.SnapshotManager.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.EntryJournal;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.api.EntryMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class handling the mapping of
 * logical LogEntry Index and the physical list index.
 */
public abstract class AbstractReplicatedLog<T extends ReplicatedLogEntry> implements ReplicatedLog {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractReplicatedLog.class);

    final @NonNull String memberId;

    // We define this as ArrayList so we can use ensureCapacity.
    private ArrayList<@NonNull T> journal = new ArrayList<>();

    private long firstJournalIndex = EntryJournal.FIRST_JOURNAL_INDEX;
    private long snapshotIndex = -1;
    private long snapshotTerm = -1;
    private long commitIndex = -1;
    private long lastApplied = -1;

    // to be used for rollback during save snapshot failure
    private ArrayList<T> snapshottedJournal;
    private long previousFirstJournalIndex = EntryJournal.FIRST_JOURNAL_INDEX;
    private long previousSnapshotIndex = -1;
    private long previousSnapshotTerm = -1;
    private int dataSize = 0;

    protected AbstractReplicatedLog(final @NonNull String memberId) {
        this.memberId = requireNonNull(memberId);
    }

    // FIXME: do not trim to int -- callers should do that for access to journal.get() purposes
    protected final int adjustedIndex(final long logEntryIndex) {
        if (snapshotIndex < 0) {
            return (int) logEntryIndex;
        }
        return (int) (logEntryIndex - (snapshotIndex + 1));
    }

    private void clearRollback() {
        previousSnapshotTerm = previousSnapshotIndex = -1;
        snapshottedJournal = null;
    }

    @Override
    public final void resetToLog(final ReplicatedLog prev) {
        clearRollback();

        firstJournalIndex = prev.firstJournalIndex();
        snapshotIndex = prev.getSnapshotIndex();
        snapshotTerm = prev.getSnapshotTerm();
        commitIndex = prev.getCommitIndex();
        lastApplied = prev.getLastApplied();

        dataSize = 0;
        final var prevSize = prev.size();
        journal = new ArrayList<>((int) Objects.checkIndex(prevSize, Integer.MAX_VALUE));
        for (long i = 0; i < prevSize; ++i) {
            final var entry = adoptEntry(prev.entryAt(i));
            journal.add(entry);
            dataSize += entry.size();
        }
    }

    @Override
    public final void resetToSnapshot(final Snapshot snapshot) {
        clearRollback();

        snapshotIndex = commitIndex = lastApplied = snapshot.getLastAppliedIndex();
        snapshotTerm = snapshot.getLastAppliedTerm();

        // Yes, there are faster ways to do this, but we want to be defensive
        dataSize = 0;
        journal = new ArrayList<>();
        final var unapplied = snapshot.getUnAppliedEntries();
        journal.ensureCapacity(unapplied.size());
        unapplied.forEach(this::append);
    }

    @Override
    public final LogEntry entryAt(final long offset) {
        return verifyNotNull(journal.get((int) Objects.checkIndex(offset, size())));
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
    public final T last() {
        return journal.isEmpty() ? null : journal.getLast();
    }

    @Override
    public final long getCommitIndex() {
        return commitIndex;
    }

    @Override
    public final void setCommitIndex(final long commitIndex) {
        this.commitIndex = commitIndex;
    }

    @Override
    public final long getLastApplied() {
        return lastApplied;
    }

    @Override
    public final void setLastApplied(final long lastApplied) {
        LOG.debug("{}: Moving last applied index from {} to {}", memberId, this.lastApplied, lastApplied,
            LOG.isTraceEnabled() ? new Throwable() : null);
        this.lastApplied = lastApplied;
    }

    /**
     * Removes entries from the in-memory log starting at the given index.
     *
     * @param fromIndex the index of the first log entry to remove
     * @return the adjusted index of the first log entry removed or -1 if the log entry is not found
     */
    protected final long removeFrom(final long fromIndex) {
        final int adjustedIndex = adjustedIndex(fromIndex);
        if (adjustedIndex < 0) {
            // physical index should be >= 0
            return -1;
        }

        final var size = journal.size();
        if (adjustedIndex >= size) {
            // physical index should be less than list size
            return -1;
        }

        final var toRemove = journal.subList(adjustedIndex, size);
        for (var entry : toRemove) {
            dataSize -= entry.size();
        }
        toRemove.clear();

        return adjustedIndex;
    }

    @Override
    public final boolean append(final LogEntry entry) {
        return appendImpl(adoptEntry(requireNonNull(entry)));
    }

    protected final boolean appendImpl(final @NonNull T entry) {
        final var entryIndex = entry.index();
        final var lastIndex = lastIndex();
        if (entryIndex > lastIndex) {
            journal.add(entry);
            dataSize += entry.size();
            return true;
        }

        LOG.warn("{}: Cannot append new entry - new index {} is not greater than the last index {}", memberId,
            entryIndex, lastIndex, new Exception("stack trace"));
        return false;
    }

    @NonNullByDefault
    protected abstract @NonNull T adoptEntry(LogEntry entry);

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

    @Override
    public final long size() {
        return journal.size();
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
    public final long firstJournalIndex() {
        return firstJournalIndex;
    }

    @Override
    public final long lastAppliedJournalIndex() {
        return adjustedIndex(getLastApplied()) + firstJournalIndex();
    }

    @Override
    public final void setFirstJournalIndex(long newFirstJournalIndex) {
        firstJournalIndex = newFirstJournalIndex;
    }

    @Override
    public final long getSnapshotIndex() {
        return snapshotIndex;
    }

    @Override
    public final long getSnapshotTerm() {
        return snapshotTerm;
    }

    @Override
    public final void setSnapshotIndex(final long snapshotIndex) {
        this.snapshotIndex = snapshotIndex;
    }

    @Override
    public final void setSnapshotTerm(final long snapshotTerm) {
        this.snapshotTerm = snapshotTerm;
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
        final var trimCount = snapshotCapturedIndex - snapshotIndex;
        if (trimCount < 0) {
            throw new IllegalArgumentException("snapshotCapturedIndex must be greater than or equal to snapshotIndex");
        }
        // we cannot hold more than MAX_VALUE elements in a collection, this mirrors what List.subList() would report
        if (trimCount > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("trimCount=" + trimCount);
        }

        final var trimSize = (int) trimCount;
        snapshottedJournal = new ArrayList<>(trimSize);
        if (trimSize != 0) {
            final var toTrim = journal.subList(0, trimSize);
            snapshottedJournal.addAll(toTrim);
            toTrim.clear();
        }

        previousFirstJournalIndex = firstJournalIndex;
        firstJournalIndex += trimSize;

        previousSnapshotIndex = snapshotIndex;
        setSnapshotIndex(snapshotCapturedIndex);

        previousSnapshotTerm = snapshotTerm;
        setSnapshotTerm(snapshotCapturedTerm);
    }

    @Override
    public final void snapshotCommit(final boolean updateDataSize) {
        snapshottedJournal = null;
        previousFirstJournalIndex = EntryJournal.FIRST_JOURNAL_INDEX;
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

        firstJournalIndex = previousFirstJournalIndex;
        previousFirstJournalIndex = EntryJournal.FIRST_JOURNAL_INDEX;

        snapshotIndex = previousSnapshotIndex;
        previousSnapshotIndex = -1;

        snapshotTerm = previousSnapshotTerm;
        previousSnapshotTerm = -1;
    }

    @Override
    public final @NonNull CaptureSnapshot newCaptureSnapshot(final EntryMeta lastLogEntry,
            final long replicatedToAllIndex, final boolean mandatoryTrim, final boolean hasFollowers) {
        final var lastAppliedEntry = computeLastAppliedEntry(this, getLastApplied(), lastLogEntry, hasFollowers);

        final var entry = lookup(replicatedToAllIndex);
        final var replicatedToAllEntry = entry != null ? entry : EntryInfo.of(-1, -1);

        long lastAppliedIndex = lastAppliedEntry.index();
        long lastAppliedTerm = lastAppliedEntry.term();

        final var unAppliedEntries = getFrom(lastAppliedIndex + 1);

        final long lastLogEntryIndex;
        final long lastLogEntryTerm;
        if (lastLogEntry == null) {
            // When we don't have journal present, for example two captureSnapshots executed right after another with no
            // new journal we still want to preserve the index and term in the snapshot.
            lastAppliedIndex = lastLogEntryIndex = getSnapshotIndex();
            lastAppliedTerm = lastLogEntryTerm = getSnapshotTerm();

            LOG.debug("{}: Capturing Snapshot : lastLogEntry is null. Using snapshot values lastAppliedIndex {} and "
                + "lastAppliedTerm {} instead.", memberId, lastAppliedIndex, lastAppliedTerm);
        } else {
            lastLogEntryIndex = lastLogEntry.index();
            lastLogEntryTerm = lastLogEntry.term();
        }

        return new CaptureSnapshot(lastLogEntryIndex, lastLogEntryTerm, lastAppliedIndex, lastAppliedTerm,
            replicatedToAllEntry.index(), replicatedToAllEntry.term(), unAppliedEntries, mandatoryTrim);
    }

    @VisibleForTesting
    @NonNullByDefault
    static final EntryMeta computeLastAppliedEntry(final ReplicatedLog log, final long originalIndex,
            final @Nullable EntryMeta lastLogEntry, final boolean hasFollowers) {
        return hasFollowers ? compulateLastAppliedEntry(log, originalIndex)
            : compulateLastAppliedEntry(log, lastLogEntry);
    }

    @NonNullByDefault
    static final EntryMeta compulateLastAppliedEntry(final ReplicatedLog log, final long originalIndex) {
        final var entry = log.lookupMeta(originalIndex);
        if (entry != null) {
            return entry;
        }

        final var snapshotIndex = log.getSnapshotIndex();
        return snapshotIndex > -1 ? EntryInfo.of(snapshotIndex, log.getSnapshotTerm()) : EntryInfo.of(-1, -1);
    }

    @NonNullByDefault
    static final EntryMeta compulateLastAppliedEntry(final ReplicatedLog log,
            final @Nullable EntryMeta lastLogEntry) {
        if (lastLogEntry != null) {
            // since we have persisted the last-log-entry to persistent journal before the capture, we would want
            // to snapshot from this entry.
            return lastLogEntry;
        }
        return EntryInfo.of(-1, -1);
    }
}
