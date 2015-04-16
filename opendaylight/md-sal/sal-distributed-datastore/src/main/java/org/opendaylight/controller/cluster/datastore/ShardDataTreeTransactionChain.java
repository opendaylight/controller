/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.ForwardingDOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

/**
 * A transaction chain attached to a Shard.
 */
@NotThreadSafe
final class ShardDataTreeTransactionChain extends ShardDataTreeTransactionParent {
    private final ShardDataTree dataTree;
    private DataTreeSnapshot previousTx;
    private String openTransaction;

    ShardDataTreeTransactionChain(final ShardDataTree dataTree) {
        this.dataTree = Preconditions.checkNotNull(dataTree);
    }

    private DataTreeSnapshot openTransaction(final String txId) {
        Preconditions.checkState(openTransaction == null, "Transaction %s is open", openTransaction);
        openTransaction = txId;
        
        if (previousTx == null) {
            return dataTree.getDataTree().takeSnapshot();
        } else {
            return previousTx;
        }
    }

    ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(final String txId) {
        final DataTreeSnapshot snapshot = openTransaction(txId);
        return new ReadOnlyShardDataTreeTransaction(snapshot);
    }

    ReadWriteShardDataTreeTransaction newReadWriteTransaction(final String txId) {
        final DataTreeSnapshot snapshot = openTransaction(txId);
        return new ReadWriteShardDataTreeTransaction(this, snapshot.newModification());
    }

    ReadWriteShardDataTreeTransaction newWriteOnlyTransaction(final String txId) {
        return newReadWriteTransaction(txId);
    }

    void close() {
        // TODO Auto-generated method stub
    }

    @Override
    protected void abortTransaction(final DataTreeModification snapshot) {
        Preconditions.checkState(openTransaction != null, "Attempted to abort transaction %s while none is outstanding", snapshot);

        // TODO Auto-generated method stub
    }

    @Override
    protected DOMStoreThreePhaseCommitCohort finishTransaction(final DataTreeModification snapshot) {
        Preconditions.checkState(openTransaction != null, "Attempted to finish transaction %s while none is outstanding", snapshot);

        // dataTree is finalizing ready the transaction, we just record it for the next
        // transaction in chain
        final DOMStoreThreePhaseCommitCohort delegate = dataTree.finishTransaction(snapshot);
        previousTx = snapshot;
        return new CommitCohort(delegate);
    }

    private final class CommitCohort extends ForwardingDOMStoreThreePhaseCommitCohort {
        private final DOMStoreThreePhaseCommitCohort delegate;

        CommitCohort(final DOMStoreThreePhaseCommitCohort delegate) {
            this.delegate = Preconditions.checkNotNull(delegate);
        }

        @Override
        protected DOMStoreThreePhaseCommitCohort delegate() {
            return delegate;
        }

        @Override
        public ListenableFuture<Void> preCommit() {
            final ListenableFuture<Void> ret = super.preCommit();
            
            Futures.addCallback(ret, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // FIXME: hook onto the cohort to change internal state here
                }

                @Override
                public void onFailure(Throwable t) {
                    // FIXME: what should we do?
                }
            });

            return ret;
        }

        @Override
        public ListenableFuture<Void> abort() {
            final ListenableFuture<Void> ret = super.abort();

            // FIXME: roll back recorded next snapshot
            return ret;
        }

        @Override
        public ListenableFuture<Void> commit() {
            final ListenableFuture<Void> ret = super.commit();

            // FIXME: roll forward the snapshot
            return ret;
        }
    }
}
