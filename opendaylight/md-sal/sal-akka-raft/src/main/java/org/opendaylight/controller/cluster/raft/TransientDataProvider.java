/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import org.apache.pekko.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

final class TransientDataProvider extends NonPersistentDataProvider {
    private final ActorRef raftActor;

    TransientDataProvider(final RaftActor raftActor) {
        super(raftActor);
        this.raftActor = raftActor.self();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The way snapshotting works is:
     * <ol>
     *   <li>RaftActor calls {@link RaftActorSnapshotCohort#createSnapshot(ActorRef, java.io.OutputStream)}
     *   <li>the cohort sends a CaptureSnapshotReply and RaftActor then calls this method
     *   <li>When saveSnapshot is invoked on the akka-persistence API it uses the SnapshotStore to save
     *       the snapshot. The SnapshotStore sends SaveSnapshotSuccess or SaveSnapshotFailure. When the
     *       RaftActor gets SaveSnapshot success it commits the snapshot to the in-memory journal. This
     *       commitSnapshot is mimicking what is done in SaveSnapshotSuccess.
     * </ol>
     */
    @Override
    public void saveSnapshot(final Snapshot object) {
        // Make saving Snapshot successful
        // Committing the snapshot here would end up calling commit in the creating state which would
        // be a state violation. That's why now we send a message to commit the snapshot.
        raftActor.tell(SnapshotManager.CommitSnapshot.INSTANCE, raftActor);
    }
}
