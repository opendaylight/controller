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
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * TransactionChainProxy acts as a proxy for a DOMStoreTransactionChain created on a remote shard
 */
public class TransactionChainProxy implements DOMStoreTransactionChain {
    private interface State {
        boolean isReady();

        SimpleEntry<Object, List<Future<ActorSelection>>> getReadyFutures();

        void setReadyFutures(Object txIdentifier, List<Future<ActorSelection>> readyFutures);
    }

    private static class Allocated implements State {
        private volatile SimpleEntry<Object, List<Future<ActorSelection>>> readyFutures;

        @Override
        public boolean isReady() {
            return readyFutures != null;
        }

        @Override
        public SimpleEntry<Object, List<Future<ActorSelection>>> getReadyFutures() {
            return readyFutures != null ? readyFutures : EMPTY_READY_FUTURES;
        }

        @Override
        public void setReadyFutures(Object txIdentifier, List<Future<ActorSelection>> readyFutures) {
            this.readyFutures = new SimpleEntry<>(txIdentifier, readyFutures);
        }
    }

    private static abstract class AbstractDefaultState implements State {
        @Override
        public SimpleEntry<Object, List<Future<ActorSelection>>> getReadyFutures() {
            return EMPTY_READY_FUTURES;
        }

        @Override
        public void setReadyFutures(Object txIdentifier, List<Future<ActorSelection>> readyFutures) {
            throw new IllegalStateException("No transaction is allocated");
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

    private static final SimpleEntry<Object, List<Future<ActorSelection>>> EMPTY_READY_FUTURES =
            new SimpleEntry<Object, List<Future<ActorSelection>>>("",
                    Collections.<Future<ActorSelection>>emptyList());

    private static final AtomicReferenceFieldUpdater<TransactionChainProxy, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(TransactionChainProxy.class, State.class, "state");

    private final ActorContext actorContext;
    private final String transactionChainId;
    private volatile State state = IDLE_STATE;

    public TransactionChainProxy(ActorContext actorContext) {
        this.actorContext = actorContext;
        transactionChainId = actorContext.getCurrentMemberName() + "-" + System.currentTimeMillis();
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        checkReadyState();
        return new ChainedTransactionProxy(actorContext, TransactionProxy.TransactionType.READ_ONLY);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return allocateWriteTransaction(TransactionProxy.TransactionType.READ_WRITE);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return allocateWriteTransaction(TransactionProxy.TransactionType.WRITE_ONLY);
    }

    @Override
    public void close() {
        state = CLOSED_STATE;

        // Send a close transaction chain request to each and every shard
        actorContext.broadcast(new CloseTransactionChain(transactionChainId));
    }

    private ChainedTransactionProxy allocateWriteTransaction(TransactionProxy.TransactionType type) {
        checkReadyState();

        ChainedTransactionProxy txProxy = new ChainedTransactionProxy(actorContext, type);
        STATE_UPDATER.compareAndSet(this, IDLE_STATE, new Allocated());

        return txProxy;
    }

    private void checkReadyState() {
        Preconditions.checkState(state.isReady(), "Previous transaction %s is not ready yet",
                state.getReadyFutures().getKey());
    }

    private class ChainedTransactionProxy extends TransactionProxy {

        ChainedTransactionProxy(ActorContext actorContext, TransactionType transactionType) {
            super(actorContext, transactionType, transactionChainId);
        }

        @Override
        protected void onTransactionReady(List<Future<ActorSelection>> readyFutures) {
            state.setReadyFutures(getIdentifier(), readyFutures);
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
            // The second check is done to ensure the the previous ready Futures aren't for this
            // Tx instance as deadlock would occur if we tried to wait on our own Futures. This can
            // occur in this scenario:
            //
            //     - the TransactionProxy is created and the client does a write.
            //
            //     - the TransactionProxy then attempts to create the shard Tx. However it first
            //       sends a FindPrimaryShard message to the shard manager to find the local shard
            //       This call is done async.
            //
            //     - the client submits the Tx and the TransactionProxy is readied and we cache
            //       the ready Futures here.
            //
            //     - then the FindPrimaryShard call completes and this method is called to create
            //       the shard Tx. However the cached Futures were from the ready on this Tx. If we
            //       tried to wait on them, it would cause a form of deadlock as the ready Future
            //       would be waiting on the Tx create Future and vice versa.
            SimpleEntry<Object, List<Future<ActorSelection>>> readyFuturesEntry = state.getReadyFutures();
            List<Future<ActorSelection>> readyFutures = readyFuturesEntry.getValue();
            if(readyFutures.isEmpty() || getIdentifier().equals(readyFuturesEntry.getKey())) {
                return super.sendCreateTransaction(shard, serializedCreateMessage);
            }

            // Combine the ready Futures into 1.
            Future<Iterable<ActorSelection>> combinedFutures = akka.dispatch.Futures.sequence(
                    readyFutures, actorContext.getActorSystem().dispatcher());

            // Add a callback for completion of the combined Futures.
            final Promise<Object> createTxPromise = akka.dispatch.Futures.promise();
            OnComplete<Iterable<ActorSelection>> onComplete = new OnComplete<Iterable<ActorSelection>>() {
                @Override
                public void onComplete(Throwable failure, Iterable<ActorSelection> notUsed) {
                    if(failure != null) {
                        // A Ready Future failed so fail the returned Promise.
                        createTxPromise.failure(failure);
                    } else {
                        // Send the CreateTx message and use the resulting Future to complete the
                        // returned Promise.
                        createTxPromise.completeWith(actorContext.executeOperationAsync(shard,
                                serializedCreateMessage));
                    }
                }
            };

            combinedFutures.onComplete(onComplete, actorContext.getActorSystem().dispatcher());

            return createTxPromise.future();
        }
    }
}
