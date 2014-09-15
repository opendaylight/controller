/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.utils;

import akka.dispatch.Futures;
import akka.japi.Option;
import akka.persistence.SelectedSnapshot;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotSelectionCriteria;
import akka.persistence.snapshot.japi.SnapshotStore;
import org.opendaylight.controller.cluster.raft.Snapshot;
import scala.concurrent.Future;


public class MockSnapshotStore  extends SnapshotStore {

    private static Snapshot mockSnapshot;
    private static String persistenceId;

    public static void setMockSnapshot(Snapshot s) {
        mockSnapshot = s;
    }

    public static void setPersistenceId(String pId) {
        persistenceId = pId;
    }

    @Override
    public Future<Option<SelectedSnapshot>> doLoadAsync(String s, SnapshotSelectionCriteria snapshotSelectionCriteria) {
        if (mockSnapshot == null) {
            return Futures.successful(Option.<SelectedSnapshot>none());
        }

        SnapshotMetadata smd = new SnapshotMetadata(persistenceId, 1, 12345);
        SelectedSnapshot selectedSnapshot =
            new SelectedSnapshot(smd, mockSnapshot);
        return Futures.successful(Option.some(selectedSnapshot));
    }

    @Override
    public Future<Void> doSaveAsync(SnapshotMetadata snapshotMetadata, Object o) {
        return null;
    }

    @Override
    public void onSaved(SnapshotMetadata snapshotMetadata) throws Exception {

    }

    @Override
    public void doDelete(SnapshotMetadata snapshotMetadata) throws Exception {

    }

    @Override
    public void doDelete(String s, SnapshotSelectionCriteria snapshotSelectionCriteria) throws Exception {

    }
}
