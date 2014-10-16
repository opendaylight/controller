/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.Collections;
import java.util.List;

/**
 * TransactionChainProxy acts as a proxy for a DOMStoreTransactionChain created on a remote shard
 */
public class TransactionChainProxy implements DOMStoreTransactionChain{
    private final ActorContext actorContext;
    private final String transactionChainId;
    private volatile List<Future<ActorSelection>> cohortFutures = Collections.emptyList();

    public TransactionChainProxy(ActorContext actorContext) {
        this.actorContext = actorContext;
        transactionChainId = actorContext.getCurrentMemberName() + "-" + System.currentTimeMillis();
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new TransactionProxy(actorContext,
            TransactionProxy.TransactionType.READ_ONLY, this);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new TransactionProxy(actorContext,
            TransactionProxy.TransactionType.READ_WRITE, this);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new TransactionProxy(actorContext,
            TransactionProxy.TransactionType.WRITE_ONLY, this);
    }

    @Override
    public void close() {
        // Send a close transaction chain request to each and every shard
        actorContext.broadcast(new CloseTransactionChain(transactionChainId));
    }

    public String getTransactionChainId() {
        return transactionChainId;
    }

    public void onTransactionReady(List<Future<ActorSelection>> cohortFutures){
        this.cohortFutures = cohortFutures;
    }

    public void waitTillCurrentTransactionReady(){
        try {
            Await.result(Futures
                .sequence(this.cohortFutures, actorContext.getActorSystem().dispatcher()),
                actorContext.getOperationDuration());
        } catch (Exception e) {
            throw new IllegalStateException("Failed when waiting for transaction on a chain to become ready", e);
        }
    }
}
