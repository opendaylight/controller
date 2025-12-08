/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;

// Non-sealed for mocking
abstract class TransactionParent {
    final @NonNull ShardDataTree dataTree;

    @NonNullByDefault
    TransactionParent(final ShardDataTree dataTree) {
        this.dataTree = requireNonNull(dataTree);
    }

    @NonNullByDefault
    abstract ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(TransactionIdentifier txId);

    @NonNullByDefault
    abstract ReadWriteShardDataTreeTransaction newReadWriteTransaction(TransactionIdentifier txId);

    void abortTransaction(final AbstractShardDataTreeTransaction<?> transaction, final Runnable callback) {
        dataTree.abortTransaction(transaction.getIdentifier(), callback);
    }

    @NonNullByDefault
    abstract CommitCohort finishTransaction(ReadWriteShardDataTreeTransaction transaction);

    @NonNullByDefault
    abstract CommitCohort createReadyCohort(TransactionIdentifier txId, DataTreeModification mod);

    @NonNullByDefault
    final CommitCohort createFailedCohort(final TransactionIdentifier txId, final DataTreeModification mod,
            final Exception failure) {
        final var transaction = new ReadWriteShardDataTreeTransaction(this, txId, mod);
        transaction.close();

        final var cohort = new CommitCohort(transaction, failure);
        dataTree.enqueueReadyTransaction(cohort);
        return cohort;
    }

    abstract @NonNull FutureCallback<UnsignedLong> wrapCommitCallback(
        @NonNull ReadWriteShardDataTreeTransaction transaction, @NonNull FutureCallback<UnsignedLong> callback);
}
