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
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * TransactionChainProxy acts as a proxy for a DOMStoreTransactionChain created on a remote shard
 */
public class TransactionChainProxy implements DOMStoreTransactionChain{
    private final ActorContext actorContext;
    private final SchemaContext schemaContext;

    public TransactionChainProxy(ActorContext actorContext, SchemaContext schemaContext) {
        this.actorContext = actorContext;
        this.schemaContext = schemaContext;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new TransactionProxy(actorContext,
            TransactionProxy.TransactionType.READ_ONLY, schemaContext);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new TransactionProxy(actorContext,
            TransactionProxy.TransactionType.WRITE_ONLY, schemaContext);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new TransactionProxy(actorContext,
            TransactionProxy.TransactionType.READ_WRITE, schemaContext);
    }

    @Override
    public void close() {
        // FIXME : The problem here is don't know which shard the transaction chain is to be created on ???
        throw new UnsupportedOperationException("close - not sure what to do here?");
    }
}
