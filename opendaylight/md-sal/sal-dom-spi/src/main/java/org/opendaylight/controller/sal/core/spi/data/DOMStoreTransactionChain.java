/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

/**
 * A chain of transactions. Transactions in a chain need to be committed in
 * sequence and each transaction must see the effects of previous transactions
 * as if they happened. A chain makes no guarantees of atomicity, in fact
 * transactions are committed as soon as possible.
 */
public interface DOMStoreTransactionChain extends DOMStoreTransactionFactory, AutoCloseable {

    /**
     * Create a new read only transaction which will continue the chain. The
     * previous write transaction has to be either READY or CANCELLED.
     *
     * If previous write transaction was already commited to data store, new
     * read-only transaction is same as obtained via {@link DOMStore#newReadOnlyTransaction()}
     * and contains merged result of previous one and current state of data store.
     *
     * Otherwise read-only transaction presents isolated view as if previous read-write
     * transaction was successful. State which was introduced by other transactions
     * outside this transaction chain after creation of previous transaction is not visible.
     *
     * @return New transaction in the chain.
     * @throws IllegalStateException
     *             if the previous transaction was not READY or CANCELLED, or
     *             if the chain has been closed.
     */
    @Override
    DOMStoreReadTransaction newReadOnlyTransaction();

    /**
     * Create a new read write transaction which will continue the chain. The
     * previous read-write transaction has to be either COMMITED or CANCELLED.
     *
     * If previous write transaction was already commited to data store, new
     * read-write transaction is same as obtained via {@link DOMStore#newReadWriteTransaction()}
     * and contains merged result of previous one and current state of data store.
     *
     * Otherwise read-write transaction presents isolated view as if previous read-write
     * transaction was successful. State which was introduced by other transactions
     * outside this transaction chain after creation of previous transaction is not visible.
     *
     * @return New transaction in the chain.
     * @throws IllegalStateException
     *             if the previous transaction was not READY or CANCELLED, or
     *             if the chain has been closed.
     */
    @Override
    DOMStoreReadWriteTransaction newReadWriteTransaction();

    /**
     * Create a new write-only transaction which will continue the chain. The
     * previous read-write transaction has to be either READY or CANCELLED.
     *
     *
     * @return New transaction in the chain.
     * @throws IllegalStateException
     *             if the previous transaction was not READY or CANCELLED, or
     *             if the chain has been closed.
     */
    @Override
    DOMStoreWriteTransaction newWriteOnlyTransaction();

    /**
     * Closes Transaction Chain.
     *
     * Close method of transaction chain does not guarantee that
     * last alocated transaction is ready or was submitted.
     *
     * @throws IllegalStateException If any of the outstanding created transactions was not canceled or ready.
     */
    @Override
    void close();
}
