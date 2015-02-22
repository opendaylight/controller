/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster;

import akka.japi.Procedure;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;

public interface SnapshotState {
    /**
     * Should return true when a snapshot is being captured
     * @return
     */
    boolean isCapturing();

    /**
     * Initiate capture snapshot
     *
     * @param lastLogEntry the last entry in the replicated log
     * @param replicatedToAllIndex the current replicatedToAllIndex
     */
    void capture(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex);

    /**
     * Initiate capture snapshot for the purposing of installing that snapshot
     *
     * @param lastLogEntry
     * @param replicatedToAllIndex
     */
    void captureToInstall(ReplicatedLogEntry lastLogEntry, long replicatedToAllIndex);

    /**
     * Create the snapshot
     *
     * @param procedure
     */
    void create(Procedure<Void> procedure);

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
