/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Encapsulates the snapshot State for a Shard.
 *
 * @author Thomas Pantelis
 */
public class ShardSnapshotState implements Snapshot.State {
    private static final long serialVersionUID = 1L;

    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private ShardSnapshotState snapshotState;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final ShardSnapshotState snapshotState) {
            this.snapshotState = snapshotState;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            snapshotState.snapshot.serialize(out);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            snapshotState = new ShardSnapshotState(ShardDataTreeSnapshot.deserialize(in));
        }

        private Object readResolve() {
            return snapshotState;
        }
    }

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but this class "
            + "implements writeReplace to delegate serialization to a Proxy class and thus instances of this class "
            + "aren't serialized. FindBugs does not recognize this.")
    private final ShardDataTreeSnapshot snapshot;

    public ShardSnapshotState(@Nonnull final ShardDataTreeSnapshot snapshot) {
        this.snapshot = Preconditions.checkNotNull(snapshot);
    }

    @Nonnull
    public ShardDataTreeSnapshot getSnapshot() {
        return snapshot;
    }

    private Object writeReplace() {
        return new Proxy(this);
    }
}
