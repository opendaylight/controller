/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;

/**
 * RaftActorSnapshotCohort implementation that does nothing.
 *
 * @author Thomas Pantelis
 */
public final class NoopRaftActorSnapshotCohort implements RaftActorSnapshotCohort {
    public static final NoopRaftActorSnapshotCohort INSTANCE = new NoopRaftActorSnapshotCohort();

    private NoopRaftActorSnapshotCohort() {
    }

    @Override
    public void createSnapshot(ActorRef actorRef, Optional<OutputStream> installSnapshotStream) {
    }

    @Override
    public void applySnapshot(State snapshotState) {
    }

    @Override
    public State deserializeSnapshot(ByteSource snapshotBytes) throws IOException {
        return EmptyState.INSTANCE;
    }
}
