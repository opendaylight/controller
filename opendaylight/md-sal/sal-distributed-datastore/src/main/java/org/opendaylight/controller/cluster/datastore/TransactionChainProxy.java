/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

import java.util.concurrent.ExecutorService;

/**
 * TransactionChainProxy acts as a proxy for a DOMStoreTransactionChain created on a remote shard
 */
public class TransactionChainProxy implements DOMStoreTransactionChain{
    private final ActorContext actorContext;
    private final ExecutorService transactionExecutor;

    public TransactionChainProxy(ActorContext actorContext, ExecutorService transactionExecutor) {
        this.actorContext = actorContext;
        this.transactionExecutor = transactionExecutor;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new TransactionProxy(actorContext,
            TransactionProxy.TransactionType.READ_ONLY, transactionExecutor);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new TransactionProxy(actorContext,
            TransactionProxy.TransactionType.WRITE_ONLY, transactionExecutor);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new TransactionProxy(actorContext,
            TransactionProxy.TransactionType.READ_WRITE, transactionExecutor);
    }

    @Override
    public void close() {
        // FIXME : The problem here is don't know which shard the transaction chain is to be created on ???
        throw new UnsupportedOperationException("close - not sure what to do here?");
    }
}
