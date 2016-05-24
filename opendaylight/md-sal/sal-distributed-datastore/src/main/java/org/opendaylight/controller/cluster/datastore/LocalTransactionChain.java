/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.sal.core.spi.data.AbstractSnapshotBackedTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

/**
 * Transaction chain instantiated on top of a locally-available DataTree. It does not instantiate
 * a transaction in the leader and rather chains transactions on top of themselves.
 */
final class LocalTransactionChain extends AbstractSnapshotBackedTransactionChain<TransactionIdentifier>
        implements LocalTransactionFactory {
    private static final Throwable ABORTED = new Throwable("Transaction aborted");
    private final TransactionChainProxy parent;
    private final ActorSelection leader;
    private final DataTree tree;

    LocalTransactionChain(final TransactionChainProxy parent, final ActorSelection leader, final DataTree tree) {
        this.parent = Preconditions.checkNotNull(parent);
        this.leader = Preconditions.checkNotNull(leader);
        this.tree = Preconditions.checkNotNull(tree);
    }

    DataTree getDataTree() {
        return tree;
    }

    @Override
    protected TransactionIdentifier nextTransactionIdentifier() {
        throw new UnsupportedOperationException();
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
    protected DOMStoreThreePhaseCommitCohort createCohort(
            final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction,
            final DataTreeModification modification) {
        return new LocalChainThreePhaseCommitCohort(transaction, modification);
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction(TransactionIdentifier identifier) {
        return super.newReadOnlyTransaction(identifier);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction(TransactionIdentifier identifier) {
        return super.newReadWriteTransaction(identifier);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction(TransactionIdentifier identifier) {
        return super.newWriteOnlyTransaction(identifier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public LocalThreePhaseCommitCohort onTransactionReady(@Nonnull DOMStoreWriteTransaction tx,
            @Nullable Exception operationError) {
        if(operationError != null) {
            return new LocalChainThreePhaseCommitCohort((SnapshotBackedWriteTransaction<TransactionIdentifier>)tx,
                    operationError);
        }

        try {
            return (LocalThreePhaseCommitCohort) tx.ready();
        } catch (Exception e) {
            // Unfortunately we need to cast to SnapshotBackedWriteTransaction here as it's required by
            // LocalThreePhaseCommitCohort and the base class.
            return new LocalChainThreePhaseCommitCohort((SnapshotBackedWriteTransaction<TransactionIdentifier>)tx, e);
        }
    }

    private class LocalChainThreePhaseCommitCohort extends LocalThreePhaseCommitCohort {

        protected LocalChainThreePhaseCommitCohort(SnapshotBackedWriteTransaction<TransactionIdentifier> transaction,
                DataTreeModification modification) {
            super(parent.getActorContext(), leader, transaction, modification);
        }

        protected LocalChainThreePhaseCommitCohort(SnapshotBackedWriteTransaction<TransactionIdentifier> transaction,
                Exception operationError) {
            super(parent.getActorContext(), leader, transaction, operationError);
        }

        @Override
        protected void transactionAborted(SnapshotBackedWriteTransaction<TransactionIdentifier> transaction) {
            onTransactionFailed(transaction, ABORTED);
        }

        @Override
        protected void transactionCommitted(SnapshotBackedWriteTransaction<TransactionIdentifier> transaction) {
            onTransactionCommited(transaction);
        }
    }
}
