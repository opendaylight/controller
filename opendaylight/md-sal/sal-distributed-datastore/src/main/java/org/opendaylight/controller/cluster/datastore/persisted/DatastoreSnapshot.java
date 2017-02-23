/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Stores a snapshot of the internal state of a data store.
 *
 * @author Thomas Pantelis
 */
public class DatastoreSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

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
            String type = (String)in.readObject();
            ShardManagerSnapshot shardManagerSnapshot = (ShardManagerSnapshot) in.readObject();

            int size = in.readInt();
            List<ShardSnapshot> shardSnapshots = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                shardSnapshots.add((ShardSnapshot) in.readObject());
            }

            datastoreSnapshot = new DatastoreSnapshot(type, shardManagerSnapshot, shardSnapshots);
        }

        private Object readResolve() {
            return datastoreSnapshot;
        }
    }

    private final String type;
    private final ShardManagerSnapshot shardManagerSnapshot;
    private final List<ShardSnapshot> shardSnapshots;

    public DatastoreSnapshot(@Nonnull String type, @Nullable ShardManagerSnapshot shardManagerSnapshot,
            @Nonnull List<ShardSnapshot> shardSnapshots) {
        this.type = Preconditions.checkNotNull(type);
        this.shardManagerSnapshot = shardManagerSnapshot;
        this.shardSnapshots = ImmutableList.copyOf(Preconditions.checkNotNull(shardSnapshots));
    }

    @Nonnull
    public String getType() {
        return type;
    }

    @Nullable
    public ShardManagerSnapshot getShardManagerSnapshot() {
        return shardManagerSnapshot;
    }

    @Nonnull
    public List<ShardSnapshot> getShardSnapshots() {
        return shardSnapshots;
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    public static class ShardSnapshot implements Serializable {
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

        private final String name;
        private final Snapshot snapshot;

        public ShardSnapshot(@Nonnull String name, @Nonnull Snapshot snapshot) {
            this.name = Preconditions.checkNotNull(name);
            this.snapshot = Preconditions.checkNotNull(snapshot);
        }

        @Nonnull
        public String getName() {
            return name;
        }

        @Nonnull
        public Snapshot getSnapshot() {
            return snapshot;
        }

        private Object writeReplace() {
            return new Proxy(this);
        }
    }
}
