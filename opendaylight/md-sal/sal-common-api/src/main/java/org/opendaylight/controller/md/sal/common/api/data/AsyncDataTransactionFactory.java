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
 * Transaction factory which allocates new transactions which operates on data
 * tree.
 *
 * <b>Note:</b> This interface is not intended to be used directly, but rather
 * via subinterfaces which introduces additional semantics to allocated
 * transactions.
 * <ul>
 * <li> {@link AsyncDataBroker}
 * <li> {@link TransactionChain}
 * </ul>
 *
 * All operations on data tree are performed via one of the transactions:
 * <ul>
 * <li>Read-Only - allocated using {@link #newReadOnlyTransaction()}
 * <li>Write-Only - allocated using {@link #newWriteOnlyTransaction()}
 * <li>Read-Write - allocated using {@link #newReadWriteTransaction()}
 * </ul>
 *
 * This transactions provides stable isolated view of data tree, which is
 * guaranteed to be not affected by other concurrent transactions, until
 * transaction is committed.
 *
 * For detailed explanation of isolated on transaction concurrency, see
 * {@link AsyncReadTransaction}, {@link AsyncWriteTransaction},
 * {@link AsyncReadWriteTransaction} and {@link AsyncWriteTransaction#commit()}
 * for more details how transaction are isolated and how transaction-local
 * changes are commited to global data tree.
 *
 * It is strongly recommended to use the type of transaction, which which
 * provides only the minimal capabilities you need. This allows for
 * optimisations at the data broker / data store level. For example,
 * implementations may optimise the transaction for reading if they know ahead
 * of time that you only need to read data in such way, that they do not need to
 * keep additional metadata, which may be required for write transactions.
 *
 * <b>Implementation Note:</b> This interface is not intended to be implemented
 * by users of MD-SAL, but only to be consumed by them.
 *
 * @see AsyncDataBroker
 * @see TransactionChain
 *
 * @param <P>
 *            Type of path (subtree identifier), which represents location in
 *            tree
 * @param <D>
 *            Type of data (payload), which represents data payload
 */
public interface AsyncDataTransactionFactory<P extends Path<P>, D> {

    /**
     * Allocates new Read-only transaction which provides latest snapshot of
     * immutable view of data tree, which could be consumed by clients.
     *
     * View of data tree is immutable snapshot of current data tree state when
     * transaction was allocated.
     *
     * @return new read-only transaction
     */
    AsyncReadTransaction<P, D> newReadOnlyTransaction();

    /**
     * Allocates new write-only transaction provides latest mutable view of data
     * tree, which could be consumed by clients.
     *
     * Preconditions for mutation of data tree are captured from snapshot of
     * data tree state, when transaction was allocated.
     *
     * @return new read-write transaction
     */
    AsyncReadWriteTransaction<P, D> newReadWriteTransaction();

    /**
     * Allocates new write-only transaction provides latest mutable view of data
     * tree, which could be consumed by clients.
     *
     * Preconditions for mutation of data tree are captured from snapshot of
     * data tree state, when transaction was allocated.
     *
     * @return new write-only transaction
     */
    AsyncWriteTransaction<P, D> newWriteOnlyTransaction();

}
