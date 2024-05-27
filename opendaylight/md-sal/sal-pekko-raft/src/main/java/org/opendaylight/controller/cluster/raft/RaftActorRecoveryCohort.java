/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Interface for a class that participates in raft actor persistence recovery.
 *
 * @author Thomas Pantelis
 */
public interface RaftActorRecoveryCohort {

    /**
     * This method is called during recovery at the start of a batch of state entries. Derived
     * classes should perform any initialization needed to start a batch.
     *
     * @param maxBatchSize the maximum batch size.
     */
    void startLogRecoveryBatch(int maxBatchSize);

    /**
     * This method is called during recovery to append state data to the current batch. This method
     * is called 1 or more times after {@link #startLogRecoveryBatch}.
     *
     * @param data the state data
     */
    void appendRecoveredLogEntry(Payload data);

    /**
     * This method is called during recovery to reconstruct the state of the actor.
     *
     * @param snapshotState A snapshot of the state of the actor
     */
    void applyRecoverySnapshot(Snapshot.State snapshotState);

    /**
     * This method is called during recovery at the end of a batch to apply the current batched
     * log entries. This method is called after {@link #appendRecoveredLogEntry}.
     */
    void applyCurrentLogRecoveryBatch();

    /**
     * Returns the snapshot to restore from on recovery.
     *
     * @return the snapshot or null if there's no snapshot to restore
     */
    @Nullable Snapshot getRestoreFromSnapshot();
}
