/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Interface for a class that participates in raft actor snapshotting.
 *
 * @author Thomas Pantelis
 */
public interface RaftActorSnapshotCohort {

    /**
     * This method is called by the RaftActor when a snapshot needs to be
     * created. The implementation should send a CaptureSnapshotReply to the given actor.
     *
     * @param actorRef the actor to which to respond
     * @param installSnapshotStream Optional OutputStream that is present if the snapshot is to also be installed
     *        on a follower. The implementation must serialize its state to the OutputStream and return the
     *        installSnapshotStream instance in the CaptureSnapshotReply along with the snapshot State instance.
     *        The snapshot State is serialized directly to the snapshot store while the OutputStream is used to send
     *        the state data to follower(s) in chunks. The {@link #deserializeSnapshot} method is used to convert the
     *        serialized data back to a State instance on the follower end. The serialization for snapshot install is
     *        passed off so the cost of serialization is not charged to the raft actor's thread.
     */
    void createSnapshot(@Nonnull ActorRef actorRef, @Nonnull Optional<OutputStream> installSnapshotStream);

    /**
     * This method is called to apply a snapshot installed by the leader.
     *
     * @param snapshotState a snapshot of the state of the actor
     */
    void applySnapshot(@Nonnull Snapshot.State snapshotState);

    /**
     * This method is called to de-serialize snapshot data that was previously serialized via {@link #createSnapshot}
     * to a State instance.
     *
     * @param snapshotBytes the ByteSource containing the serialized data
     * @return the converted snapshot State
     * @throws IOException if an error occurs accessing the ByteSource or de-serializing
     */
    @Nonnull
    Snapshot.State deserializeSnapshot(@Nonnull ByteSource snapshotBytes) throws IOException;
}
