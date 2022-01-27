/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorSelection;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.mdsal.dom.spi.store.AbstractSnapshotBackedTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.SnapshotBackedWriteTransaction;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.tree.api.ReadOnlyDataTree;

/**
 * Transaction chain instantiated on top of a locally-available DataTree. It does not instantiate
 * a transaction in the leader and rather chains transactions on top of themselves.
 */
final class LocalTransactionChain extends AbstractSnapshotBackedTransactionChain<TransactionIdentifier>
        implements LocalTransactionFactory {
    private static final Throwable ABORTED = new Throwable("Transaction aborted");
    private final TransactionChainProxy parent;
    private final ActorSelection leader;
    private final ReadOnlyDataTree tree;

    LocalTransactionChain(final TransactionChainProxy parent, final ActorSelection leader,
            final ReadOnlyDataTree tree) {
        this.parent = requireNonNull(parent);
        this.leader = requireNonNull(leader);
        this.tree = requireNonNull(tree);
    }

    ReadOnlyDataTree getDataTree() {
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
            final DataTreeModification modification,
            final Exception operationError) {
        return new LocalChainThreePhaseCommitCohort(transaction, modification, operationError);
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction(final TransactionIdentifier identifier) {
        return super.newReadOnlyTransaction(identifier);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction(final TransactionIdentifier identifier) {
        return super.newReadWriteTransaction(identifier);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction(final TransactionIdentifier identifier) {
        return super.newWriteOnlyTransaction(identifier);
    }

    @SuppressWarnings({"unchecked", "checkstyle:IllegalCatch"})
    @Override
    public LocalThreePhaseCommitCohort onTransactionReady(final DOMStoreWriteTransaction tx, final Exception operationError) {
        checkArgument(tx instanceof SnapshotBackedWriteTransaction);
        if (operationError != null) {
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

        protected LocalChainThreePhaseCommitCohort(final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction,
                final DataTreeModification modification, final Exception operationError) {
            super(parent.getActorUtils(), leader, transaction, modification, operationError);
        }

        protected LocalChainThreePhaseCommitCohort(final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction,
                final Exception operationError) {
            super(parent.getActorUtils(), leader, transaction, operationError);
        }

        @Override
        protected void transactionAborted(final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction) {
            onTransactionFailed(transaction, ABORTED);
        }

        @Override
        protected void transactionCommitted(final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction) {
            onTransactionCommited(transaction);
        }
    }
}
