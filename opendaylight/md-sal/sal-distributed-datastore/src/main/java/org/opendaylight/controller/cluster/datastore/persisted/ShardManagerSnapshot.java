/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.cluster.raft.persisted.MigratedSerializable;

/**
 * Represents the persisted snapshot state for the ShardManager.
 *
 * @author Thomas Pantelis
 */
public sealed class ShardManagerSnapshot implements Serializable {
    interface SerializedForm extends Externalizable {
        /**
         * Return the serial form of this object contents, corresponding to {@link ShardManagerSnapshot#shardList}.
         *
         * @return List of shards names.
         */
        List<String> shardNames();

        /**
         * Resolve this proxy to an actual {@link ShardManagerSnapshot}. Implementations can rely on the object to be
         * set via {@link #resolveTo(ShardManagerSnapshot)}.
         *
         * @return A snapshot
         */
        Object readResolve();

        /**
         * Set this proxy to return {@code snapshot} on next {@link #readResolve()}.
         *
         * @param newSnapshot Snapshot to set
         */
        void resolveTo(@NonNull ShardManagerSnapshot newSnapshot);

        @Override
        default void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final int size = in.readInt();
            final var shardList = new ArrayList<String>(size);
            for (int i = 0; i < size; i++) {
                shardList.add((String) in.readObject());
            }
            resolveTo(new ShardManagerSnapshot(shardList));
        }

        @Override
        default void writeExternal(final ObjectOutput out) throws IOException {
            final var shardList = shardNames();
            out.writeInt(shardList.size());
            for (var shardName : shardList) {
                out.writeObject(shardName);
            }
        }
    }

    @Deprecated(since = "7.0.0", forRemoval = true)
    private static final class Magnesium extends ShardManagerSnapshot implements MigratedSerializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        Magnesium(final List<String> shardList) {
            super(shardList);
        }

        @Override
        public boolean isMigrated() {
            return true;
        }
    }

    @Deprecated(since = "7.0.0", forRemoval = true)
    private static final class Proxy implements SerializedForm {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private ShardManagerSnapshot snapshot = null;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        @Override
        public List<String> shardNames() {
            return snapshot.getShardList();
        }

        @Override
        public void resolveTo(final ShardManagerSnapshot newSnapshot) {
            snapshot = requireNonNull(newSnapshot);
        }

        @Override
        public Object readResolve() {
            return new Magnesium(snapshot.getShardList());
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final List<String> shardList;

    public ShardManagerSnapshot(final @NonNull List<String> shardList) {
        this.shardList = ImmutableList.copyOf(shardList);
    }

    public final List<String> getShardList() {
        return shardList;
    }

    @java.io.Serial
    public final Object writeReplace() {
        return new SM(this);
    }

    @Override
    public final String toString() {
        return "ShardManagerSnapshot [ShardList = " + shardList + " ]";
    }
}
