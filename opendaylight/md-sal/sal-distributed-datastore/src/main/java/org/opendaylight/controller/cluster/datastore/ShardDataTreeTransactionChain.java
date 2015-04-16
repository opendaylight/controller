/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
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
final class ShardDataTreeTransactionChain extends ShardDatTreeTransactionParent {
    private final ShardDataTree dataTree;
    private DataTreeSnapshot snapshot;
    private String openTransaction;

    ShardDataTreeTransactionChain(final ShardDataTree dataTree) {
        this.dataTree = Preconditions.checkNotNull(dataTree);
    }
    
    private void openTransaction(String txId) {
        Preconditions.checkState(openTransaction == null, "Transaction %s is open", openTransaction);
        openTransaction = txId;
    }

    ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(final String txId) {
        openTransaction(txId);
        
        
        return new ReadOnlyShardDataTreeTransaction(snapshot);
    }

    ReadWriteShardDataTreeTransaction newReadWriteTransaction(String txId) {
        openTransaction(txId);
        
        
        return new ReadWriteShardDataTreeTransaction(this, snapshot.newModification());
    }

    ReadWriteShardDataTreeTransaction newWriteOnlyTransaction(String txId) {
        return newReadWriteTransaction(txId);
    }

    void close() {
        // TODO Auto-generated method stub
    }

    @Override
    protected void abortTransaction(DataTreeModification snapshot) {
        // TODO Auto-generated method stub        
    }

    @Override
    protected DOMStoreThreePhaseCommitCohort finishTransaction(DataTreeModification snapshot) {
        return new CommitCohort(dataTree.finishTransaction(snapshot));
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
            
            // FIXME: hook onto the cohort to change internal state here
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
