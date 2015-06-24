/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * TransactionChainProxy acts as a proxy for a DOMStoreTransactionChain created on a remote shard
 */
public class TransactionChainProxy implements DOMStoreTransactionChain {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionChainProxy.class);

    private interface State {
        boolean isReady();

        List<Future<ActorSelection>> getPreviousReadyFutures();
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
        public List<Future<ActorSelection>> getPreviousReadyFutures() {
            return transaction.getReadyFutures();
        }
    }

    private static abstract class AbstractDefaultState implements State {
        @Override
        public List<Future<ActorSelection>> getPreviousReadyFutures() {
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

        return new ChainedTransactionProxy(actorContext, TransactionProxy.TransactionType.READ_ONLY,
                transactionChainId, localState.getPreviousReadyFutures());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        actorContext.acquireTxCreationPermit();
        return allocateWriteTransaction(TransactionProxy.TransactionType.READ_WRITE);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        actorContext.acquireTxCreationPermit();
        return allocateWriteTransaction(TransactionProxy.TransactionType.WRITE_ONLY);
    }

    @Override
    public void close() {
        currentState = CLOSED_STATE;

        // Send a close transaction chain request to each and every shard
        actorContext.broadcast(new CloseTransactionChain(transactionChainId));
    }

    private ChainedTransactionProxy allocateWriteTransaction(TransactionProxy.TransactionType type) {
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

    private static class ChainedTransactionProxy extends TransactionProxy {

        /**
         * Stores the ready Futures from the previous Tx in the chain.
         */
        private final List<Future<ActorSelection>> previousReadyFutures;

        /**
         * Stores the ready Futures from this transaction when it is readied.
         */
        private volatile List<Future<ActorSelection>> readyFutures;

        private ChainedTransactionProxy(ActorContext actorContext, TransactionType transactionType,
                String transactionChainId, List<Future<ActorSelection>> previousReadyFutures) {
            super(actorContext, transactionType, transactionChainId);
            this.previousReadyFutures = previousReadyFutures;
        }

        List<Future<ActorSelection>> getReadyFutures() {
            return readyFutures;
        }

        boolean isReady() {
            return readyFutures != null;
        }

        @Override
        protected void onTransactionReady(List<Future<ActorSelection>> readyFutures) {
            LOG.debug("onTransactionReady {} pending readyFutures size {} chain {}", getIdentifier(),
                    readyFutures.size(), getTransactionChainId());
            this.readyFutures = readyFutures;
        }

        /**
         * This method is overridden to ensure the previous Tx's ready operations complete
         * before we create the next shard Tx in the chain to avoid creation failures if the
         * previous Tx's ready operations haven't completed yet.
         */
        @Override
        protected Future<Object> sendCreateTransaction(final ActorSelection shard,
                final Object serializedCreateMessage) {

            // Check if there are any previous ready Futures, otherwise let the super class handle it.
            if(previousReadyFutures.isEmpty()) {
                return super.sendCreateTransaction(shard, serializedCreateMessage);
            }

            // Combine the ready Futures into 1.
            Future<Iterable<ActorSelection>> combinedFutures = akka.dispatch.Futures.sequence(
                    previousReadyFutures, getActorContext().getActorSystem().dispatcher());

            // Add a callback for completion of the combined Futures.
            final Promise<Object> createTxPromise = akka.dispatch.Futures.promise();
            OnComplete<Iterable<ActorSelection>> onComplete = new OnComplete<Iterable<ActorSelection>>() {
                @Override
                public void onComplete(Throwable failure, Iterable<ActorSelection> notUsed) {
                    if(failure != null) {
                        // A Ready Future failed so fail the returned Promise.
                        createTxPromise.failure(failure);
                    } else {
                        LOG.debug("Previous Tx readied - sending CreateTransaction for {} on chain {}",
                                getIdentifier(), getTransactionChainId());

                        // Send the CreateTx message and use the resulting Future to complete the
                        // returned Promise.
                        createTxPromise.completeWith(getActorContext().executeOperationAsync(shard,
                                serializedCreateMessage));
                    }
                }
            };

            combinedFutures.onComplete(onComplete, getActorContext().getActorSystem().dispatcher());

            return createTxPromise.future();
        }
    }
}
