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
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.SnapshotManager.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.api.EntryMeta;

/**
 * Represents the ReplicatedLog that needs to be kept in sync by the RaftActor.
 */
@NonNullByDefault
public interface ReplicatedLog {
    /**
     * A combination of {@link EntryMeta} and indicator of whether the entry is stable.
     *
     * @param meta the {@link EntryMeta}
     * @param durable {@code true} if the entry is known to be durable
     */
    record StoredEntryMeta(EntryMeta meta, boolean durable) {
        /**
         * Default constructor.
         *
         * @param meta the {@link EntryMeta}
         * @param durable {@code true} if the entry is known to be durable
         */
        public StoredEntryMeta {
            requireNonNull(meta);
        }
    }

    interface RecoveringPosition {

        RecoveringApplied recoverPosition(long journalIndex, EntryInfo snapshotEntry);
    }

    interface RecoveringApplied {

        void recoverAppliedEntry(LogEntry entry);

        RecoveringUnapplied finish();
    }

    interface RecoveringUnapplied {

        void recoverUnappliedEntry(LogEntry entry);

        void finish();
    }

    /**
     * Returns the entry and specified offset.
     *
     * @param offset the offset
     * @return the {@link LogEntry}
     * @throws IndexOutOfBoundsException if the offset is out of range ({@code offset < 0 || offset >= size()})
     */
    LogEntry entryAt(long offset);

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
     * Return {@link StoredEntryMeta} a replicated entry.
     *
     * @param index the index of the log entry
     * @return the {@link StoredEntryMeta} if found, otherwise null if the adjusted index less than 0 or greater than
     *         the size of the in-memory journal
     */
    @Nullable StoredEntryMeta lookupStoredMeta(long index);

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
     * Mark the current value {@link #getLastApplied()} for recovery purposes.
     */
    void markLastApplied();

    /**
     * Removes entries all entries from the log starting at the given index, resetting {@link #lastIndex()} to
     * {@code nextIndex - 1}.
     *
     * @param nextIndex the index of the first log entry to remove
     * @return {@code true} if the operation succeeds
     */
    // TODO: This method should only ever be invoked from a Follower, split it to a separate interface accessible only
    //       to Follower (i.e. invoke some .toFollower() which will give out FollowerReplicatedLog with this method.
    // FIXME: CONTROLLER-2044: change to appendEntries(List<ReplicatedLogEntry>)
    boolean trimToReceive(long nextIndex);

    /**
     * Appends an entry to the log if its index is already included in the log.
     *
     * @param entry the entry to append
     * @return {@code true} if the entry was successfully appended, {@code false} otherwise.
     */
    boolean append(LogEntry entry);

    /**
     * Optimization method to increase the capacity of the journal log prior to appending entries.
     *
     * @param amount the amount to increase by
     */
    void increaseJournalLogCapacity(int amount);

    /**
     * Appends an entry received by a follower to the in-memory log and persists it as well, returning an indication
     * whether or not a snapshot should be taken.
     *
     * @param entry the entry to append
     * @param callback optional callback to be notified when persistence is complete
     * @return {@code true} if the journal requires trimming and a snapshot needs to be taken
     */
    boolean appendReceived(LogEntry entry, @Nullable Consumer<LogEntry> callback);

    /**
     * Appends an entry submitted on the leader to the in-memory log and persists it as well.
     *
     * @param index the index
     * @param term the term
     * @param command the command
     * @param callback the callback to be notified when persistence is complete
     * @return {@code true} if the entry was successfully appended, false otherwise
     */
    boolean appendSubmitted(long index, long term, Payload command, Consumer<ReplicatedLogEntry> callback);

    /**
     * Returns a list of log entries starting from the given index to the end of the log.
     *
     * @param index the index of the first log entry to get.
     * @return the List of entries
     */
    List<ReplicatedLogEntry> getFrom(long index);

    /**
     * Returns a list of log entries starting from the given index up to the given maximum of entries or
     * the given maximum accumulated size, whichever comes first.
     *
     * @param index the index of the first log entry to get
     * @param maxEntries the maximum number of entries to get
     * @param maxDataSize the maximum accumulated size of the log entries to get, negative means no limit
     * @return the List of entries meeting the criteria.
     */
    List<ReplicatedLogEntry> getFrom(long index, int maxEntries, long maxDataSize);

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
     * Returns the actual index of the entry in replicated log for the given index or -1 if not found.
     *
     * @return the log entry index or -1 if not found
     */
    default long getLogEntryIndex(final long index) {
        if (index == getSnapshotIndex()) {
            return index;
        }

        final var meta = lookupMeta(index);
        return meta != null ? meta.index() : -1;
    }

    /**
     * Returns the actual term of the entry in the replicated log for the given index or -1 if not found.
     *
     * @return the log entry term or -1 if not found
     */
    default long getLogEntryTerm(final long index) {
        if (index == getSnapshotIndex()) {
            return getSnapshotTerm();
        }

        final var meta = lookupMeta(index);
        return meta != null ? meta.term() : -1;
    }

    /**
     * Returns the actual term of the entry in the replicated log for the given index or, if not present, returns the
     * snapshot term if the given index is in the snapshot or -1 otherwise.
     *
     * @return the term or -1 otherwise
     */
    default long getLogEntryOrSnapshotTerm(final long index) {
        return isInSnapshot(index) ? getSnapshotTerm() : getLogEntryTerm(index);
    }

    default boolean isLogEntryPresent(final long index) {
        return isInSnapshot(index) || lookupMeta(index) != null;
    }

    /**
     * Clears all entries.
     *
     * @deprecated Use {@link #resetToSnapshot(Snapshot)} instead.
     */
    @VisibleForTesting
    @Deprecated(since = "11.0.0", forRemoval = true)
    void clear();

    /**
     * Clears the journal entries with startIndex (inclusive) and endIndex (exclusive).
     *
     * @param startIndex the start index (inclusive)
     * @param endIndex the end index (exclusive)
     */
    // FIXME: this method does not update dataSize()
    @VisibleForTesting
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
     * @param lastEntry the last log entry.
     */
    void captureSnapshotIfReady(EntryMeta lastEntry);

    /**
     * Determines if a snapshot should be captured based on the count/memory consumed.
     *
     * @param logIndex the log index to use to determine if the log count has exceeded the threshold
     * @return true if a snapshot should be captured, false otherwise
     */
    boolean shouldCaptureSnapshot(long logIndex);

    /**
     * Reset interal state to match specified {@link ReplicatedLog}.
     *
     * @param prev the log to reset to
     */
    void resetToLog(ReplicatedLog prev);

    /**
     * Reset internal state to specified {@link Snapshot}.
     *
     * @param snapshot snapshot to reset to
     */
    void resetToSnapshot(Snapshot snapshot);

    /**
     * Constructs a CaptureSnapshot instance.
     *
     * @param lastLogEntry the last log entry for the snapshot.
     * @param replicatedToAllIndex the index of the last entry replicated to all followers.
     * @return a new CaptureSnapshot instance.
     */
    CaptureSnapshot newCaptureSnapshot(@Nullable EntryMeta lastLogEntry, long replicatedToAllIndex,
        boolean mandatoryTrim, boolean hasFollowers);
}
