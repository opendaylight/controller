/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import akka.dispatch.Futures;
import akka.japi.Option;
import akka.persistence.SelectedSnapshot;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotSelectionCriteria;
import akka.persistence.snapshot.japi.SnapshotStore;
import com.google.common.collect.Iterables;
import scala.concurrent.Future;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemorySnapshotStore extends SnapshotStore {

    Map<String, List<Snapshot>> snapshots = new HashMap<>();

    @Override public Future<Option<SelectedSnapshot>> doLoadAsync(String s,
        SnapshotSelectionCriteria snapshotSelectionCriteria) {
        List<Snapshot> snapshotList = snapshots.get(s);
        if(snapshotList == null){
            return Futures.successful(Option.<SelectedSnapshot>none());
        }

        Snapshot snapshot = Iterables.getLast(snapshotList);
        SelectedSnapshot selectedSnapshot =
            new SelectedSnapshot(snapshot.getMetadata(), snapshot.getData());
        return Futures.successful(Option.some(selectedSnapshot));
    }

    @Override public Future<Void> doSaveAsync(SnapshotMetadata snapshotMetadata, Object o) {
        List<Snapshot> snapshotList = snapshots.get(snapshotMetadata.persistenceId());

        if(snapshotList == null){
            snapshotList = new ArrayList<>();
            snapshots.put(snapshotMetadata.persistenceId(), snapshotList);
        }
        snapshotList.add(new Snapshot(snapshotMetadata, o));

        return Futures.successful(null);
    }

    @Override public void onSaved(SnapshotMetadata snapshotMetadata) throws Exception {
    }

    @Override public void doDelete(SnapshotMetadata snapshotMetadata) throws Exception {
        List<Snapshot> snapshotList = snapshots.get(snapshotMetadata.persistenceId());

        if(snapshotList == null){
            return;
        }

        int deleteIndex = -1;

        for(int i=0;i<snapshotList.size(); i++){
            Snapshot snapshot = snapshotList.get(i);
            if(snapshotMetadata.equals(snapshot.getMetadata())){
                deleteIndex = i;
                break;
            }
        }

        if(deleteIndex != -1){
            snapshotList.remove(deleteIndex);
        }

    }

    @Override public void doDelete(String s, SnapshotSelectionCriteria snapshotSelectionCriteria)
        throws Exception {
        List<Snapshot> snapshotList = snapshots.get(s);

        if(snapshotList == null){
            return;
        }

        // TODO : This is a quick and dirty implementation. Do actual match later.
        snapshotList.clear();
        snapshots.remove(s);
    }

    private static class Snapshot {
        private final SnapshotMetadata metadata;
        private final Object data;

        private Snapshot(SnapshotMetadata metadata, Object data) {
            this.metadata = metadata;
            this.data = data;
        }

        public SnapshotMetadata getMetadata() {
            return metadata;
        }

        public Object getData() {
            return data;
        }
    }
}
