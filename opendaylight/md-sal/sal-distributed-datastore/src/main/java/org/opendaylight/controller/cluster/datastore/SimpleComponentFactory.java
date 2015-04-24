/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
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
import scala.concurrent.Future;

// FIXME: this should be integrated into DistributedDataStore
final class SimpleComponentFactory extends ComponentFactory {
    private final TransactionReadyPrototype<TransactionIdentifier> readyImpl = new TransactionReadyPrototype<TransactionIdentifier>() {
        @Override
        protected DOMStoreThreePhaseCommitCohort transactionReady(SnapshotBackedWriteTransaction<TransactionIdentifier> tx, DataTreeModification tree) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected void transactionAborted(SnapshotBackedWriteTransaction<TransactionIdentifier> tx) {
            // Intentional no-op, as we do not need to do anything
        }
    };

    protected SimpleComponentFactory(ActorContext actorContext) {
        super(actorContext);
    }

    private TransactionIdentifier nextIdentifier() {
        // FIXME: implement this
        return null;
    }

    @Override
    protected DOMStoreTransactionFactory factoryForShard(final String shardName, final DataTree dataTree) {
        return new DOMStoreTransactionFactory() {
            @Override
            public DOMStoreReadTransaction newReadOnlyTransaction() {
                return SnapshotBackedTransactions.newReadTransaction(nextIdentifier(), false, dataTree.takeSnapshot());
            }

            @Override
            public DOMStoreWriteTransaction newWriteOnlyTransaction() {
                return SnapshotBackedTransactions.newWriteTransaction(nextIdentifier(), false, dataTree.takeSnapshot(), readyImpl);
            }

            @Override
            public DOMStoreReadWriteTransaction newReadWriteTransaction() {
                return SnapshotBackedTransactions.newReadWriteTransaction(nextIdentifier(), false, dataTree.takeSnapshot(), readyImpl);
            }
        };
    }

    @Override
    protected Future<PrimaryShardInfo> findPrimaryShard(String shardName) {
        return getActorContext().findPrimaryShardAsync(shardName);
    }
}
