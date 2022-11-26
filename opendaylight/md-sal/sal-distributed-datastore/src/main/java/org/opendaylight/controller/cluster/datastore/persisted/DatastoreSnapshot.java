/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Stores a snapshot of the internal state of a data store.
 *
 * @author Thomas Pantelis
 */
public final class DatastoreSnapshot implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull String type;
    private final ShardManagerSnapshot shardManagerSnapshot;
    private final @NonNull ImmutableList<ShardSnapshot> shardSnapshots;

    public DatastoreSnapshot(final @NonNull String type, final @Nullable ShardManagerSnapshot shardManagerSnapshot,
            final @NonNull List<ShardSnapshot> shardSnapshots) {
        this.type = requireNonNull(type);
        this.shardManagerSnapshot = shardManagerSnapshot;
        this.shardSnapshots = ImmutableList.copyOf(shardSnapshots);
    }

    public @NonNull String getType() {
        return type;
    }

    public @Nullable ShardManagerSnapshot getShardManagerSnapshot() {
        return shardManagerSnapshot;
    }

    public @NonNull List<ShardSnapshot> getShardSnapshots() {
        return shardSnapshots;
    }

    @java.io.Serial
    private Object writeReplace() {
        return new DS(this);
    }

    public static final class ShardSnapshot implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private final @NonNull String name;
        private final @NonNull Snapshot snapshot;

        public ShardSnapshot(final @NonNull String name, final @NonNull Snapshot snapshot) {
            this.name = requireNonNull(name);
            this.snapshot = requireNonNull(snapshot);
        }

        public @NonNull String getName() {
            return name;
        }

        public @NonNull Snapshot getSnapshot() {
            return snapshot;
        }

        @java.io.Serial
        private Object writeReplace() {
            return new DSS(this);
        }
    }
}
