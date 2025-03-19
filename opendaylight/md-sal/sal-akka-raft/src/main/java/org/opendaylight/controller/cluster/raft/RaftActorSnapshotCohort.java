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
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;

/**
 * Interface for a class that participates in raft actor snapshotting.
 *
 * @author Thomas Pantelis
 */
public interface RaftActorSnapshotCohort {
    /**
     * This method is called by the RaftActor when a snapshot needs to be created.
     *
     * @return a {@link State}
     */
    State createSnapshot();

    /**
     * This method is called to apply a snapshot installed by the leader.
     *
     * @param snapshotState a snapshot of the state of the actor
     */
    void applySnapshot(@NonNull State snapshotState);

    /**
     * This method is called to de-serialize snapshot data that was previously serialized via {@link #createSnapshot}
     * to a State instance.
     *
     * @param snapshotBytes the ByteSource containing the serialized data
     * @return the converted snapshot State
     * @throws IOException if an error occurs accessing the ByteSource or de-serializing
     */
    @NonNull State deserializeSnapshot(@NonNull ByteSource snapshotBytes) throws IOException;
}
