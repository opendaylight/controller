/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A transaction chain attached to a Shard.
 */
@NotThreadSafe
final class ShardDataTreeTransactionChain extends ShardDataTreeTransactionParent
        implements Identifiable<LocalHistoryIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTreeTransactionChain.class);
    private final LocalHistoryIdentifier chainId;
    private final ShardDataTree dataTree;

    private ReadWriteShardDataTreeTransaction previousTx;
    private ReadWriteShardDataTreeTransaction openTransaction;
    private boolean closed;

    ShardDataTreeTransactionChain(final LocalHistoryIdentifier localHistoryIdentifier, final ShardDataTree dataTree) {
        this.chainId = Preconditions.checkNotNull(localHistoryIdentifier);
        this.dataTree = Preconditions.checkNotNull(dataTree);
    }

    private DataTreeSnapshot getSnapshot() {
        Preconditions.checkState(!closed, "TransactionChain %s has been closed", this);
        Preconditions.checkState(openTransaction == null, "Transaction %s is open", openTransaction);

        if (previousTx == null) {
            return dataTree.takeSnapshot();
        } else {
            return previousTx.getSnapshot();
        }
    }

    ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(final TransactionIdentifier txId) {
        final DataTreeSnapshot snapshot = getSnapshot();
        LOG.debug("Allocated read-only transaction {} snapshot {}", txId, snapshot);

        return new ReadOnlyShardDataTreeTransaction(txId, snapshot);
    }

    ReadWriteShardDataTreeTransaction newReadWriteTransaction(final TransactionIdentifier txId) {
        final DataTreeSnapshot snapshot = getSnapshot();
        LOG.debug("Allocated read-write transaction {} snapshot {}", txId, snapshot);

        openTransaction = new ReadWriteShardDataTreeTransaction(this, txId, snapshot.newModification());
        return openTransaction;
    }

    void close() {
        closed = true;
    }

    @Override
    protected void abortTransaction(final AbstractShardDataTreeTransaction<?> transaction) {
        if (transaction instanceof ReadWriteShardDataTreeTransaction) {
            Preconditions.checkState(openTransaction != null, "Attempted to abort transaction %s while none is outstanding", transaction);
            LOG.debug("Aborted transaction {}", transaction);
            openTransaction = null;
        }
    }

    @Override
    protected ShardDataTreeCohort finishTransaction(final ReadWriteShardDataTreeTransaction transaction) {
        Preconditions.checkState(openTransaction != null, "Attempted to finish transaction %s while none is outstanding", transaction);

        // dataTree is finalizing ready the transaction, we just record it for the next
        // transaction in chain
        final ShardDataTreeCohort delegate = dataTree.finishTransaction(transaction);
        openTransaction = null;
        previousTx = transaction;
        LOG.debug("Committing transaction {}", transaction);

        return new ChainedCommitCohort(this, transaction, delegate);
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

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return chainId;
    }

    @Override
    ShardDataTreeCohort createReadyCohort(final TransactionIdentifier txId, final DataTreeModification modification) {
        return dataTree.createReadyCohort(txId, modification);
    }
}
