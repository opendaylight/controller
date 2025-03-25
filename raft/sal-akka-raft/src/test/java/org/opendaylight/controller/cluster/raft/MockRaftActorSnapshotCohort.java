/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import org.apache.pekko.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.raft.spi.InputStreamProvider;

/**
 * {@link RaftActorSnapshotCohort} corresponding to {@link MockSnapshotState}.
 */
public interface MockRaftActorSnapshotCohort extends RaftActorSnapshotCohort<MockSnapshotState> {
    @Override
    default Class<MockSnapshotState> stateClass() {
        return MockSnapshotState.class;
    }

    @Override
    default void applySnapshot(final MockSnapshotState snapshotState) {
        // No-op
    }

    @Override
    default void serializeSnapshot(final MockSnapshotState snapshotState, final OutputStream out) throws IOException {
        try (var oos = new ObjectOutputStream(out)) {
            oos.writeObject(snapshotState);
        }
    }

    @Override
    default MockSnapshotState deserializeSnapshot(final InputStreamProvider snapshotBytes) throws IOException {
        try (var ois = new ObjectInputStream(snapshotBytes.openStream())) {
            return assertInstanceOf(MockSnapshotState.class, ois.readObject());
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    default void createSnapshot(final ActorRef actorRef, final OutputStream installSnapshotStream) {
        actorRef.tell(new CaptureSnapshotReply(takeSnapshot(), installSnapshotStream), actorRef);
    }
}
