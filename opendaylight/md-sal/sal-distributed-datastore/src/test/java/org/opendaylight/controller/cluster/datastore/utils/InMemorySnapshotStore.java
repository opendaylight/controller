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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.cluster.raft.Snapshot;

public class InMemorySnapshotStore extends SnapshotStore {

    private static Map<String, List<StoredSnapshot>> snapshots = new ConcurrentHashMap<>();

    public static void addSnapshot(String persistentId, Snapshot snapshot) {
        List<StoredSnapshot> snapshotList = snapshots.get(persistentId);

        if(snapshotList == null) {
            snapshotList = new ArrayList<>();
            snapshots.put(persistentId, snapshotList);
        }

        snapshotList.add(new StoredSnapshot(new SnapshotMetadata(persistentId, snapshotList.size(),
                System.currentTimeMillis()), snapshot));
    }

    public static void clear() {
        snapshots.clear();
    }

    @Override
    public Future<Option<SelectedSnapshot>> doLoadAsync(String s,
        SnapshotSelectionCriteria snapshotSelectionCriteria) {
        List<StoredSnapshot> snapshotList = snapshots.get(s);
        if(snapshotList == null){
            return Futures.successful(Option.<SelectedSnapshot>none());
        }

        StoredSnapshot snapshot = Iterables.getLast(snapshotList);
        SelectedSnapshot selectedSnapshot =
            new SelectedSnapshot(snapshot.getMetadata(), snapshot.getData());
        return Futures.successful(Option.some(selectedSnapshot));
    }

    @Override
    public Future<Void> doSaveAsync(SnapshotMetadata snapshotMetadata, Object o) {
        List<StoredSnapshot> snapshotList = snapshots.get(snapshotMetadata.persistenceId());

        if(snapshotList == null){
            snapshotList = new ArrayList<>();
            snapshots.put(snapshotMetadata.persistenceId(), snapshotList);
        }
        snapshotList.add(new StoredSnapshot(snapshotMetadata, o));

        return Futures.successful(null);
    }

    @Override
    public void onSaved(SnapshotMetadata snapshotMetadata) throws Exception {
    }

    @Override
    public void doDelete(SnapshotMetadata snapshotMetadata) throws Exception {
        List<StoredSnapshot> snapshotList = snapshots.get(snapshotMetadata.persistenceId());

        if(snapshotList == null){
            return;
        }

        int deleteIndex = -1;

        for(int i=0;i<snapshotList.size(); i++){
            StoredSnapshot snapshot = snapshotList.get(i);
            if(snapshotMetadata.equals(snapshot.getMetadata())){
                deleteIndex = i;
                break;
            }
        }

        if(deleteIndex != -1){
            snapshotList.remove(deleteIndex);
        }

    }

    @Override
    public void doDelete(String s, SnapshotSelectionCriteria snapshotSelectionCriteria)
        throws Exception {
        List<StoredSnapshot> snapshotList = snapshots.get(s);

        if(snapshotList == null){
            return;
        }

        // TODO : This is a quick and dirty implementation. Do actual match later.
        snapshotList.clear();
        snapshots.remove(s);
    }

    private static class StoredSnapshot {
        private final SnapshotMetadata metadata;
        private final Object data;

        private StoredSnapshot(SnapshotMetadata metadata, Object data) {
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
