/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * An akka SnapshotStore implementation that stores data in memory. This is intended for testing.
 *
 * @author Thomas Pantelis
 */
public class InMemorySnapshotStore extends SnapshotStore {

    static final Logger LOG = LoggerFactory.getLogger(InMemorySnapshotStore.class);

    private static Map<String, List<StoredSnapshot>> snapshots = new ConcurrentHashMap<>();
    private static final Map<String, CountDownLatch> snapshotSavedLatches = new ConcurrentHashMap<>();

    public static void addSnapshot(String persistentId, Object snapshot) {
        List<StoredSnapshot> snapshotList = snapshots.get(persistentId);

        if(snapshotList == null) {
            snapshotList = new ArrayList<>();
            snapshots.put(persistentId, snapshotList);
        }

        synchronized (snapshotList) {
            snapshotList.add(new StoredSnapshot(new SnapshotMetadata(persistentId, snapshotList.size(),
                    System.currentTimeMillis()), snapshot));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getSnapshots(String persistentId, Class<T> type) {
        List<StoredSnapshot> stored = snapshots.get(persistentId);
        if(stored == null) {
            return Collections.emptyList();
        }

        List<T> retList;
        synchronized (stored) {
            retList = Lists.newArrayListWithCapacity(stored.size());
            for(StoredSnapshot s: stored) {
                if(type.isInstance(s.getData())) {
                    retList.add((T) s.getData());
                }
            }
        }

        return retList;
    }

    public static void clear() {
        snapshots.clear();
    }

    public static void addSnapshotSavedLatch(String persistenceId) {
        snapshotSavedLatches.put(persistenceId, new CountDownLatch(1));
    }

    public static <T> T waitForSavedSnapshot(String persistenceId, Class<T> type) {
        if(!Uninterruptibles.awaitUninterruptibly(snapshotSavedLatches.get(persistenceId), 5, TimeUnit.SECONDS)) {
            throw new AssertionError("Snapshot was not saved");
        }

        return getSnapshots(persistenceId, type).get(0);
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
        synchronized (snapshotList) {
            snapshotList.add(new StoredSnapshot(snapshotMetadata, o));
        }

        CountDownLatch latch = snapshotSavedLatches.get(snapshotMetadata.persistenceId());
        if(latch != null) {
            latch.countDown();
        }

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

        synchronized (snapshotList) {
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
    }

    @Override
    public void doDelete(String persistentId, SnapshotSelectionCriteria snapshotSelectionCriteria)
        throws Exception {
        List<StoredSnapshot> snapshotList = snapshots.get(persistentId);

        if(snapshotList == null){
            return;
        }

        synchronized (snapshotList) {
            Iterator<StoredSnapshot> iter = snapshotList.iterator();
            while(iter.hasNext()) {
                StoredSnapshot s = iter.next();
                LOG.trace("doDelete: sequenceNr: {}, maxSequenceNr: {}", s.getMetadata().sequenceNr(),
                        snapshotSelectionCriteria.maxSequenceNr());

                if(s.getMetadata().sequenceNr() <= snapshotSelectionCriteria.maxSequenceNr()) {
                    iter.remove();
                }
            }
        }
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
