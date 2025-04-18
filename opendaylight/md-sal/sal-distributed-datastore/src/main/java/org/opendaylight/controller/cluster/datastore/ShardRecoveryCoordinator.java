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
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.datastore.utils.NormalizedNodeXMLOutput;
import org.opendaylight.controller.cluster.raft.RaftActorRecoveryCohort;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.StateCommand;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        Simple(final ShardDataTree store, final String memberId) {
            super(store, memberId);
        }

        @Override
        public Snapshot getRestoreFromSnapshot() {
            return null;
        }
    }

    private static final class WithSnapshot extends ShardRecoveryCoordinator {
        private final Snapshot restoreFromSnapshot;

        WithSnapshot(final ShardDataTree store, final String memberId, final Snapshot snapshot) {
            super(store, memberId);
            restoreFromSnapshot = requireNonNull(snapshot);
        }

        @Override
        public Snapshot getRestoreFromSnapshot() {
            return restoreFromSnapshot;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ShardRecoveryCoordinator.class);

    private final ShardDataTree store;
    private final String memberId;

    private boolean open;

    ShardRecoveryCoordinator(final ShardDataTree store, final String memberId) {
        this.store = requireNonNull(store);
        this.memberId = requireNonNull(memberId);
    }

    static ShardRecoveryCoordinator create(final ShardDataTree store, final String memberId) {
        return new Simple(store, memberId);
    }

    static ShardRecoveryCoordinator forSnapshot(final ShardDataTree store, final String memberId,
            final Snapshot snapshot) {
        return new WithSnapshot(store, memberId, snapshot);
    }

    @Override
    public void startLogRecoveryBatch(final int maxBatchSize) {
        LOG.debug("{}: starting log recovery batch with max size {}", memberId, maxBatchSize);
        open = true;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void appendRecoveredCommand(final StateCommand command) {
        checkState(open, "call startLogRecovery before calling appendRecoveredLogEntry");

        try {
            store.applyRecoveryCommand(command);
        } catch (Exception e) {
            LOG.error("{}: failed to apply payload {}", memberId, command, e);
            throw new IllegalStateException("%s: Failed to apply recovery payload %s".formatted(memberId, command), e);
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

    private File writeRoot(final String kind, final NormalizedNode node) {
        final var file = new File(System.getProperty("karaf.data", "."),
            "failed-recovery-" + kind + "-" + memberId + ".xml");
        NormalizedNodeXMLOutput.toFile(file, node);
        return file;
    }

    /**
     * Applies a recovered snapshot to the data store.
     *
     * @param snapshot the serialized snapshot
     */
    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void applyRecoveredSnapshot(final StateSnapshot snapshot) {
        if (!(snapshot instanceof ShardSnapshotState shardSnapshotState)) {
            LOG.debug("{}: applyRecoverySnapshot ignoring snapshot: {}", memberId, snapshot);
            return;
        }

        LOG.debug("{}: Applying recovered snapshot", memberId);
        try {
            store.applyRecoverySnapshot(shardSnapshotState);
        } catch (Exception e) {
            final var shardSnapshot = shardSnapshotState.getSnapshot();
            final var file = writeRoot("snapshot", shardSnapshot.getRootNode().orElse(null));
            throw new IllegalStateException(
                "%s: Failed to apply recovery snapshot %s. Node data was written to file %s".formatted(
                    memberId, shardSnapshot, file), e);
        }
    }
}
