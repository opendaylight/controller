/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.io.File;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.utils.NormalizedNodeXMLOutput;
import org.opendaylight.controller.cluster.raft.RaftActorRecoveryCohort;
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
class ShardRecoveryCoordinator implements RaftActorRecoveryCohort {
    private final ShardDataTree store;
    private final String shardName;
    private final Logger log;
    private final byte[] restoreFromSnapshot;

    private boolean open;

    ShardRecoveryCoordinator(final ShardDataTree store,  final byte[] restoreFromSnapshot, final String shardName, final Logger log) {
        this.store = Preconditions.checkNotNull(store);
        this.shardName = Preconditions.checkNotNull(shardName);
        this.log = Preconditions.checkNotNull(log);

        this.restoreFromSnapshot = restoreFromSnapshot;
    }

    @Override
    public void startLogRecoveryBatch(final int maxBatchSize) {
        log.debug("{}: starting log recovery batch with max size {}", shardName, maxBatchSize);
        open = true;
    }

    @Override
    public void appendRecoveredLogEntry(final Payload payload) {
        Preconditions.checkState(open, "call startLogRecovery before calling appendRecoveredLogEntry");

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
        Preconditions.checkState(open, "call startLogRecovery before calling applyCurrentLogRecoveryBatch");
        open = false;
    }

    private File writeRoot(final String kind, final NormalizedNode<?, ?> node) {
        final File file = new File(System.getProperty("karaf.data", "."),
            "failed-" + kind + "-snapshot-" + shardName + ".xml");
        NormalizedNodeXMLOutput.toFile(file, node);
        return file;
    }

    /**
     * Applies a recovered snapshot to the data store.
     *
     * @param snapshotBytes the serialized snapshot
     */
    @Override
    public void applyRecoverySnapshot(final byte[] snapshotBytes) {
        log.debug("{}: Applying recovered snapshot", shardName);

        final ShardDataTreeSnapshot snapshot;
        try {
            snapshot = ShardDataTreeSnapshot.deserialize(snapshotBytes);
        } catch (Exception e) {
            log.error("{}: failed to deserialize snapshot", shardName, e);
            throw Throwables.propagate(e);
        }

        try {
            store.applyRecoverySnapshot(snapshot);
        } catch (Exception e) {
            log.error("{}: failed to apply snapshot {}", shardName, snapshot, e);

            final File f = writeRoot("recovery", snapshot.getRootNode().orElse(null));
            throw new IllegalStateException(String.format(
                    "%s: Failed to apply recovery snapshot. Node data was written to file %s", shardName, f), e);
        }
    }

    @Override
    public byte[] getRestoreFromSnapshot() {
        return restoreFromSnapshot;
    }
}
