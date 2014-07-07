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
 * A chain of transactions. Transactions in a chain need to be committed in
 * sequence and each transaction should see the effects of previous transactions
 * as if they happened. A chain makes no guarantees of atomicity, in fact
 * transactions are committed as soon as possible, but in order as they were
 * allocated.
 *
 * This behaviour is different from the default AsyncDataBroker, where a
 * transaction is always created from the current global state, not taking into
 * account any transactions previously committed by the calling thread. Due to
 * the asynchronous nature of transaction submission this can lead to surprising
 * results. If a thread executes the following sequence sufficiently quickly:
 *
 * AsyncWriteTransaction t1 = broker.newWriteOnlyTransaction();
 * t1.put(id, data);
 * t1.submit();
 *
 * AsyncReadTransaction t2 = broker.newReadOnlyTransaction();
 * Optional<?> maybeData = t2.read(id).get();
 *
 * it may happen, that it sees maybeData.isPresent() == false, simply because
 * t1 has not completed the processes of being applied and t2 is actually
 * allocated from the previous state. This is obviously bad for users who create
 * incremental state in the datastore and actually read what they write in
 * subsequent transactions.
 *
 * Using a TransactionChain instead of a broker solves this particular problem,
 * and leads to expected behavior: t2 will always see the data written in t1
 * present.
 */
public interface TransactionChain<P extends Path<P>, D> extends AutoCloseable,
        AsyncDataTransactionFactory<P, D> {

    /**
     * Create a new read only transaction which will continue the chain.
     *
     * <p>
     * The previous write transaction has to be either SUBMITTED (
     * {@link AsyncWriteTransaction#commit()} was invoked) or CANCELLED.
     * <p>
     * Returned read-only transaction presents isolated view as if previous
     * read-write transaction was successful. State which was introduced by
     * other transactions outside this transaction chain after creation of
     * previous transaction is not visible.
     *
     * @return New transaction in the chain.
     * @throws IllegalStateException
     *             if the previous transaction was not COMMITED or CANCELLED.
     * @throws TransactionChainClosedException
     *             if the chain has been closed.
     */
    @Override
    public AsyncReadOnlyTransaction<P, D> newReadOnlyTransaction();

    /**
     * Create a new read write transaction which will continue the chain.
     *
     * <p>
     * The previous write transaction has to be either SUBMITTED (
     * {@link AsyncWriteTransaction#commit()} was invoked) or CANCELLED.
     * <p>
     * Returned read-write transaction presents isolated view as if previous
     * read-write transaction was successful. State which was introduced by
     * other transactions outside this transaction chain after creation of
     * previous transaction is not visible.
     * <p>
     * Committing this read-write transaction using
     * {@link AsyncWriteTransaction#commit()} will submit changes in this
     * transaction to be available also outside this transaction chain.
     *
     * @return New transaction in the chain.
     * @throws IllegalStateException
     *             if the previous transaction was not COMMITTED or CANCELLED.
     * @throws TransactionChainClosedException
     *             if the chain has been closed.
     */
    @Override
    public AsyncReadWriteTransaction<P, D> newReadWriteTransaction();

    /**
     * Create a new write-only transaction which will continue the chain.
     *
     * <p>
     * The previous write transaction has to be either SUBMITTED (
     * {@link AsyncWriteTransaction#commit()} was invoked) or CANCELLED.
     * <p>
     * Returned read-write transaction presents isolated view as if previous
     * read-write transaction was successful. State which was introduced by
     * other transactions outside this transaction chain after creation of
     * previous transaction is not visible.
     * <p>
     * Committing this read-write transaction using
     * {@link AsyncWriteTransaction#commit()} will submit changes in this
     * transaction to be available also outside this transaction chain.
     *
     * @return New transaction in the chain.
     * @throws IllegalStateException
     *             if the previous transaction was not SUBMITTED or CANCELLED.
     * @throws TransactionChainClosedException
     *             if the chain has been closed.
     */
    @Override
    public AsyncWriteTransaction<P, D> newWriteOnlyTransaction();

    @Override
    void close();
}
