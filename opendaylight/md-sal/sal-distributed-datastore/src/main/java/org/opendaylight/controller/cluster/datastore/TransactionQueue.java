/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import java.util.SortedSet;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.AbortTransactionPayload;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Non-sealed for mocking
abstract class TransactionQueue {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionQueue.class);

    final @NonNull ShardDataTree dataTree;

    @NonNullByDefault
    TransactionQueue(final ShardDataTree dataTree) {
        this.dataTree = requireNonNull(dataTree);
    }

    @NonNullByDefault
    abstract ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(TransactionIdentifier txId);

    @NonNullByDefault
    abstract ReadWriteShardDataTreeTransaction newReadWriteTransaction(TransactionIdentifier txId);

    void abortTransaction(final AbstractShardDataTreeTransaction<?> transaction, final Runnable callback) {
        final var txId = transaction.getIdentifier();
        LOG.debug("{}: aborting transaction {}", dataTree.logContext(), txId);
        dataTree.replicatePayload(txId, AbortTransactionPayload.create(txId, dataTree.initialPayloadBufferSize()),
            callback);
    }

    abstract CommitCohort finishTransaction(ReadWriteShardDataTreeTransaction transaction,
        @Nullable SortedSet<String> participatingShardNames);

    abstract CommitCohort createReadyCohort(TransactionIdentifier txId, DataTreeModification mod,
        @Nullable SortedSet<String> participatingShardNames);

    abstract CommitCohort createFailedCohort(TransactionIdentifier txId, DataTreeModification mod,
            Exception failure);
}
