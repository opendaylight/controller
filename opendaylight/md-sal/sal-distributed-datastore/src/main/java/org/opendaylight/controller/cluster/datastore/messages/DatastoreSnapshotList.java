/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.util.ArrayList;
import java.util.List;

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

    private Object readResolve() {
        List<org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot> snapshots =
                new ArrayList<>(size());
        for (DatastoreSnapshot legacy: this) {
            snapshots.add(new org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot(
                    legacy.getType(), legacy.getShardManagerSnapshot(), fromLegacy(legacy.getShardSnapshots())));
        }

        return new org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshotList(snapshots);
    }

    private List<org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot.ShardSnapshot> fromLegacy(
            List<DatastoreSnapshot.ShardSnapshot> from) {
        List<org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot.ShardSnapshot> snapshots =
                new ArrayList<>(from.size());
        for (DatastoreSnapshot.ShardSnapshot legacy: from) {
            snapshots.add(new org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot.ShardSnapshot(
                    legacy.getName(), legacy.getSnapshot()));
        }

        return snapshots;
    }
}
