/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TransactionParent} of inter-dependent transactions. Transactions are submitted in-order and a transaction
 * failure causes all subsequent transactions to fail as well.
 */
final class ChainedTransactionParent extends TransactionParent implements Identifiable<LocalHistoryIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(ChainedTransactionParent.class);

    private ReadWriteShardDataTreeTransaction previousTx;
    private ReadWriteShardDataTreeTransaction openTransaction;
    private boolean closed;

    @NonNullByDefault
    ChainedTransactionParent(final ShardDataTree dataTree, final LocalHistoryIdentifier historyId) {
        super(dataTree, historyId);
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return historyId;
    }

    @Override
    ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(final TransactionIdentifier txId) {
        final var snapshot = getSnapshot();
        LOG.debug("Allocated read-only transaction {} snapshot {}", txId, snapshot);

        return new ReadOnlyShardDataTreeTransaction(this, txId, snapshot);
    }

    @Override
    ReadWriteShardDataTreeTransaction newReadWriteTransaction(final TransactionIdentifier txId) {
        final var snapshot = getSnapshot();
        LOG.debug("Allocated read-write transaction {} snapshot {}", txId, snapshot);

        final var ret = new ReadWriteShardDataTreeTransaction(this, txId, snapshot.newModification());
        openTransaction = ret;
        return ret;
    }

    void close() {
        closed = true;
        LOG.debug("Closing chain {}", historyId);
    }

    @Override
    void abortTransaction(final AbstractShardDataTreeTransaction<?> transaction, final Runnable callback) {
        if (transaction instanceof ReadWriteShardDataTreeTransaction) {
            checkState(openTransaction != null, "Attempted to abort transaction %s while none is outstanding",
                    transaction);
            LOG.debug("Aborted open transaction {}", transaction);
            openTransaction = null;
        }
        super.abortTransaction(transaction, callback);
    }

    @Override
    CommitCohort finishTransaction(final ReadWriteShardDataTreeTransaction transaction) {
        checkState(openTransaction != null, "Attempted to finish transaction %s while none is outstanding",
                transaction);

        // dataTree is finalizing ready the transaction, we just record it for the next transaction in chain
        final var cohort = dataTree.finishTransaction(transaction);
        recordTransaction(transaction);
        return cohort;
    }

    @Override
    CommitCohort createReadyCohort(final TransactionIdentifier txId, final DataTreeModification mod) {
        checkState(openTransaction == null, "Attempted to finish transaction %s while %s is outstanding", txId,
            openTransaction);

        final var transaction = new ReadWriteShardDataTreeTransaction(this, txId, mod);
        transaction.close();

        final var cohort = dataTree.enqueueReadyTransaction(transaction);
        recordTransaction(transaction);
        return cohort;
    }

    @NonNullByDefault
    private void recordTransaction(final ReadWriteShardDataTreeTransaction transaction) {
        previousTx = transaction;
        LOG.debug("Committing transaction {}", transaction);
    }

    private DataTreeSnapshot getSnapshot() {
        checkState(!closed, "TransactionChain %s has been closed", this);
        checkState(openTransaction == null, "Transaction %s is open", openTransaction);

        if (previousTx == null) {
            LOG.debug("Opening an unchained snapshot in {}", historyId);
            return dataTree.takeSnapshot();
        }

        LOG.debug("Reusing a chained snapshot in {}", historyId);
        return previousTx.getSnapshot();
    }

    @Override
    FutureCallback<UnsignedLong> wrapCommitCallback(final ReadWriteShardDataTreeTransaction transaction,
            final FutureCallback<UnsignedLong> callback) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(final UnsignedLong result) {
                if (transaction.equals(previousTx)) {
                    previousTx = null;
                }
                LOG.debug("Committed transaction {}", transaction);
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(final Throwable failure) {
                LOG.error("Transaction {} commit failed, cannot recover", transaction, failure);
                callback.onFailure(failure);
            }
        };
    }
}
