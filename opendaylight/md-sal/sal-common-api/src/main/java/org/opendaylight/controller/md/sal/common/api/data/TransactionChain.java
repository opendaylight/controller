/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

/**
 * A chain of transactions. Transactions in a chain need to be committed in sequence and each
 * transaction should see the effects of previous transactions as if they happened. A chain
 * makes no guarantees of atomicity, in fact transactions are committed as soon as possible.
 *
 */
public interface TransactionChain<P extends Path<P>, D> extends AutoCloseable, AsyncDataTransactionFactory<P, D> {

    /**
     * Create a new read only transaction which will continue the chain.
     * The previous read-write transaction has to be either COMMITED or CANCELLED.
     *
     * @return New transaction in the chain.
     * @throws IllegalStateException if the previous transaction was not COMMITED
     *    or CANCELLED.
     * @throws TransactionChainClosedException if the chain has been closed.
     */
    @Override
    public AsyncReadTransaction<P, D> newReadOnlyTransaction();


    /**
     * Create a new read write transaction which will continue the chain.
     * The previous read-write transaction has to be either COMMITED or CANCELLED.
     *
     * @return New transaction in the chain.
     * @throws IllegalStateException if the previous transaction was not COMMITTED
     *    or CANCELLED.
     * @throws TransactionChainClosedException if the chain has been closed.
     */
    @Override
    public AsyncReadWriteTransaction<P, D> newReadWriteTransaction();

    @Override
    void close();
}

