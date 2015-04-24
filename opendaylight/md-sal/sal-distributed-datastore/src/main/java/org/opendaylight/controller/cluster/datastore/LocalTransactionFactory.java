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
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedTransactions;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction.TransactionReadyPrototype;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DOMStoreTransactionFactory} for instantiating backing transactions which are
 * disconnected from each other. These are used by {@link AbstractTransactionComponentFactory} to instantiate
 * transactions on shards which are co-located with the shard leader.
 */
final class LocalTransactionFactory extends TransactionReadyPrototype<TransactionIdentifier> implements DOMStoreTransactionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(LocalTransactionFactory.class);
    private final SingleTransactionComponentFactory parent;
    private final ActorSelection leader;
    private final DataTree dataTree;

    LocalTransactionFactory(final SingleTransactionComponentFactory parent, final ActorSelection leader, final DataTree dataTree) {
        this.leader = Preconditions.checkNotNull(leader);
        this.dataTree = Preconditions.checkNotNull(dataTree);
        this.parent = Preconditions.checkNotNull(parent);
    }

    DataTree getDataTree() {
        return dataTree;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return SnapshotBackedTransactions.newReadTransaction(parent.nextIdentifier(), false, dataTree.takeSnapshot());
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return SnapshotBackedTransactions.newWriteTransaction(parent.nextIdentifier(), false, dataTree.takeSnapshot(), this);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return SnapshotBackedTransactions.newReadWriteTransaction(parent.nextIdentifier(), false, dataTree.takeSnapshot(), this);
    }

    @Override
    protected void transactionAborted(final SnapshotBackedWriteTransaction<TransactionIdentifier> tx) {
        // No-op
    }

    @Override
    protected DOMStoreThreePhaseCommitCohort transactionReady(final SnapshotBackedWriteTransaction<TransactionIdentifier> tx,
            final DataTreeModification tree) {
        return new LocalThreePhaseCommitCohort(parent.getActorContext(), leader, tx, tree) {
            @Override
            protected void transactionAborted(final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction) {
                // No-op
                LOG.debug("Transaction {} aborted", transaction);
            }

            @Override
            protected void transactionCommitted(final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction) {
                // No-op
                LOG.debug("Transaction {} committed", transaction);
            }
        };
    }
}
