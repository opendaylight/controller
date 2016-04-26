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
import java.util.LinkedHashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.datastore.actors.localhistory.TransactionContext.Fate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LocalHistoryContext {
    private static final Logger LOG = LoggerFactory.getLogger(LocalHistoryContext.class);
    private final Map<Long, TransactionContext> transactions = new LinkedHashMap<>(2);
    private final LocalHistoryIdentifier historyId;
    private final DataTree dataTree;
    private final ActorRef parent;
    private final ActorRef self;

    private TransactionContext lastSubmittedTransaction;
    private Long lastCommittedTx;

    LocalHistoryContext(final ActorRef parent, final ActorRef self, final LocalHistoryIdentifier historyId, final DataTree dataTree) {
        this.historyId = Preconditions.checkNotNull(historyId);
        this.dataTree = Preconditions.checkNotNull(dataTree);
        this.parent = Preconditions.checkNotNull(parent);
        this.self = Preconditions.checkNotNull(self);
    }

    LocalHistoryIdentifier getHistoryId() {
        return historyId;
    }

    ActorRef getParent() {
        return parent;
    }

    ActorRef getSelf() {
        return self;
    }

    TransactionContext allocateTransaction(final GlobalTransactionIdentifier transactionId) {
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
        transactions.put(tx.getTransactionId(), tx);
    }

    void releaseTransaction(final long transactionId) {
        final TransactionContext tx = transactions.remove(transactionId);
        if (tx != null) {
            LOG.debug("Released transaction {} context {}", transactionId, tx);
        } else {
            LOG.warn("Attempted to release non-existing transaction {}", transactionId);
        }
    }

    TransactionContext getTransaction(final long transactionId) {
        return transactions.get(transactionId);
    }

    boolean isEmpty() {
        return transactions.isEmpty();
    }
}
