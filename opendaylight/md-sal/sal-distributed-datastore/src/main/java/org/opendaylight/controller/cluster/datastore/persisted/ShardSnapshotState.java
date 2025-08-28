/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Encapsulates the snapshot State for a Shard.
 *
 * @author Thomas Pantelis
 */
public final class ShardSnapshotState implements Snapshot.State {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @NonNullByDefault
    public static final Support<ShardSnapshotState> SUPPORT = new Support<>() {
        @Override
        public Class<ShardSnapshotState> snapshotType() {
            return ShardSnapshotState.class;
        }

        @Override
        public Reader<ShardSnapshotState> reader() {
            return in -> {
                try (var oin = new ObjectInputStream(in)) {
                    return ShardDataTreeSnapshot.deserialize(oin);
                }
            };
        }

        @Override
        public Writer<ShardSnapshotState> writer() {
            return (snapshot, out) -> {
                try (var oos = new ObjectOutputStream(out)) {
                    snapshot.getSnapshot().serialize(oos);
                }
            };
        }
    };

    private final @NonNull ShardDataTreeSnapshot snapshot;
    private final boolean migrated;

    @VisibleForTesting
    public ShardSnapshotState(final @NonNull ShardDataTreeSnapshot snapshot, final boolean migrated) {
        this.snapshot = requireNonNull(snapshot);
        this.migrated = migrated;
    }

    public ShardSnapshotState(final @NonNull ShardDataTreeSnapshot snapshot) {
        this(snapshot, false);
    }

    public @NonNull ShardDataTreeSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public boolean needsMigration() {
        return migrated;
    }

    @java.io.Serial
    private Object writeReplace() {
        return new SS(this);
    }
}
