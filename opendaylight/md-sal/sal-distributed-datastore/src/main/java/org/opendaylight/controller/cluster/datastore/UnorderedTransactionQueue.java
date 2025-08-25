/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.SortedSet;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.AbortTransactionPayload;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TransactionQueue}. Transactions are independent and can be submitted in any order.
 */
final class UnorderedTransactionQueue extends TransactionQueue {
    private static final Logger LOG = LoggerFactory.getLogger(UnorderedTransactionQueue.class);

    @NonNullByDefault
    UnorderedTransactionQueue(final ShardDataTree dataTree) {
        super(dataTree);
    }

    @Override
    ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(final TransactionIdentifier txId) {
        return new ReadOnlyShardDataTreeTransaction(this, txId, dataTree.takeSnapshot());
    }

    @Override
    ReadWriteShardDataTreeTransaction newReadWriteTransaction(final TransactionIdentifier txId) {
        return new ReadWriteShardDataTreeTransaction(this, txId, dataTree.newModification());
    }

    @Override
    void abortTransaction(final AbstractShardDataTreeTransaction<?> transaction, final Runnable callback) {
        final TransactionIdentifier id = transaction.getIdentifier();
        LOG.debug("{}: aborting transaction {}", dataTree.logContext(), id);
        replicatePayload(id, AbortTransactionPayload.create(
                id, shard.getDatastoreContext().getInitialPayloadSerializedBufferCapacity()), callback);
    }

    @Override
    SimpleCommitCohort finishTransaction(final ReadWriteShardDataTreeTransaction transaction,
            final SortedSet<String> participatingShardNames) {
        final var userCohorts = dataTree.finishTransaction(transaction);
        final var cohort = new SimpleCommitCohort(dataTree, transaction, userCohorts, participatingShardNames);
        dataTree.enqueueReadyTransaction(cohort);
        return cohort;
    }

    @Override
    SimpleCommitCohort createFailedCohort(final TransactionIdentifier txId, final DataTreeModification mod,
            final Exception failure) {
        final var cohort = new SimpleCommitCohort(dataTree, mod, txId, failure);
        dataTree.enqueueReadyTransaction(cohort);
        return cohort;
    }

    @Override
    SimpleCommitCohort createReadyCohort(final TransactionIdentifier txId, final DataTreeModification mod,
            final SortedSet<String> participatingShardNames) {
        final var cohort = new SimpleCommitCohort(dataTree, mod, txId, dataTree.newUserCohorts(txId),
            participatingShardNames);
        dataTree.enqueueReadyTransaction(cohort);
        return cohort;
    }
}
