/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;

/**
 * A {@link TransactionParent}. Transactions are independent and can be submitted in any order.
 */
final class SimpleTransactionParent extends TransactionParent {
    @NonNullByDefault
    SimpleTransactionParent(final ShardDataTree dataTree) {
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
    CommitCohort finishTransaction(final ReadWriteShardDataTreeTransaction transaction) {
        final var userCohorts = dataTree.finishTransaction(transaction);
        final var cohort = new CommitCohort(transaction, userCohorts);
        dataTree.enqueueReadyTransaction(cohort);
        return cohort;
    }

    @Override
    CommitCohort createReadyCohort(final TransactionIdentifier txId, final DataTreeModification mod) {
        final var transaction = new ReadWriteShardDataTreeTransaction(this, txId, mod);
        transaction.close();

        final var cohort = new CommitCohort(transaction, dataTree.newUserCohorts(txId));
        dataTree.enqueueReadyTransaction(cohort);
        return cohort;
    }

    @Override
    FutureCallback<UnsignedLong> wrapCommitCallback(final ReadWriteShardDataTreeTransaction transaction,
            final FutureCallback<UnsignedLong> callback) {
        return callback;
    }
}
