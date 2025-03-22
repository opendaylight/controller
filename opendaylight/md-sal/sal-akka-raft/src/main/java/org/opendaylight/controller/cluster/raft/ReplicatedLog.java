/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.cluster.raft.spi.RaftEntryMeta;

/**
 * Represents the ReplicatedLog that needs to be kept in sync by the RaftActor.
 */
public interface ReplicatedLog {
    long NO_MAX_SIZE = -1;

    /**
     * Return the replicated log entry at the specified index.
     *
     * @param index the index of the log entry
     * @return the ReplicatedLogEntry if found, otherwise null if the adjusted index less than 0 or
     *         greater than the size of the in-memory journal
     */
    @Nullable ReplicatedLogEntry get(long index);

    /**
     * Return metadata about a replicated entry.
     *
     * @param index the index of the log entry
     * @return the RaftEntryMeta if found, otherwise null if the adjusted index less than 0 or
     *         greater than the size of the in-memory journal
     */
    default @Nullable RaftEntryMeta lookupMeta(final long index) {
        return get(index);
    }

    /**
     * Return the last replicated log entry in the log or null of not found.
     *
     * @return the last replicated log entry in the log or null of not found.
     */
    @Nullable ReplicatedLogEntry last();

    /**
     * Return the last replicated log entry in the log or null of not found.
     *
     * @return the last replicated log entry in the log or null of not found.
     */
    default @Nullable RaftEntryMeta lastMeta() {
        return last();
    }

    /**
     * Return the index of the last entry in the log or -1 if the log is empty.
     *
     * @return the index of the last entry in the log or -1 if the log is empty.
     */
    long lastIndex();

    /**
     * Return the term of the last entry in the log or -1 if the log is empty.
     *
     * @return the term of the last entry in the log or -1 if the log is empty.
     */
    long lastTerm();

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
     * Removes entries from the in-memory log starting at the given index.
     *
     * @param index the index of the first log entry to remove
     * @return the adjusted index of the first log entry removed or -1 if the log entry is not found.
     */
    long removeFrom(long index);

    /**
     * Removes entries from the in-memory log and the persisted log starting at the given index.
     *
     * <p>The persisted information would then be used during recovery to properly reconstruct the state
     * of the in-memory replicated log
     *
     * @param index the index of the first log entry to remove
     * @return true if entries were removed, false otherwise
     */
    boolean removeFromAndPersist(long index);

    /**
     * Appends an entry to the log.
     *
     * @param replicatedLogEntry the entry to append
     * @return true if the entry was successfully appended, false otherwise. An entry can fail to append if
     *         the index is already included in the log.
     */
    boolean append(ReplicatedLogEntry replicatedLogEntry);

    /**
     * Optimization method to increase the capacity of the journal log prior to appending entries.
     *
     * @param amount the amount to increase by
     */
    void increaseJournalLogCapacity(int amount);

    /**
     * Appends an entry to the in-memory log and persists it as well.
     *
     * @param <T> entry type
     * @param replicatedLogEntry the entry to append
     * @param callback the callback to be notified when persistence is complete (optional).
     * @param doAsync if true, the persistent actor can receive subsequent messages to process in between the persist
     *        call and the execution of the associated callback. If false, subsequent messages are stashed and get
     *        delivered after persistence is complete and the associated callback is executed. In either case the
     *        callback is guaranteed to execute in the context of the actor associated with this log.
     * @return true if the entry was successfully appended, false otherwise.
     */
    <T extends ReplicatedLogEntry> boolean appendAndPersist(@NonNull T replicatedLogEntry,
            @Nullable Consumer<T> callback, boolean doAsync);

    /**
     * Returns a list of log entries starting from the given index to the end of the log.
     *
     * @param index the index of the first log entry to get.
     * @return the List of entries
     */
    @NonNull List<ReplicatedLogEntry> getFrom(long index);

    /**
     * Returns a list of log entries starting from the given index up to the given maximum of entries or
     * the given maximum accumulated size, whichever comes first.
     *
     * @param index the index of the first log entry to get
     * @param maxEntries the maximum number of entries to get
     * @param maxDataSize the maximum accumulated size of the log entries to get
     * @return the List of entries meeting the criteria.
     */
    @NonNull List<ReplicatedLogEntry> getFrom(long index, int maxEntries, long maxDataSize);

    /**
     * Returns the number of entries in the journal.
     *
     * @return the number of entries
     */
    long size();

    /**
     * Checks if the entry at the specified index is present or not.
     *
     * @param index the index of the log entry
     * @return true if the entry is present in the in-memory journal
     */
    boolean isPresent(long index);

    /**
     * Checks if the entry is present in a snapshot.
     *
     * @param index the index of the log entry
     * @return true if the entry is in the snapshot. false if the entry is not in the snapshot even if the entry may
     *         be present in the replicated log
     */
    boolean isInSnapshot(long index);

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
     * Clears the journal entries with startIndex (inclusive) and endIndex (exclusive).
     *
     * @param startIndex the start index (inclusive)
     * @param endIndex the end index (exclusive)
     */
    void clear(int startIndex, int endIndex);

    /**
     * Handles all the bookkeeping in order to perform a rollback in the event of SaveSnapshotFailure.
     *
     * @param snapshotCapturedIndex the new snapshot index
     * @param snapshotCapturedTerm the new snapshot term
     */
    void snapshotPreCommit(long snapshotCapturedIndex, long snapshotCapturedTerm);

    /**
     * Sets the Replicated log to state after snapshot success. This method is equivalent to
     * {@code snapshotCommit(true)}.
     */
    default void snapshotCommit() {
        snapshotCommit(true);
    }

    /**
     * Sets the Replicated log to state after snapshot success. Most users will want to use {@link #snapshotCommit()}
     * instead.
     *
     * @param updateDataSize true if {@link #dataSize()} should also be updated
     */
    void snapshotCommit(boolean updateDataSize);

    /**
     * Restores the replicated log to a state in the event of a save snapshot failure.
     */
    void snapshotRollback();

    /**
     * Returns the size of the data in the log (in bytes).
     *
     * @return the size of the data in the log (in bytes)
     */
    int dataSize();

    /**
     * Determines if a snapshot needs to be captured based on the count/memory consumed and initiates the capture.
     *
     * @param replicatedLogEntry the last log entry.
     */
    void captureSnapshotIfReady(RaftEntryMeta replicatedLogEntry);

    /**
     * Determines if a snapshot should be captured based on the count/memory consumed.
     *
     * @param logIndex the log index to use to determine if the log count has exceeded the threshold
     * @return true if a snapshot should be captured, false otherwise
     */
    boolean shouldCaptureSnapshot(long logIndex);
}
