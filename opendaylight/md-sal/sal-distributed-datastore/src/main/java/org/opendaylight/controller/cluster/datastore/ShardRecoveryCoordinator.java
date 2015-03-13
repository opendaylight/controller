/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.modification.ModificationPayload;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationByteStringPayload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationPayload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;

/**
 * Coordinates persistence recovery of journal log entries and snapshots for a shard. Each snapshot
 * and journal log entry batch are de-serialized and applied to their own write transaction
 * instance in parallel on a thread pool for faster recovery time. However the transactions are
 * committed to the data store in the order the corresponding snapshot or log batch are received
 * to preserve data store integrity.
 *
 * @author Thomas Panetelis
 */
class ShardRecoveryCoordinator {

    private final InMemoryDOMDataStore store;
    private List<ModificationPayload> currentLogRecoveryBatch;
    private final String shardName;
    private final Logger log;

    ShardRecoveryCoordinator(InMemoryDOMDataStore store, String shardName, Logger log) {
        this.store = store;
        this.shardName = shardName;
        this.log = log;
    }

    void startLogRecoveryBatch(int maxBatchSize) {
        currentLogRecoveryBatch = Lists.newArrayListWithCapacity(maxBatchSize);

        log.debug("{}: starting log recovery batch with max size {}", shardName, maxBatchSize);
    }

    void appendRecoveredLogPayload(Payload payload) {
        try {
            if(payload instanceof ModificationPayload) {
                currentLogRecoveryBatch.add((ModificationPayload) payload);
            } else if (payload instanceof CompositeModificationPayload) {
                currentLogRecoveryBatch.add(new ModificationPayload(MutableCompositeModification.fromSerializable(
                        ((CompositeModificationPayload) payload).getModification())));
            } else if (payload instanceof CompositeModificationByteStringPayload) {
                currentLogRecoveryBatch.add(new ModificationPayload(MutableCompositeModification.fromSerializable(
                        ((CompositeModificationByteStringPayload) payload).getModification())));
            } else {
                log.error("{}: Unknown payload {} received during recovery", shardName, payload);
            }
        } catch (IOException e) {
            log.error("{}: Error extracting ModificationPayload", shardName, e);
        }

    }

    private void commitTransaction(DOMStoreWriteTransaction transaction) {
        DOMStoreThreePhaseCommitCohort commitCohort = transaction.ready();
        try {
            commitCohort.preCommit().get();
            commitCohort.commit().get();
        } catch (Exception e) {
            log.error("{}: Failed to commit Tx on recovery", shardName, e);
        }
    }

    /**
     * Applies the current batched log entries to the data store.
     */
    void applyCurrentLogRecoveryBatch() {
        log.debug("{}: Applying current log recovery batch with size {}", shardName, currentLogRecoveryBatch.size());

        DOMStoreWriteTransaction writeTx = store.newWriteOnlyTransaction();
        for(ModificationPayload payload: currentLogRecoveryBatch) {
            try {
                MutableCompositeModification.fromSerializable(payload.getModification()).apply(writeTx);
            } catch (Exception e) {
                log.error("{}: Error extracting ModificationPayload", shardName, e);
            }
        }

        commitTransaction(writeTx);

        currentLogRecoveryBatch = null;
    }

    /**
     * Applies a recovered snapshot to the data store.
     *
     * @param snapshotBytes the serialized snapshot
     */
    void applyRecoveredSnapshot(final byte[] snapshotBytes) {
        log.debug("{}: Applyng recovered sbapshot", shardName);

        DOMStoreWriteTransaction writeTx = store.newWriteOnlyTransaction();

        NormalizedNode<?, ?> node = SerializationUtils.deserializeNormalizedNode(snapshotBytes);

        writeTx.write(YangInstanceIdentifier.builder().build(), node);

        commitTransaction(writeTx);
    }
}
