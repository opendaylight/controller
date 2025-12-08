/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import org.eclipse.jdt.annotation.NonNull;
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

    private final @NonNull LocalHistoryIdentifier chainId;

    private ReadWriteShardDataTreeTransaction previousTx;
    private ReadWriteShardDataTreeTransaction openTransaction;
    private boolean closed;

    @NonNullByDefault
    ChainedTransactionParent(final LocalHistoryIdentifier localHistoryIdentifier, final ShardDataTree dataTree) {
        super(dataTree);
        chainId = requireNonNull(localHistoryIdentifier);
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return chainId;
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
        LOG.debug("Closing chain {}", chainId);
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
        final var userCohorts = dataTree.finishTransaction(transaction);
        openTransaction = null;
        return createReadyCohort(transaction, userCohorts);
    }

    @Override
    CommitCohort createReadyCohort(final TransactionIdentifier txId, final DataTreeModification mod) {
        checkState(openTransaction == null, "Attempted to finish transaction %s while %s is outstanding", txId,
            openTransaction);

        final var transaction = new ReadWriteShardDataTreeTransaction(this, txId, mod);
        transaction.close();
        return createReadyCohort(transaction, dataTree.newUserCohorts(txId));
    }

    @NonNullByDefault
    private CommitCohort createReadyCohort(final ReadWriteShardDataTreeTransaction transaction,
            final UserCohorts userCohorts) {
        previousTx = transaction;
        LOG.debug("Committing transaction {}", transaction);
        final var cohort = new CommitCohort(transaction, userCohorts);
        dataTree.enqueueReadyTransaction(cohort);
        return cohort;
    }

    private DataTreeSnapshot getSnapshot() {
        checkState(!closed, "TransactionChain %s has been closed", this);
        checkState(openTransaction == null, "Transaction %s is open", openTransaction);

        if (previousTx == null) {
            LOG.debug("Opening an unchained snapshot in {}", chainId);
            return dataTree.takeSnapshot();
        }

        LOG.debug("Reusing a chained snapshot in {}", chainId);
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("id", chainId).toString();
    }
}
