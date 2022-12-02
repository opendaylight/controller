/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Encapsulates the snapshot State for a Shard.
 *
 * @author Thomas Pantelis
 */
public final class ShardSnapshotState implements Snapshot.State {
    interface SerialForm extends Externalizable {

        ShardSnapshotState snapshotState();

        void resolveTo(@NonNull ShardSnapshotState newSnapshotState);

        Object readResolve();

        @Override
        default void readExternal(final ObjectInput in) throws IOException {
            resolveTo(ShardDataTreeSnapshot.deserialize(in));
        }

        @Override
        default void writeExternal(final ObjectOutput out) throws IOException {
            snapshotState().getSnapshot().serialize(out);
        }
    }

    @Deprecated(since = "7.0.0", forRemoval = true)
    private static final class Proxy implements SerialForm {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private ShardSnapshotState snapshotState;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        @Override
        public ShardSnapshotState snapshotState() {
            return snapshotState;
        }

        @Override
        public void resolveTo(final ShardSnapshotState newSnapshotState) {
            snapshotState = requireNonNull(newSnapshotState);
        }

        @Override
        public Object readResolve() {
            return verifyNotNull(snapshotState);
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but this class "
            + "implements writeReplace to delegate serialization to a Proxy class and thus instances of this class "
            + "aren't serialized. FindBugs does not recognize this.")
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
