/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

final class ChainedTransactionProxy extends TransactionProxy {
    private static final Logger LOG = LoggerFactory.getLogger(ChainedTransactionProxy.class);

    /**
     * Stores the ready Futures from the previous Tx in the chain.
     */
    private final List<Future<ActorSelection>> previousReadyFutures;

    /**
     * Stores the ready Futures from this transaction when it is readied.
     */
    private volatile List<Future<ActorSelection>> readyFutures;

    ChainedTransactionProxy(ActorContext actorContext, TransactionType transactionType,
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
     * before we initiate the next Tx in the chain to avoid creation failures if the
     * previous Tx's ready operations haven't completed yet.
     */
    @Override
    protected Future<ActorSelection> sendFindPrimaryShardAsync(final String shardName) {
        // Check if there are any previous ready Futures, otherwise let the super class handle it.
        if(previousReadyFutures.isEmpty()) {
            return super.sendFindPrimaryShardAsync(shardName);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Waiting for {} previous ready futures for Tx {} on chain {}",
                    previousReadyFutures.size(), getIdentifier(), getTransactionChainId());
        }

        // Combine the ready Futures into 1.
        Future<Iterable<ActorSelection>> combinedFutures = akka.dispatch.Futures.sequence(
                previousReadyFutures, getActorContext().getClientDispatcher());

        // Add a callback for completion of the combined Futures.
        final Promise<ActorSelection> returnPromise = akka.dispatch.Futures.promise();
        OnComplete<Iterable<ActorSelection>> onComplete = new OnComplete<Iterable<ActorSelection>>() {
            @Override
            public void onComplete(Throwable failure, Iterable<ActorSelection> notUsed) {
                if(failure != null) {
                    // A Ready Future failed so fail the returned Promise.
                    returnPromise.failure(failure);
                } else {
                    LOG.debug("Previous Tx readied - sending FindPrimaryShard for {} on chain {}",
                            getIdentifier(), getTransactionChainId());

                    // Send the FindPrimaryShard message and use the resulting Future to complete the
                    // returned Promise.
                    returnPromise.completeWith(ChainedTransactionProxy.super.sendFindPrimaryShardAsync(shardName));
                }
            }
        };

        combinedFutures.onComplete(onComplete, getActorContext().getClientDispatcher());

        return returnPromise.future();
    }
}