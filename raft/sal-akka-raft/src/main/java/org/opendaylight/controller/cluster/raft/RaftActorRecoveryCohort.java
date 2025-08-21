/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.spi.StateCommand;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;

/**
 * Interface for a class that participates in raft actor persistence recovery.
 *
 * @author Thomas Pantelis
 */
// FIXME: promote this interface into raft.spi with a tad saner semantics:
//        - we have an explicit recovery start, when this object is allocated
//        - we then can receive either a snapshot or command (the start of a batch)
//        - we then can only receive a number of batches
//        - we also can take snapshots
//        - we *mumble-mumble* with getRestoreFromSnapshot()
//        - we complete by giving out something supports:
//          -- RaftActorSnapshotCohort
//          -- RaftActor.applyCommand()
@NonNullByDefault
public interface RaftActorRecoveryCohort {
    /**
     * This method is called during recovery at the start of a batch of state entries. Derived
     * classes should perform any initialization needed to start a batch.
     *
     * @param maxBatchSize the maximum batch size.
     */
    void startLogRecoveryBatch(int maxBatchSize);

    /**
     * This method is called during recovery to append a {@link StateCommand} to the current batch. This method is
     * called 1 or more times after {@link #startLogRecoveryBatch}.
     *
     * @param command the command
     */
    // FIXME: allow an IOException (or some other checked exception) to be thrown here
    void appendRecoveredCommand(StateCommand command);

    /**
     * This method is called during recovery to reconstruct the state of the actor.
     *
     * @param snapshot the {@link StateSnapshot} to apply
     */
    // FIXME: allow an IOException (or some other checked exception) to be thrown here
    void applyRecoveredSnapshot(StateSnapshot snapshot);

    /**
     * This method is called during recovery at the end of a batch to apply the current batched commands. This method is
     * called after {@link #appendRecoveredCommand(StateCommand)}.
     */
    void applyCurrentLogRecoveryBatch();
}
