/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.utils;

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.persistence.SelectedSnapshot;
import org.apache.pekko.persistence.SnapshotMetadata;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.apache.pekko.persistence.snapshot.japi.SnapshotStore;
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

    private static final Map<String, CountDownLatch> SNAPSHOT_SAVED_LATCHES = new ConcurrentHashMap<>();
    private static final Map<String, CountDownLatch> SNAPSHOT_DELETED_LATCHES = new ConcurrentHashMap<>();
    private static Map<String, List<StoredSnapshot>> snapshots = new ConcurrentHashMap<>();

    public static void addSnapshot(final String persistentId, final Object snapshot) {
        List<StoredSnapshot> snapshotList = snapshots.computeIfAbsent(persistentId, k -> new ArrayList<>());

        synchronized (snapshotList) {
            snapshotList.add(new StoredSnapshot(new SnapshotMetadata(persistentId, snapshotList.size(),
                    System.currentTimeMillis()), snapshot));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getSnapshots(final String persistentId, final Class<T> type) {
        List<StoredSnapshot> stored = snapshots.get(persistentId);
        if (stored == null) {
            return Collections.emptyList();
        }

        List<T> retList;
        synchronized (stored) {
            retList = new ArrayList<>(stored.size());
            for (StoredSnapshot s: stored) {
                if (type.isInstance(s.data)) {
                    retList.add((T) s.data);
                }
            }
        }

        return retList;
    }

    public static void clearSnapshotsFor(final String persistenceId) {
        snapshots.remove(persistenceId);
    }

    public static void clear() {
        snapshots.clear();
    }

    public static void addSnapshotSavedLatch(final String persistenceId) {
        SNAPSHOT_SAVED_LATCHES.put(persistenceId, new CountDownLatch(1));
    }

    public static void addSnapshotDeletedLatch(final String persistenceId) {
        SNAPSHOT_DELETED_LATCHES.put(persistenceId, new CountDownLatch(1));
    }

    public static <T> T waitForSavedSnapshot(final String persistenceId, final Class<T> type) {
        if (!Uninterruptibles.awaitUninterruptibly(SNAPSHOT_SAVED_LATCHES.get(persistenceId), 5, TimeUnit.SECONDS)) {
            throw new AssertionError("Snapshot was not saved");
        }

        return getSnapshots(persistenceId, type).get(0);
    }

    public static void waitForDeletedSnapshot(final String persistenceId) {
        if (!Uninterruptibles.awaitUninterruptibly(SNAPSHOT_DELETED_LATCHES.get(persistenceId), 5, TimeUnit.SECONDS)) {
            throw new AssertionError("Snapshot was not deleted");
        }
    }

    @Override
    public Future<Optional<SelectedSnapshot>> doLoadAsync(final String persistenceId,
            final SnapshotSelectionCriteria snapshotSelectionCriteria) {
        List<StoredSnapshot> snapshotList = snapshots.get(persistenceId);
        if (snapshotList == null) {
            return Futures.successful(Optional.<SelectedSnapshot>empty());
        }

        synchronized (snapshotList) {
            for (int i = snapshotList.size() - 1; i >= 0; i--) {
                StoredSnapshot snapshot = snapshotList.get(i);
                if (matches(snapshot, snapshotSelectionCriteria)) {
                    return Futures.successful(Optional.of(new SelectedSnapshot(snapshot.metadata,
                            snapshot.data)));
                }
            }
        }

        return Futures.successful(Optional.<SelectedSnapshot>empty());
    }

    private static boolean matches(final StoredSnapshot snapshot, final SnapshotSelectionCriteria criteria) {
        return snapshot.metadata.sequenceNr() <= criteria.maxSequenceNr()
                && snapshot.metadata.timestamp() <= criteria.maxTimestamp();
    }

    @Override
    public Future<Void> doSaveAsync(final SnapshotMetadata snapshotMetadata, final Object obj) {
        List<StoredSnapshot> snapshotList = snapshots.get(snapshotMetadata.persistenceId());

        LOG.trace("doSaveAsync: persistentId {}: sequenceNr: {}: timestamp {}: {}", snapshotMetadata.persistenceId(),
                snapshotMetadata.sequenceNr(), snapshotMetadata.timestamp(), obj);

        if (snapshotList == null) {
            snapshotList = new ArrayList<>();
            snapshots.put(snapshotMetadata.persistenceId(), snapshotList);
        }
        synchronized (snapshotList) {
            snapshotList.add(new StoredSnapshot(snapshotMetadata, obj));
        }

        CountDownLatch latch = SNAPSHOT_SAVED_LATCHES.get(snapshotMetadata.persistenceId());
        if (latch != null) {
            latch.countDown();
        }

        return Futures.successful(null);
    }

    @Override
    public Future<Void> doDeleteAsync(final SnapshotMetadata metadata) {
        List<StoredSnapshot> snapshotList = snapshots.get(metadata.persistenceId());

        if (snapshotList != null) {
            synchronized (snapshotList) {
                for (int i = 0; i < snapshotList.size(); i++) {
                    StoredSnapshot snapshot = snapshotList.get(i);
                    if (metadata.equals(snapshot.metadata)) {
                        snapshotList.remove(i);
                        break;
                    }
                }
            }
        }

        return Futures.successful(null);
    }

    @Override
    public Future<Void> doDeleteAsync(final String persistenceId, final SnapshotSelectionCriteria criteria) {
        LOG.trace("doDelete: persistentId {}: maxSequenceNr: {}: maxTimestamp {}", persistenceId,
            criteria.maxSequenceNr(), criteria.maxTimestamp());

        List<StoredSnapshot> snapshotList = snapshots.get(persistenceId);
        if (snapshotList != null) {
            synchronized (snapshotList) {
                Iterator<StoredSnapshot> iter = snapshotList.iterator();
                while (iter.hasNext()) {
                    StoredSnapshot stored = iter.next();
                    if (matches(stored, criteria)) {
                        LOG.trace("Deleting snapshot for sequenceNr: {}, timestamp: {}: {}",
                                stored.metadata.sequenceNr(), stored.metadata.timestamp(), stored.data);

                        iter.remove();
                    }
                }
            }
        }

        CountDownLatch latch = SNAPSHOT_DELETED_LATCHES.get(persistenceId);
        if (latch != null) {
            latch.countDown();
        }

        return Futures.successful(null);
    }

    private static final class StoredSnapshot {
        private final SnapshotMetadata metadata;
        private final Object data;

        StoredSnapshot(final SnapshotMetadata metadata, final Object data) {
            this.metadata = metadata;
            this.data = data;
        }
    }
}
