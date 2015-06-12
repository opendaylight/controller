/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.modification.ModificationPayload;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.node.utils.transformer.NormalizedNodePruner;
import org.opendaylight.controller.cluster.datastore.utils.PruningDataTreeModification;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.controller.cluster.raft.RaftActorRecoveryCohort;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationByteStringPayload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationPayload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
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
    private static final YangInstanceIdentifier ROOT = YangInstanceIdentifier.builder().build();
    private final DataTree store;
    private final String shardName;
    private final Logger log;
    private final Set<URI> validNamespaces;
    private PruningDataTreeModification transaction;
    private int size;

    ShardRecoveryCoordinator(ShardDataTree store, SchemaContext schemaContext, String shardName, Logger log) {
        this.store = store.getDataTree();
        this.shardName = shardName;
        this.log = log;
        this.validNamespaces = NormalizedNodePruner.namespaces(schemaContext);
    }

    @Override
    public void startLogRecoveryBatch(int maxBatchSize) {
        log.debug("{}: starting log recovery batch with max size {}", shardName, maxBatchSize);
        transaction = new PruningDataTreeModification(store.takeSnapshot().newModification(), validNamespaces);
        size = 0;
    }

    @Override
    public void appendRecoveredLogEntry(Payload payload) {
        Preconditions.checkState(transaction != null, "call startLogRecovery before calling appendRecoveredLogEntry");

        try {
            if (payload instanceof DataTreeCandidatePayload) {
                DataTreeCandidates.applyToModification(transaction, ((DataTreeCandidatePayload)payload).getCandidate());
                size++;
            } else if (payload instanceof ModificationPayload) {
                MutableCompositeModification.fromSerializable(
                    ((ModificationPayload) payload).getModification()).apply(transaction);
                size++;
            } else if (payload instanceof CompositeModificationPayload) {
                MutableCompositeModification.fromSerializable(
                    ((CompositeModificationPayload) payload).getModification()).apply(transaction);
                size++;
            } else if (payload instanceof CompositeModificationByteStringPayload) {
                MutableCompositeModification.fromSerializable(
                        ((CompositeModificationByteStringPayload) payload).getModification()).apply(transaction);
                size++;
            } else {
                log.error("{}: Unknown payload {} received during recovery", shardName, payload);
            }
        } catch (IOException | ClassNotFoundException e) {
            log.error("{}: Error extracting ModificationPayload", shardName, e);
        }
    }

    private void commitTransaction(PruningDataTreeModification tx) throws DataValidationFailedException {
        DataTreeModification delegate = tx.getDelegate();
        delegate.ready();
        store.validate(delegate);
        store.commit(store.prepare(delegate));
    }

    /**
     * Applies the current batched log entries to the data store.
     */
    @Override
    public void applyCurrentLogRecoveryBatch() {
        Preconditions.checkState(transaction != null, "call startLogRecovery before calling applyCurrentLogRecoveryBatch");

        log.debug("{}: Applying current log recovery batch with size {}", shardName, size);
        try {
            commitTransaction(transaction);
        } catch (DataValidationFailedException e) {
            log.error("{}: Failed to apply recovery batch", shardName, e);
        }
        transaction = null;
    }

    /**
     * Applies a recovered snapshot to the data store.
     *
     * @param snapshotBytes the serialized snapshot
     */
    @Override
    public void applyRecoverySnapshot(final byte[] snapshotBytes) {
        log.debug("{}: Applying recovered snapshot", shardName);

        final NormalizedNode<?, ?> node = SerializationUtils.deserializeNormalizedNode(snapshotBytes);
        final PruningDataTreeModification tx = new PruningDataTreeModification(store.takeSnapshot().newModification(), validNamespaces);
        tx.write(ROOT, node);
        try {
            commitTransaction(tx);
        } catch (DataValidationFailedException e) {
            log.error("{}: Failed to apply recovery snapshot", shardName, e);
        }
    }
}
