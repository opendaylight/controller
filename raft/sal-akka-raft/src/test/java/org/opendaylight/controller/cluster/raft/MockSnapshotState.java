/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;

public record MockSnapshotState(List<Object> state) implements Snapshot.State {
    @NonNullByDefault
    public static final StateSnapshot.Support<MockSnapshotState> SUPPORT = new Support<>() {
        @Override
        public Class<MockSnapshotState> snapshotType() {
            return MockSnapshotState.class;
        }

        @Override
        public Reader<MockSnapshotState> reader() {
            return in -> {
                try (var ois = new ObjectInputStream(in)) {
                    return assertInstanceOf(MockSnapshotState.class, ois.readObject());
                } catch (ClassNotFoundException e) {
                    throw new AssertionError(e);
                }
            };
        }

        @Override
        public Writer<MockSnapshotState> writer() {
            return (snapshot, out) -> {
                try (var oos = new ObjectOutputStream(out)) {
                    oos.writeObject(snapshot);
                }
            };
        }
    };

    public MockSnapshotState {
        requireNonNull(state);
    }
}
