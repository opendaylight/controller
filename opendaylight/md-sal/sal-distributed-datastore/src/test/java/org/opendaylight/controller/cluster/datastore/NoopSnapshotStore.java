/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import scala.concurrent.Future;
import akka.dispatch.Futures;
import akka.japi.Option;
import akka.persistence.SelectedSnapshot;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotSelectionCriteria;
import akka.persistence.snapshot.japi.SnapshotStore;

public class NoopSnapshotStore extends SnapshotStore {

    @Override
    public Future<Option<SelectedSnapshot>> doLoadAsync(String persistenceId,
            SnapshotSelectionCriteria criteria) {
        return Futures.successful(Option.<SelectedSnapshot>none());
    }

    @Override
    public Future<Void> doSaveAsync(SnapshotMetadata metadata, Object snapshot) {
        return Futures.successful(null);
    }

    @Override
    public void onSaved(SnapshotMetadata metadata) throws Exception {
    }

    @Override
    public void doDelete(SnapshotMetadata metadata) throws Exception {
    }

    @Override
    public void doDelete(String persistenceId, SnapshotSelectionCriteria criteria) throws Exception {
    }
}
