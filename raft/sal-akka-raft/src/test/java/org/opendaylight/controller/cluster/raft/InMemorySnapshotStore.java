/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
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
@Deprecated(since = "11.0.0", forRemoval = true)
public class InMemorySnapshotStore extends SnapshotStore {
    private static final Logger LOG = LoggerFactory.getLogger(InMemorySnapshotStore.class);

    private static final Map<String, CountDownLatch> SNAPSHOT_DELETED_LATCHES = new ConcurrentHashMap<>();
    private static Map<String, List<StoredSnapshot>> snapshots = new ConcurrentHashMap<>();

    public static void addSnapshot(final String persistentId, final Object snapshot) {
        final var snapshotList = snapshots.computeIfAbsent(persistentId, k -> new ArrayList<>());

        synchronized (snapshotList) {
            snapshotList.add(new StoredSnapshot(new SnapshotMetadata(persistentId, snapshotList.size(),
                    System.currentTimeMillis()), snapshot));
        }
    }

    public static <T> List<T> getSnapshots(final String persistentId, final Class<T> type) {
        final var stored = snapshots.get(persistentId);
        if (stored == null) {
            return List.of();
        }

        synchronized (stored) {
            final var ret = new ArrayList<T>(stored.size());
            for (var snapshot : stored) {
                if (type.isInstance(snapshot.data)) {
                    ret.add(type.cast(snapshot.data));
                }
            }
            return ret;
        }
    }

    public static void clear() {
        snapshots.clear();
    }

    public static void addSnapshotDeletedLatch(final String persistenceId) {
        SNAPSHOT_DELETED_LATCHES.put(persistenceId, new CountDownLatch(1));
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
        final var snapshotList = snapshots.computeIfAbsent(snapshotMetadata.persistenceId(),
            unused -> new ArrayList<>());
        LOG.trace("doSaveAsync: persistentId {}: sequenceNr: {}: timestamp {}: {}", snapshotMetadata.persistenceId(),
                snapshotMetadata.sequenceNr(), snapshotMetadata.timestamp(), obj);

        synchronized (snapshotList) {
            snapshotList.add(new StoredSnapshot(snapshotMetadata, obj));
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
