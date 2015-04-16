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
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

final class ShardDataTreeTransactionChain extends ShardDatTreeTransactionParent {
    private final ShardDataTree dataTree;
    private DataTreeSnapshot snapshot;
    private String openTransaction;

    ShardDataTreeTransactionChain(final ShardDataTree dataTree, final DataTreeSnapshot snapshot) {
        this.dataTree = Preconditions.checkNotNull(dataTree);
        this.snapshot = Preconditions.checkNotNull(snapshot);
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
        final DOMStoreThreePhaseCommitCohort delegate = dataTree.finishTransaction(snapshot);
        
        return new DOMStoreThreePhaseCommitCohort() {
            @Override
            public ListenableFuture<Boolean> canCommit() {
                return delegate.canCommit();
            }

            @Override
            public ListenableFuture<Void> preCommit() {
                final ListenableFuture<Void> ret = delegate.preCommit();
                
                // FIXME: hook onto the cohort to change internal state here
                return ret;
            }

            @Override
            public ListenableFuture<Void> abort() {
                delegate.abort();

                // FIXME: roll back recorded next snapshot
                return null;
            }

            @Override
            public ListenableFuture<Void> commit() {
                final ListenableFuture<Void> ret = delegate.commit();

                // FIXME: roll forward the snapshot
                return ret;
            }            
        };
    }
}
