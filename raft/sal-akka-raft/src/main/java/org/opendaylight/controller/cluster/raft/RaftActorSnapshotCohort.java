/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;

/**
 * Interface for a class that participates in raft actor snapshotting.
 *
 * @param <T> type of state
 * @author Thomas Pantelis
 */
public interface RaftActorSnapshotCohort<T extends State> {
    /**
     * Return the type of state supported by this cohort.
     *
     * @return the state class
     */
    @NonNull Class<T> stateClass();

    /**
     * Take a snapshot of current state.
     *
     * @return current snapshot state
     */
    @NonNull T takeSnapshot();

    /**
     * This method is called to apply a snapshot installed by the leader.
     *
     * @param snapshotState a snapshot of the state of the actor
     */
    void applySnapshot(@NonNull T snapshotState);

    /**
     * This method is called to de-serialize snapshot data that was previously serialized via {@link #createSnapshot}
     * to a State instance.
     *
     * @param snapshotBytes the ByteSource containing the serialized data
     * @return the converted snapshot State
     * @throws IOException if an error occurs accessing the ByteSource or de-serializing
     */
    @NonNull T deserializeSnapshot(@NonNull ByteSource snapshotBytes) throws IOException;

    void serializeSnapshot(@NonNull T snapshotState, @NonNull OutputStream out) throws IOException;
}
