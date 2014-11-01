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
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * TransactionChainProxy acts as a proxy for a DOMStoreTransactionChain created on a remote shard
 */
public class TransactionChainProxy implements DOMStoreTransactionChain{
    private final ActorContext actorContext;
    private final String transactionChainId;
    private volatile SimpleEntry<Object, List<Future<ActorSelection>>> previousTxReadyFutures;

    public TransactionChainProxy(ActorContext actorContext) {
        this.actorContext = actorContext;
        transactionChainId = actorContext.getCurrentMemberName() + "-" + System.currentTimeMillis();
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new ChainedTransactionProxy(actorContext, TransactionProxy.TransactionType.READ_ONLY);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new ChainedTransactionProxy(actorContext, TransactionProxy.TransactionType.READ_WRITE);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new ChainedTransactionProxy(actorContext, TransactionProxy.TransactionType.WRITE_ONLY);
    }

    @Override
    public void close() {
        // Send a close transaction chain request to each and every shard
        actorContext.broadcast(new CloseTransactionChain(transactionChainId));
    }

    public String getTransactionChainId() {
        return transactionChainId;
    }

    private class ChainedTransactionProxy extends TransactionProxy {

        ChainedTransactionProxy(ActorContext actorContext, TransactionType transactionType) {
            super(actorContext, transactionType, transactionChainId);
        }

        @Override
        protected void onTransactionReady(List<Future<ActorSelection>> cohortFutures) {
            if(!cohortFutures.isEmpty()) {
                previousTxReadyFutures = new SimpleEntry<>(getIdentifier(), cohortFutures);
            } else {
                previousTxReadyFutures = null;
            }
        }

        /**
         * This method is overridden to ensure the previous Tx's ready operations complete
         * before we create the next shard Tx in the chain to avoid creation failures if the
         * previous Tx's ready operations haven't completed yet.
         */
        @Override
        protected Future<Object> sendCreateTransaction(final ActorSelection shard,
                final Object serializedCreateMessage) {
            // Check if there are any previous ready Futures. Also make sure the previous ready
            // Futures aren't for this Tx as deadlock would occur if tried to wait on our own
            // Futures. This may happen b/c the shard Tx creates are done async so it's possible
            // for the client to ready this Tx before we've even attempted to create a shard Tx.
            if(previousTxReadyFutures == null ||
                    previousTxReadyFutures.getKey().equals(getIdentifier())) {
                return super.sendCreateTransaction(shard, serializedCreateMessage);
            }

            // Combine the ready Futures into 1.
            Future<Iterable<ActorSelection>> combinedFutures = akka.dispatch.Futures.sequence(
                    previousTxReadyFutures.getValue(), actorContext.getActorSystem().dispatcher());

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
