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
import java.util.Optional;
import java.util.SortedSet;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A transaction chain attached to a Shard. This class is NOT thread-safe.
 */
final class ShardDataTreeTransactionChain extends ShardDataTreeTransactionParent
        implements Identifiable<LocalHistoryIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTreeTransactionChain.class);

    private final @NonNull LocalHistoryIdentifier chainId;
    private final @NonNull ShardDataTree dataTree;

    private ReadWriteShardDataTreeTransaction previousTx;
    private ReadWriteShardDataTreeTransaction openTransaction;
    private boolean closed;

    ShardDataTreeTransactionChain(final LocalHistoryIdentifier localHistoryIdentifier, final ShardDataTree dataTree) {
        chainId = requireNonNull(localHistoryIdentifier);
        this.dataTree = requireNonNull(dataTree);
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return chainId;
    }

    @NonNull ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(final TransactionIdentifier txId) {
        final var snapshot = getSnapshot();
        LOG.debug("Allocated read-only transaction {} snapshot {}", txId, snapshot);

        return new ReadOnlyShardDataTreeTransaction(this, txId, snapshot);
    }

    @NonNull ReadWriteShardDataTreeTransaction newReadWriteTransaction(final TransactionIdentifier txId) {
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
    void abortFromTransactionActor(final AbstractShardDataTreeTransaction<?> transaction) {
        if (transaction instanceof ReadWriteShardDataTreeTransaction) {
            checkState(openTransaction != null, "Attempted to abort transaction %s while none is outstanding",
                    transaction);
            LOG.debug("Aborted open transaction {}", transaction);
            openTransaction = null;
        }
    }

    @Override
    void abortTransaction(final AbstractShardDataTreeTransaction<?> transaction, final Runnable callback) {
        abortFromTransactionActor(transaction);
        dataTree.abortTransaction(transaction, callback);
    }

    @Override
    ChainedCommitCohort finishTransaction(final ReadWriteShardDataTreeTransaction transaction,
            final Optional<SortedSet<String>> participatingShardNames) {
        checkState(openTransaction != null, "Attempted to finish transaction %s while none is outstanding",
                transaction);

        // dataTree is finalizing ready the transaction, we just record it for the next transaction in chain
        final var userCohorts = dataTree.finishTransaction(transaction);
        openTransaction = null;
        return createReadyCohort(transaction, userCohorts, participatingShardNames);
    }

    // TODO: this is not nice: we should handle this in ChainedCommitCohort -- we just do not have an underlying
    //       ReadWriteShardDataTreeTransaction
    @Override
    CommitCohort createFailedCohort(final TransactionIdentifier txId, final DataTreeModification mod,
            final Exception failure) {
        return dataTree.createFailedCohort(txId, mod, failure);
    }

    @Override
    ChainedCommitCohort createReadyCohort(final TransactionIdentifier txId, final DataTreeModification mod,
            final Optional<SortedSet<String>> participatingShardNames) {
        checkState(openTransaction == null, "Attempted to finish transaction %s while %s is outstanding", txId,
            openTransaction);

        final var transaction = new ReadWriteShardDataTreeTransaction(dataTree, txId, mod);
        transaction.close();
        return createReadyCohort(transaction, dataTree.newUserCohorts(txId), participatingShardNames);
    }

    private ChainedCommitCohort createReadyCohort(final ReadWriteShardDataTreeTransaction transaction,
            final CompositeDataTreeCohort userCohorts, final Optional<SortedSet<String>> participatingShardNames) {
        previousTx = transaction;
        LOG.debug("Committing transaction {}", transaction);
        final var cohort = new ChainedCommitCohort(dataTree, this, transaction, userCohorts, participatingShardNames);
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
    public String toString() {
        return MoreObjects.toStringHelper(this).add("id", chainId).toString();
    }

    void clearTransaction(final ReadWriteShardDataTreeTransaction transaction) {
        if (transaction.equals(previousTx)) {
            previousTx = null;
        }
    }
}
