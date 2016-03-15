/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;

public interface SnapshotState {
    /**
     * @return true when a snapshot is being captured
     */
    boolean isCapturing();

    /**
     * Initiate capture snapshot
     *
     * @param lastLogEntry the last entry in the replicated log
     * @param replicatedToAllIndex the current replicatedToAllIndex
     *
     * @return true if capture was started
     */
    boolean capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex);

    /**
     * Initiate capture snapshot for the purposing of installing that snapshot
     *
     * @param lastLogEntry
     * @param replicatedToAllIndex
     * @param targetFollower
     *
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
     * Persist the snapshot
     *
     * @param snapshotBytes
     * @param currentBehavior
     * @param totalMemory
     */
    void persist(byte[] snapshotBytes, long totalMemory);

    /**
     * Commit the snapshot by trimming the log
     *
     * @param sequenceNumber
     */
    void commit(long sequenceNumber);

    /**
     * Rollback the snapshot
     */
    void rollback();

    /**
     * Trim the log
     *
     * @param desiredTrimIndex
     * @return the actual trim index
     */
    long trimLog(long desiredTrimIndex);
}
