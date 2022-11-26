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
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
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
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private DatastoreSnapshot datastoreSnapshot;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final DatastoreSnapshot datastoreSnapshot) {
            this.datastoreSnapshot = datastoreSnapshot;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(datastoreSnapshot.type);
            out.writeObject(datastoreSnapshot.shardManagerSnapshot);

            out.writeInt(datastoreSnapshot.shardSnapshots.size());
            for (ShardSnapshot shardSnapshot: datastoreSnapshot.shardSnapshots) {
                out.writeObject(shardSnapshot);
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            String localType = (String)in.readObject();
            ShardManagerSnapshot localShardManagerSnapshot = (ShardManagerSnapshot) in.readObject();

            int size = in.readInt();
            List<ShardSnapshot> localShardSnapshots = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                localShardSnapshots.add((ShardSnapshot) in.readObject());
            }

            datastoreSnapshot = new DatastoreSnapshot(localType, localShardManagerSnapshot, localShardSnapshots);
        }

        private Object readResolve() {
            return datastoreSnapshot;
        }
    }

    private static final long serialVersionUID = 1L;

    private final String type;
    private final ShardManagerSnapshot shardManagerSnapshot;
    private final List<ShardSnapshot> shardSnapshots;

    public DatastoreSnapshot(@NonNull String type, @Nullable ShardManagerSnapshot shardManagerSnapshot,
            @NonNull List<ShardSnapshot> shardSnapshots) {
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

    private Object writeReplace() {
        return new Proxy(this);
    }

    public static final class ShardSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;

        private static final class Proxy implements Externalizable {
            private static final long serialVersionUID = 1L;

            private ShardSnapshot shardSnapshot;

            // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
            // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
            @SuppressWarnings("checkstyle:RedundantModifier")
            public Proxy() {
                // For Externalizable
            }

            Proxy(final ShardSnapshot shardSnapshot) {
                this.shardSnapshot = shardSnapshot;
            }

            @Override
            public void writeExternal(ObjectOutput out) throws IOException {
                out.writeObject(shardSnapshot.name);
                out.writeObject(shardSnapshot.snapshot);
            }

            @Override
            public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
                shardSnapshot = new ShardSnapshot((String)in.readObject(), (Snapshot) in.readObject());
            }

            private Object readResolve() {
                return shardSnapshot;
            }
        }

        private final @NonNull String name;
        private final @NonNull Snapshot snapshot;

        public ShardSnapshot(@NonNull String name, @NonNull Snapshot snapshot) {
            this.name = requireNonNull(name);
            this.snapshot = requireNonNull(snapshot);
        }

        public @NonNull String getName() {
            return name;
        }

        public @NonNull Snapshot getSnapshot() {
            return snapshot;
        }

        private Object writeReplace() {
            return new Proxy(this);
        }
    }
}
