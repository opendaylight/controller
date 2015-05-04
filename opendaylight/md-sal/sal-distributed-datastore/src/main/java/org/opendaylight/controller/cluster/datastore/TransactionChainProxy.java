/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import scala.concurrent.Future;

/**
 * TransactionChainProxy acts as a proxy for a DOMStoreTransactionChain created on a remote shard
 */
public class TransactionChainProxy implements DOMStoreTransactionChain {

    private interface State {
        boolean isReady();

        List<Future<Object>> getPreviousReadyFutures();
    }

    private static class Allocated implements State {
        private final ChainedTransactionProxy transaction;

        Allocated(ChainedTransactionProxy transaction) {
            this.transaction = transaction;
        }

        @Override
        public boolean isReady() {
            return transaction.isReady();
        }

        @Override
        public List<Future<Object>> getPreviousReadyFutures() {
            return transaction.getReadyFutures();
        }
    }

    private static abstract class AbstractDefaultState implements State {
        @Override
        public List<Future<Object>> getPreviousReadyFutures() {
            return Collections.emptyList();
        }
    }

    private static final State IDLE_STATE = new AbstractDefaultState() {
        @Override
        public boolean isReady() {
            return true;
        }
    };

    private static final State CLOSED_STATE = new AbstractDefaultState() {
        @Override
        public boolean isReady() {
            throw new TransactionChainClosedException("Transaction chain has been closed");
        }
    };

    private static final AtomicInteger counter = new AtomicInteger(0);

    private final ActorContext actorContext;
    private final String transactionChainId;
    private volatile State currentState = IDLE_STATE;

    public TransactionChainProxy(ActorContext actorContext) {
        this.actorContext = actorContext;
        transactionChainId = actorContext.getCurrentMemberName() + "-txn-chain-" + counter.incrementAndGet();
    }

    public String getTransactionChainId() {
        return transactionChainId;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        State localState = currentState;
        checkReadyState(localState);

        return new ChainedTransactionProxy(actorContext, TransactionType.READ_ONLY,
                transactionChainId, localState.getPreviousReadyFutures());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        actorContext.acquireTxCreationPermit();
        return allocateWriteTransaction(TransactionType.READ_WRITE);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        actorContext.acquireTxCreationPermit();
        return allocateWriteTransaction(TransactionType.WRITE_ONLY);
    }

    @Override
    public void close() {
        currentState = CLOSED_STATE;

        // Send a close transaction chain request to each and every shard
        actorContext.broadcast(new CloseTransactionChain(transactionChainId).toSerializable());
    }

    private ChainedTransactionProxy allocateWriteTransaction(TransactionType type) {
        State localState = currentState;

        checkReadyState(localState);

        // Pass the ready Futures from the previous Tx.
        ChainedTransactionProxy txProxy = new ChainedTransactionProxy(actorContext, type,
                transactionChainId, localState.getPreviousReadyFutures());

        currentState = new Allocated(txProxy);

        return txProxy;
    }

    private void checkReadyState(State state) {
        Preconditions.checkState(state.isReady(), "Previous transaction is not ready yet");
    }
}
