/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Verify.verifyNotNull;
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
    interface SerialForm extends Externalizable {

        DatastoreSnapshot datastoreSnapshot();

        Object readResolve();

        void resolveTo(@NonNull DatastoreSnapshot newDatastoreSnapshot);

        @Override
        default void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final var type = (String)in.readObject();
            final var snapshot = (ShardManagerSnapshot) in.readObject();

            final int size = in.readInt();
            var localShardSnapshots = new ArrayList<ShardSnapshot>(size);
            for (int i = 0; i < size; i++) {
                localShardSnapshots.add((ShardSnapshot) in.readObject());
            }

            resolveTo(new DatastoreSnapshot(type, snapshot, localShardSnapshots));
        }

        @Override
        default void writeExternal(final ObjectOutput out) throws IOException {
            final var datastoreSnapshot = datastoreSnapshot();
            out.writeObject(datastoreSnapshot.type);
            out.writeObject(datastoreSnapshot.shardManagerSnapshot);

            out.writeInt(datastoreSnapshot.shardSnapshots.size());
            for (ShardSnapshot shardSnapshot: datastoreSnapshot.shardSnapshots) {
                out.writeObject(shardSnapshot);
            }
        }
    }

    private static final class Proxy implements SerialForm {
        private static final long serialVersionUID = 1L;

        private DatastoreSnapshot datastoreSnapshot;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        @Override
        public DatastoreSnapshot datastoreSnapshot() {
            return datastoreSnapshot;
        }

        @Override
        public void resolveTo(final DatastoreSnapshot newDatastoreSnapshot) {
            datastoreSnapshot = requireNonNull(newDatastoreSnapshot);
        }

        @Override
        public Object readResolve() {
            return verifyNotNull(datastoreSnapshot);
        }
    }

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
        interface SerialForm extends Externalizable {

            ShardSnapshot shardSnapshot();

            Object readResolve();

            void resolveTo(String name, Snapshot snapshot);

            @Override
            default void writeExternal(final ObjectOutput out) throws IOException {
                final var shardSnapshot = shardSnapshot();
                out.writeObject(shardSnapshot.name);
                out.writeObject(shardSnapshot.snapshot);
            }

            @Override
            default void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                resolveTo((String) in.readObject(), (Snapshot) in.readObject());
            }
        }

        private static final class Proxy implements SerialForm {
            @java.io.Serial
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
            public ShardSnapshot shardSnapshot() {
                return shardSnapshot;
            }

            @Override
            public void resolveTo(final String name, final Snapshot snapshot) {
                shardSnapshot = new ShardSnapshot(name, snapshot);
            }

            @Override
            public Object readResolve() {
                return verifyNotNull(shardSnapshot);
            }
        }

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
            return new Proxy(this);
        }
    }
}
