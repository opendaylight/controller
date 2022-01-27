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
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.SnapshotBackedTransactions;
import org.opendaylight.mdsal.dom.spi.store.SnapshotBackedWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.SnapshotBackedWriteTransaction.TransactionReadyPrototype;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.ReadOnlyDataTree;

/**
 * {@link LocalTransactionFactory} for instantiating backing transactions which are
 * disconnected from each other, ie not chained. These are used by {@link AbstractTransactionContextFactory}
 * to instantiate transactions on shards which are co-located with the shard leader.
 */
final class LocalTransactionFactoryImpl extends TransactionReadyPrototype<TransactionIdentifier>
        implements LocalTransactionFactory {

    private final ActorSelection leader;
    private final ReadOnlyDataTree dataTree;
    private final ActorUtils actorUtils;

    LocalTransactionFactoryImpl(final ActorUtils actorUtils, final ActorSelection leader,
            final ReadOnlyDataTree dataTree) {
        this.leader = requireNonNull(leader);
        this.dataTree = requireNonNull(dataTree);
        this.actorUtils = actorUtils;
    }

    ReadOnlyDataTree getDataTree() {
        return dataTree;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction(final TransactionIdentifier identifier) {
        return SnapshotBackedTransactions.newReadTransaction(identifier, false, dataTree.takeSnapshot());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction(final TransactionIdentifier identifier) {
        return SnapshotBackedTransactions.newReadWriteTransaction(identifier, false, dataTree.takeSnapshot(), this);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction(final TransactionIdentifier identifier) {
        return SnapshotBackedTransactions.newWriteTransaction(identifier, false, dataTree.takeSnapshot(), this);
    }

    @Override
    protected void transactionAborted(final SnapshotBackedWriteTransaction<TransactionIdentifier> tx) {
        // No-op
    }

    @Override
    protected DOMStoreThreePhaseCommitCohort transactionReady(
            final SnapshotBackedWriteTransaction<TransactionIdentifier> tx,
            final DataTreeModification tree,
            final Exception readyError) {
        return new LocalThreePhaseCommitCohort(actorUtils, leader, tx, tree, readyError);
    }

    @SuppressWarnings({"unchecked", "checkstyle:IllegalCatch"})
    @Override
    public LocalThreePhaseCommitCohort onTransactionReady(final DOMStoreWriteTransaction tx, final Exception operationError) {
        checkArgument(tx instanceof SnapshotBackedWriteTransaction);
        if (operationError != null) {
            return new LocalThreePhaseCommitCohort(actorUtils, leader,
                    (SnapshotBackedWriteTransaction<TransactionIdentifier>)tx, operationError);
        }

        return (LocalThreePhaseCommitCohort) tx.ready();
    }
}
