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
import org.opendaylight.controller.cluster.datastore.identifiers.ChainedTransactionIdentifier;
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
    private final ActorSelection leader;
    private final String memberName;
    private final DataTree tree;

    LocalTransactionChain(final ShardedTransactionChain parent, final String memberName, final ActorSelection leader, final DataTree tree) {
        this.parent = Preconditions.checkNotNull(parent);
        this.memberName = Preconditions.checkNotNull(memberName);
        this.leader = Preconditions.checkNotNull(leader);
        this.tree = Preconditions.checkNotNull(tree);
    }

    @Override
    protected TransactionIdentifier nextTransactionIdentifier() {
        return new ChainedTransactionIdentifier(memberName, parent.currentCounter(), parent.getTransactionChainId());
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
        return new LocalThreePhaseCommitCohort(parent.getActorContext(), leader, transaction, modification) {
            @Override
            protected void transactionAborted(final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction) {
                onTransactionFailed(transaction, ABORTED);
            }

            @Override
            protected void transactionCommitted(final SnapshotBackedWriteTransaction<TransactionIdentifier> transaction) {
                onTransactionCommited(transaction);
            }
        };
    }
}
