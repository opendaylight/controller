/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.raft.Snapshot;

/**
 * Stores a list of DatastoreSnapshot instances.
 *
 * @deprecated Use {@link org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshotList} instead.
 */
@Deprecated
public class DatastoreSnapshotList extends ArrayList<DatastoreSnapshot> {
    private static final long serialVersionUID = 1L;

    public DatastoreSnapshotList() {
    }

    public DatastoreSnapshotList(List<DatastoreSnapshot> snapshots) {
        super(snapshots);
    }

    private Object readResolve() throws IOException, ClassNotFoundException {
        List<org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot> snapshots =
                new ArrayList<>(size());
        for (DatastoreSnapshot legacy: this) {
            snapshots.add(new org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot(
                    legacy.getType(), legacy.getShardManagerSnapshot(), fromLegacy(legacy.getShardSnapshots())));
        }

        return new org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshotList(snapshots);
    }

    private static List<org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot.ShardSnapshot>
            fromLegacy(List<DatastoreSnapshot.ShardSnapshot> from) throws IOException, ClassNotFoundException {
        List<org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot.ShardSnapshot> snapshots =
                new ArrayList<>(from.size());
        for (DatastoreSnapshot.ShardSnapshot legacy: from) {
            snapshots.add(new org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot.ShardSnapshot(
                    legacy.getName(), deserialize(legacy.getSnapshot())));
        }

        return snapshots;
    }

    private static org.opendaylight.controller.cluster.raft.persisted.Snapshot deserialize(byte[] bytes)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Snapshot legacy = (Snapshot) ois.readObject();

            ShardDataTreeSnapshot shardDataTreeSnapshot = ShardDataTreeSnapshot.deserializePreCarbon(legacy.getState());
            return org.opendaylight.controller.cluster.raft.persisted.Snapshot.create(
                    new ShardSnapshotState(shardDataTreeSnapshot), legacy.getUnAppliedEntries(), legacy.getLastIndex(),
                    legacy.getLastTerm(), legacy.getLastAppliedIndex(), legacy.getLastAppliedTerm(),
                    legacy.getElectionTerm(), legacy.getElectionVotedFor(), legacy.getServerConfiguration());
        }
    }
}
