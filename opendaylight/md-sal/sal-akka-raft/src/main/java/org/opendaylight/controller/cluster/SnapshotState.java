/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster;

import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;

public interface SnapshotState {
    /**
     * Should return true when a snapshot is being captured
     * @return
     */
    boolean isCapturing();

    /**
     * Request snapshot capture. If the criteria for snapshot creation matches any one of the following then
     * the snapshot capturing process should begin otherwise not.
     *
     * <li> dataSizeForCheck exceeds the configured memory threshold</li>
     * <li> the size of the journal exceeds the configured snapshot batch count </li>
     * <li> the snapshot needs to be installed on to a follower </li>
     *
     * @param lastLogEntry the last entry in the replicated log
     * @param replicatedToAllIndex the current replicatedToAllIndex
     */
    void capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex);

    void captureToInstall(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex);

    /**
     * Persist the snapshot
     *
     * @param persistenceProvider
     * @param snapshotBytes
     * @param currentBehavior
     */
    void persist(DataPersistenceProvider persistenceProvider, byte[] snapshotBytes, RaftActorBehavior currentBehavior);

    /**
     * Commit the snapshot by trimming the log
     *
     * @param persistenceProvider
     * @param sequenceNumber
     */
    void commit(DataPersistenceProvider persistenceProvider, long sequenceNumber);

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
