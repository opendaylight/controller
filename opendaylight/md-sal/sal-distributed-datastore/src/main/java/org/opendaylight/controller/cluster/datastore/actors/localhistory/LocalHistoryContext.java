/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Queue;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.datastore.actors.localhistory.TransactionContext.Fate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

final class LocalHistoryContext {
    private final Queue<TransactionContext> pendingTransactions = new ArrayDeque<>(2);
    private final LocalHistoryIdentifier historyId;
    private final DataTree dataTree;
    private final ActorRef self;

    private TransactionContext lastSubmittedTransaction;
    private Long lastCommittedTx;

    LocalHistoryContext(final ActorRef self, final LocalHistoryIdentifier historyId, final DataTree dataTree) {
        this.historyId = Preconditions.checkNotNull(historyId);
        this.dataTree = Preconditions.checkNotNull(dataTree);
        this.self = Preconditions.checkNotNull(self);
    }

    LocalHistoryIdentifier getHistoryId() {
        return historyId;
    }

    ActorRef getSelf() {
        return self;
    }

    final TransactionContext allocateTransaction(final GlobalTransactionIdentifier transactionId) {
        final DataTreeModification mod;
        if (lastSubmittedTransaction == null) {
            mod = dataTree.takeSnapshot().newModification();
        } else {
            mod = lastSubmittedTransaction.nextTransaction();
        }

        return new TransactionContext(transactionId, mod);
    }

    Long getLastCommittedTx() {
        return lastCommittedTx;
    }

    void recordTransaction(final TransactionContext tx, final Fate fate) {
        tx.setFate(fate);
        pendingTransactions.add(tx);

    }

    TransactionContext getTransaction(long transactionId) {
        // TODO Auto-generated method stub
        return null;
    }
}
