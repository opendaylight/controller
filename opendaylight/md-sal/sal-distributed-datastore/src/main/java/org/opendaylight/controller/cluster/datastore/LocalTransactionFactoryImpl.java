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
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedTransactions;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction.TransactionReadyPrototype;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * {@link LocalTransactionFactory} for instantiating backing transactions which are
 * disconnected from each other, ie not chained. These are used by {@link AbstractTransactionContextFactory}
 * to instantiate transactions on shards which are co-located with the shard leader.
 */
final class LocalTransactionFactoryImpl extends TransactionReadyPrototype<TransactionIdentifier>
        implements LocalTransactionFactory {

    private final ActorSelection leader;
    private final DataTree dataTree;
    private final ActorContext actorContext;

    LocalTransactionFactoryImpl(final ActorContext actorContext, final ActorSelection leader, final DataTree dataTree) {
        this.leader = Preconditions.checkNotNull(leader);
        this.dataTree = Preconditions.checkNotNull(dataTree);
        this.actorContext = actorContext;
    }

    DataTree getDataTree() {
        return dataTree;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction(TransactionIdentifier identifier) {
        return SnapshotBackedTransactions.newReadTransaction(identifier, false, dataTree.takeSnapshot());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction(TransactionIdentifier identifier) {
        return SnapshotBackedTransactions.newReadWriteTransaction(identifier, false, dataTree.takeSnapshot(), this);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction(TransactionIdentifier identifier) {
        return SnapshotBackedTransactions.newWriteTransaction(identifier, false, dataTree.takeSnapshot(), this);
    }

    @Override
    protected void transactionAborted(final SnapshotBackedWriteTransaction<TransactionIdentifier> tx) {
        // No-op
    }

    @Override
    protected DOMStoreThreePhaseCommitCohort transactionReady(final SnapshotBackedWriteTransaction<TransactionIdentifier> tx,
            final DataTreeModification tree) {
        return new LocalThreePhaseCommitCohort(actorContext, leader, tx, tree);
    }

    @SuppressWarnings("unchecked")
    @Override
    public LocalThreePhaseCommitCohort onTransactionReady(@Nonnull DOMStoreWriteTransaction tx,
            @Nullable Exception operationError) {
        if(operationError != null) {
            return new LocalThreePhaseCommitCohort(actorContext, leader,
                    (SnapshotBackedWriteTransaction<TransactionIdentifier>)tx, operationError);
        }

        try {
            return (LocalThreePhaseCommitCohort) tx.ready();
        } catch (Exception e) {
            // Unfortunately we need to cast to SnapshotBackedWriteTransaction here as it's required by
            // LocalThreePhaseCommitCohort.
            return new LocalThreePhaseCommitCohort(actorContext, leader,
                    (SnapshotBackedWriteTransaction<TransactionIdentifier>)tx, e);
        }
    }
}
