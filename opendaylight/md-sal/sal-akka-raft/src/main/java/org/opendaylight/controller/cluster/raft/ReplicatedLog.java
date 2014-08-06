/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import java.util.List;

/**
 * Represents the ReplicatedLog that needs to be kept in sync by the RaftActor
 */
public interface ReplicatedLog {
    /**
     * Get a replicated log entry at the specified index
     *
     * @param index the index of the log entry
     * @return the ReplicatedLogEntry at index. null if index less than 0 or
     * greater than the size of the in-memory journal.
     */
    ReplicatedLogEntry get(long index);


    /**
     * Get the last replicated log entry
     *
     * @return
     */
    ReplicatedLogEntry last();

    /**
     *
     * @return
     */
    long lastIndex();

    /**
     *
     * @return
     */
    long lastTerm();

    /**
     * To be called when we need to remove entries from the in-memory log.
     * This method will remove all entries >= index. This method should be used
     * during recovery to appropriately trim the log based on persisted
     * information
     *
     * @param index the index of the log entry
     */
    void removeFrom(long index);


    /**
     * To be called when we need to remove entries from the in-memory log and we
     * need that information persisted to disk. This method will remove all
     * entries >= index.
     * <p>
     * The persisted information would then be used during recovery to properly
     * reconstruct the state of the in-memory replicated log
     *
     * @param index the index of the log entry
     */
    void removeFromAndPersist(long index);

    /**
     * Append an entry to the log
     * @param replicatedLogEntry
     */
    void append(ReplicatedLogEntry replicatedLogEntry);

    /**
     *
     * @param replicatedLogEntry
     */
    void appendAndPersist(final ReplicatedLogEntry replicatedLogEntry);

    /**
     *
     * @param index the index of the log entry
     */
    List<ReplicatedLogEntry> getFrom(long index);

    /**
     *
     * @param index the index of the log entry
     */
    List<ReplicatedLogEntry> getFrom(long index, int max);

    /**
     *
     * @return
     */
    long size();

    /**
     * Checks if the entry at the specified index is present or not
     *
     * @param index the index of the log entry
     * @return true if the entry is present in the in-memory journal
     */
    boolean isPresent(long index);

    /**
     * Checks if the entry is present in a snapshot
     *
     * @param index the index of the log entry
     * @return true if the entry is in the snapshot. false if the entry is not
     * in the snapshot even if the entry may be present in the replicated log
     */
    boolean isInSnapshot(long index);

    /**
     * Get the snapshot
     *
     * @return an object representing the snapshot if it exists. null otherwise
     */
    Object getSnapshot();

    /**
     * Get the index of the snapshot
     *
     * @return the index from which the snapshot was created. -1 otherwise.
     */
    long getSnapshotIndex();

    /**
     * Get the term of the snapshot
     *
     * @return the term of the index from which the snapshot was created. -1
     * otherwise
     */
    long getSnapshotTerm();
}
