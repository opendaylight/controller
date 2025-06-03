/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.SnapshotManager.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.api.EntryMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for {@link BaseLog} implementations.
 */
@NonNullByDefault
public abstract class AbstractBaseLog<T extends ReplicatedLogEntry> implements BaseLog {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBaseLog.class);

    protected final String memberId;

    // We define this as ArrayList so we can use ensureCapacity.
    private ArrayList<@NonNull T> journal = new ArrayList<>();
    private long snapshotIndex = -1;
    private long snapshotTerm = -1;
    private long commitIndex = -1;
    private long lastApplied = -1;
    private int dataSize = 0;

    protected AbstractBaseLog(final String memberId) {
        this.memberId = requireNonNull(memberId);
    }

    protected abstract @NonNull T adoptEntry(LogEntry entry);

    @Override
    public final LogEntry entryAt(final long offset) {
        return verifyNotNull(journal.get((int) Objects.checkIndex(offset, size())));
    }

    @Override
    public final @Nullable T last() {
        return journal.isEmpty() ? null : journal.getLast();
    }

    @Override
    public final long size() {
        return journal.size();
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
    public final CaptureSnapshot startCapture(final @Nullable EntryMeta lastLogEntry,
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

    protected void resetToLog(final BaseLog prev) {
        snapshotIndex = prev.getSnapshotIndex();
        snapshotTerm = prev.getSnapshotTerm();
        commitIndex = prev.getCommitIndex();
        lastApplied = prev.getLastApplied();

        for (long i = 0, size = prev.size(); i < size; ++i) {
            final var entry = adoptEntry(prev.entryAt(i));
            journal.add(entry);
            dataSize += entry.size();
        }
    }

    @Override
    public void resetToSnapshot(final Snapshot snapshot) {
        snapshotIndex = commitIndex = lastApplied = snapshot.getLastAppliedIndex();
        snapshotTerm = snapshot.getLastAppliedTerm();

        // Yes, there are faster ways to do this, but we want to be defensive
        dataSize = 0;
        journal = new ArrayList<>();
        final var unapplied = snapshot.getUnAppliedEntries();
        journal.ensureCapacity(unapplied.size());
        unapplied.forEach(this::append);
    }

    // FIXME: do not trim to int -- callers should do that for access to journal.get() purposes
    protected final int adjustedIndex(final long logEntryIndex) {
        if (snapshotIndex < 0) {
            return (int) logEntryIndex;
        }
        return (int) (logEntryIndex - (snapshotIndex + 1));
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

    @VisibleForTesting
    public static final EntryMeta computeLastAppliedEntry(final BaseLog log, final long originalIndex,
            final @Nullable EntryMeta lastLogEntry, final boolean hasFollowers) {
        return hasFollowers ? compulateLastAppliedEntry(log, originalIndex)
            : compulateLastAppliedEntry(log, lastLogEntry);
    }

    @VisibleForTesting
    public static final EntryMeta compulateLastAppliedEntry(final BaseLog log, final long originalIndex) {
        final var entry = log.lookupMeta(originalIndex);
        if (entry != null) {
            return entry;
        }

        final var snapshotIndex = log.getSnapshotIndex();
        return snapshotIndex > -1 ? EntryInfo.of(snapshotIndex, log.getSnapshotTerm()) : EntryInfo.of(-1, -1);
    }

    @VisibleForTesting
    public static final EntryMeta compulateLastAppliedEntry(final BaseLog log,
            final @Nullable EntryMeta lastLogEntry) {
        if (lastLogEntry != null) {
            // since we have persisted the last-log-entry to persistent journal before the capture, we would want
            // to snapshot from this entry.
            return lastLogEntry;
        }
        return EntryInfo.of(-1, -1);
    }
}
