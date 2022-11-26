/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot.ShardSnapshot;

/**
 * Serialization proxy for {@link DatastoreSnapshot}.
 */
final class DS implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private DatastoreSnapshot datastoreSnapshot;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public DS() {
        // For Externalizable
    }

    DS(final DatastoreSnapshot datastoreSnapshot) {
        this.datastoreSnapshot = requireNonNull(datastoreSnapshot);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final var type = (String) in.readObject();
        final var snapshot = (ShardManagerSnapshot) in.readObject();

        final int size = in.readInt();
        var localShardSnapshots = new ArrayList<ShardSnapshot>(size);
        for (int i = 0; i < size; i++) {
            localShardSnapshots.add((ShardSnapshot) in.readObject());
        }

        datastoreSnapshot = new DatastoreSnapshot(type, snapshot, localShardSnapshots);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(datastoreSnapshot.getType());
        out.writeObject(datastoreSnapshot.getShardManagerSnapshot());

        final var shardSnapshots = datastoreSnapshot.getShardSnapshots();
        out.writeInt(shardSnapshots.size());
        for (var shardSnapshot : shardSnapshots) {
            out.writeObject(shardSnapshot);
        }
    }

    @java.io.Serial
    private Object readResolve() {
        return verifyNotNull(datastoreSnapshot);
    }
}
