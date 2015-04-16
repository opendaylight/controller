/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

final class ShardDataTreeTransactionChain extends ShardDatTreeTransactionParent {
    private DataTreeSnapshot snapshot;
    private String openTransaction;

    ShardDataTreeTransactionChain(DataTreeSnapshot snapshot) {
        this.snapshot = Preconditions.checkNotNull(snapshot);
    }
    
    private void openTransaction(String txId) {
        Preconditions.checkState(openTransaction == null, "Transaction %s is open", openTransaction);
        openTransaction = txId;
    }

    ReadOnlyShardDataTreeTransaction newReadOnlyTransaction(String txId) {
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
        // TODO Auto-generated method stub
        return null;
    }
}
