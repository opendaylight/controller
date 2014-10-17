/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.persistence;

import akka.dispatch.Futures;
import akka.japi.Option;
import akka.persistence.SelectedSnapshot;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotSelectionCriteria;
import akka.persistence.snapshot.japi.SnapshotStore;
import scala.concurrent.Future;

/**
 * This is really a do nothing snapshot store
 */
public class NonPersistentSnapshotStore extends SnapshotStore {
    @Override
    public Future<Option<SelectedSnapshot>> doLoadAsync(String s, SnapshotSelectionCriteria snapshotSelectionCriteria) {
        return Futures.successful(Option.<SelectedSnapshot>none());
    }

    @Override
    public Future<Void> doSaveAsync(SnapshotMetadata snapshotMetadata, Object o) {
        return Futures.successful(null);
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
