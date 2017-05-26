/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import java.io.OutputStream;
import java.util.Optional;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Interface for a snapshot phase state.
 *
 * @author Moiz Raja
 * @author Thomas Pantelis
 */
public interface SnapshotState {
    /**
     * Returns whether or not a capture is in progress.
     *
     * @return true when a snapshot is being captured, false otherwise
     */
    boolean isCapturing();

    /**
     * Initiates a capture snapshot.
     *
     * @param lastLogEntry the last entry in the replicated log
     * @param replicatedToAllIndex the current replicatedToAllIndex
     * @return true if capture was started
     */
    boolean capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex);

    /**
     * Initiates a capture snapshot for the purposing of installing the snapshot on a follower.
     *
     * @param lastLogEntry the last entry in the replicated log
     * @param replicatedToAllIndex the current replicatedToAllIndex
     * @param targetFollower the id of the follower on which to install
     * @return true if capture was started
     */
    boolean captureToInstall(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex, String targetFollower);

    /**
     * Applies a snapshot on a follower that was installed by the leader.
     *
     * @param snapshot the Snapshot to apply.
     */
    void apply(ApplySnapshot snapshot);

    /**
     * Persists a snapshot.
     *
     * @param snapshotState the snapshot State
     * @param installSnapshotStream Optional OutputStream that is present if the snapshot is to also be installed
     *        on a follower.
     * @param totalMemory the total memory threshold
     */
    void persist(Snapshot.State snapshotState, Optional<OutputStream> installSnapshotStream, long totalMemory);

    /**
     * Commit the snapshot by trimming the log.
     *
     * @param sequenceNumber the sequence number of the persisted snapshot
     * @param timeStamp the time stamp of the persisted snapshot
     */
    void commit(long sequenceNumber, long timeStamp);

    /**
     * Rolls back the snapshot on failure.
     */
    void rollback();

    /**
     * Trims the in-memory log.
     *
     * @param desiredTrimIndex the desired index to trim from
     * @return the actual trim index
     */
    long trimLog(long desiredTrimIndex);
}
