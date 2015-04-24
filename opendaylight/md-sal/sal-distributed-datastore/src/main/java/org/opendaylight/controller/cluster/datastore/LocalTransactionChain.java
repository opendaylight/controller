/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.sal.core.spi.data.AbstractSnapshotBackedTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

/**
 * Transaction chain instantiated on top of a locally-available DataTree.
 */
final class LocalTransactionChain extends AbstractSnapshotBackedTransactionChain<TransactionIdentifier> {
    private static final Throwable ABORTED = new Throwable("Transaction aborted");
    private final ShardedTransactionChain parent;
    private final String memberName;
    private final DataTree tree;

    LocalTransactionChain(final ShardedTransactionChain parent, final String memberName, final DataTree tree) {
        this.parent = Preconditions.checkNotNull(parent);
        this.memberName = Preconditions.checkNotNull(memberName);
        this.tree = Preconditions.checkNotNull(tree);
    }

    @Override
    protected TransactionIdentifier nextTransactionIdentifier() {
        // FIXME: needs to include transaction chain ID, too
        return new TransactionIdentifier(memberName, parent.currentCounter());
    }

    @Override
    protected boolean getDebugTransactions() {
        return false;
    }

    @Override
    protected DataTreeSnapshot takeSnapshot() {
        return tree.takeSnapshot();
    }

    @Override
    protected DOMStoreThreePhaseCommitCohort createCohort(final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction, final DataTreeModification modification) {
        return new LocalThreePhaseCommitCohort(parent.getActorContext(), transaction, modification) {
            @Override
            protected void transactionAborted(SnapshotBackedWriteTransaction<TransactionIdentifier> transaction) {
                onTransactionFailed(transaction, ABORTED);
            }

            @Override
            protected void transactionCommitted(SnapshotBackedWriteTransaction<TransactionIdentifier> transaction) {
                onTransactionCommited(transaction);
            }
        };
    }
}
