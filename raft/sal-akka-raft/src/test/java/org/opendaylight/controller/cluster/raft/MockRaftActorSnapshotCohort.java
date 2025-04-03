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
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.raft.spi.StreamSource;

/**
 * {@link RaftActorSnapshotCohort} corresponding to {@link MockSnapshotState}.
 */
@NonNullByDefault
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
    default void writeSnapshot(final MockSnapshotState snapshot, final OutputStream out) throws IOException {
        try (var oos = new ObjectOutputStream(out)) {
            oos.writeObject(snapshot);
        }
    }

    @Override
    default MockSnapshotState readSnapshot(final StreamSource source) throws IOException {
        try (var ois = new ObjectInputStream(source.openStream())) {
            return assertInstanceOf(MockSnapshotState.class, ois.readObject());
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }
}
