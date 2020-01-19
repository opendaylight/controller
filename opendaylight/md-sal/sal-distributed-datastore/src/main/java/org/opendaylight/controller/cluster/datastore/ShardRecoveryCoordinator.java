/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.io.File;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.datastore.utils.NormalizedNodeXMLOutput;
import org.opendaylight.controller.cluster.raft.RaftActorRecoveryCohort;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;

/**
 * Coordinates persistence recovery of journal log entries and snapshots for a shard. Each snapshot
 * and journal log entry batch are de-serialized and applied to their own write transaction
 * instance in parallel on a thread pool for faster recovery time. However the transactions are
 * committed to the data store in the order the corresponding snapshot or log batch are received
 * to preserve data store integrity.
 *
 * @author Thomas Pantelis
 */
abstract class ShardRecoveryCoordinator implements RaftActorRecoveryCohort {
    private static final class Simple extends ShardRecoveryCoordinator {
        Simple(final ShardDataTree store, final String shardName, final Logger log) {
            super(store, shardName, log);
        }

        @Override
        public Snapshot getRestoreFromSnapshot() {
            return null;
        }
    }

    private static final class WithSnapshot extends ShardRecoveryCoordinator {
        private final Snapshot restoreFromSnapshot;

        WithSnapshot(final ShardDataTree store, final String shardName, final Logger log, final Snapshot snapshot) {
            super(store, shardName, log);
            this.restoreFromSnapshot = requireNonNull(snapshot);
        }

        @Override
        public Snapshot getRestoreFromSnapshot() {
            return restoreFromSnapshot;
        }
    }

    private final ShardDataTree store;
    private final String shardName;
    private final Logger log;

    private boolean open;

    ShardRecoveryCoordinator(final ShardDataTree store, final String shardName, final Logger log) {
        this.store = requireNonNull(store);
        this.shardName = requireNonNull(shardName);
        this.log = requireNonNull(log);
    }

    static ShardRecoveryCoordinator create(final ShardDataTree store, final String shardName, final Logger log) {
        return new Simple(store, shardName, log);
    }

    static ShardRecoveryCoordinator forSnapshot(final ShardDataTree store, final String shardName, final Logger log,
            final Snapshot snapshot) {
        return new WithSnapshot(store, shardName, log, snapshot);
    }

    @Override
    public void startLogRecoveryBatch(final int maxBatchSize) {
        log.debug("{}: starting log recovery batch with max size {}", shardName, maxBatchSize);
        open = true;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void appendRecoveredLogEntry(final Payload payload) {
        checkState(open, "call startLogRecovery before calling appendRecoveredLogEntry");

        try {
            store.applyRecoveryPayload(payload);
        } catch (Exception e) {
            log.error("{}: failed to apply payload {}", shardName, payload, e);
            throw new IllegalStateException(String.format("%s: Failed to apply recovery payload %s",
                shardName, payload), e);
        }
    }

    /**
     * Applies the current batched log entries to the data store.
     */
    @Override
    public void applyCurrentLogRecoveryBatch() {
        checkState(open, "call startLogRecovery before calling applyCurrentLogRecoveryBatch");
        open = false;
    }

    private File writeRoot(final String kind, final NormalizedNode<?, ?> node) {
        final File file = new File(System.getProperty("karaf.data", "."),
            "failed-recovery-" + kind + "-" + shardName + ".xml");
        NormalizedNodeXMLOutput.toFile(file, node);
        return file;
    }

    /**
     * Applies a recovered snapshot to the data store.
     *
     * @param snapshotState the serialized snapshot
     */
    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void applyRecoverySnapshot(final Snapshot.State snapshotState) {
        if (!(snapshotState instanceof ShardSnapshotState)) {
            log.debug("{}: applyRecoverySnapshot ignoring snapshot: {}", shardName, snapshotState);
            return;
        }

        log.debug("{}: Applying recovered snapshot", shardName);
        final ShardSnapshotState shardSnapshotState = (ShardSnapshotState)snapshotState;
        try {
            store.applyRecoverySnapshot(shardSnapshotState);
        } catch (Exception e) {
            final ShardDataTreeSnapshot shardSnapshot = shardSnapshotState.getSnapshot();
            final File f = writeRoot("snapshot", shardSnapshot.getRootNode().orElse(null));
            throw new IllegalStateException(String.format(
                    "%s: Failed to apply recovery snapshot %s. Node data was written to file %s",
                    shardName, shardSnapshot, f), e);
        }
    }
}
