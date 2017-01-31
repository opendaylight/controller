/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores a list of DatastoreSnapshot instances.
 */
public class DatastoreSnapshotList extends ArrayList<DatastoreSnapshot> {
    private static final long serialVersionUID = 1L;

    public DatastoreSnapshotList() {
    }

    public DatastoreSnapshotList(List<DatastoreSnapshot> snapshots) {
        super(snapshots);
    }
}
