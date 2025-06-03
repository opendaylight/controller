/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.SnapshotManager.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.raft.api.EntryMeta;

/**
 * Baseline interface for implementations RAFT log.
 */
@NonNullByDefault
public interface BaseLog {
    /**
     * Returns the entry and specified offset.
     *
     * @param offset the offset
     * @return the {@link LogEntry}
     * @throws IndexOutOfBoundsException if the offset is out of range ({@code offset < 0 || offset >= size()})
     */
    LogEntry entryAt(long offset);

    /**
     * Return the last replicated log entry in the log or null of not found.
     *
     * @return the last replicated log entry in the log or null of not found.
     */
    @Nullable LogEntry last();

    /**
     * Return the last replicated log entry in the log or null of not found.
     *
     * @return the last replicated log entry in the log or null of not found.
     */
    default @Nullable EntryMeta lastMeta() {
        return last();
    }

    /**
     * Return the index of the last entry in the log or -1 if the log is empty.
     *
     * @return the index of the last entry in the log or -1 if the log is empty.
     */
    default long lastIndex() {
        final var last = lastMeta();
        // it can happen that after snapshot, all the entries of the journal are trimmed till lastApplied,
        // so lastIndex = snapshotIndex
        return last != null ? last.index() : getSnapshotIndex();
    }

    /**
     * Return the term of the last entry in the log or -1 if the log is empty.
     *
     * @return the term of the last entry in the log or -1 if the log is empty.
     */
    default long lastTerm() {
        final var last = lastMeta();
        // it can happen that after snapshot, all the entries of the journal are trimmed till lastApplied,
        // so lastTerm = snapshotTerm
        return last != null ? last.term() : getSnapshotTerm();
    }

    /**
     * {@return the number of entries in this log}
     */
    long size();

    /**
     * Appends an entry to the log if its index is already included in the log.
     *
     * @param entry the entry to append
     * @return {@code true} if the entry was successfully appended, {@code false} otherwise.
     */
    boolean append(LogEntry entry);

    /**
     * Returns the index of highest log entry known to be committed.
     *
     * @return index of highest log entry known to be committed.
     */
    long getCommitIndex();

    /**
     * Sets the index of highest log entry known to be committed.
     *
     * @param commitIndex new commit index
     */
    void setCommitIndex(long commitIndex);

    /**
     * Returns index of highest log entry applied to state machine.
     *
     * @return index of highest log entry applied to state machine.
     */
    long getLastApplied();

    /**
     * Sets index of highest log entry applied to state machine.
     *
     * @param lastApplied the new applied index.
     */
    void setLastApplied(long lastApplied);

    /**
     * Returns the index of the snapshot.
     *
     * @return the index from which the snapshot was created. -1 otherwise.
     */
    long getSnapshotIndex();

    /**
     * Returns the term of the snapshot.
     *
     * @return the term of the index from which the snapshot was created. -1 otherwise
     */
    long getSnapshotTerm();

    /**
     * Sets the snapshot index in the replicated log.
     *
     * @param snapshotIndex the index to set
     */
    void setSnapshotIndex(long snapshotIndex);

    /**
     * Sets snapshot term.
     *
     * @param snapshotTerm the term to set
     */
    void setSnapshotTerm(long snapshotTerm);

    /**
     * Return the replicated log entry at the specified index.
     *
     * @param index the index of the log entry
     * @return the ReplicatedLogEntry if found, otherwise null if the adjusted index less than 0 or greater than the
     *         size of the in-memory journal
     */
    @Nullable LogEntry lookup(long index);

    /**
     * Return metadata about a replicated entry.
     *
     * @param index the index of the log entry
     * @return the {@link EntryMeta} if found, otherwise null if the adjusted index less than 0 or greater than the size
     *         of the in-memory journal
     */
    default @Nullable EntryMeta lookupMeta(final long index) {
        return lookup(index);
    }

    /**
     * Constructs a CaptureSnapshot instance.
     *
     * @param lastLogEntry the last log entry for the snapshot.
     * @param replicatedToAllIndex the index of the last entry replicated to all followers.
     * @return a new CaptureSnapshot instance.
     */
    CaptureSnapshot startCapture(@Nullable EntryMeta lastLogEntry, long replicatedToAllIndex, boolean mandatoryTrim,
        boolean hasFollowers);

    /**
     * Reset internal state to specified {@link Snapshot}.
     *
     * @param snapshot snapshot to reset to
     */
    void resetToSnapshot(Snapshot snapshot);
}