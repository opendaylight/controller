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
 * A factory which allocates new transactions to operate on the data
 * tree.
 *
 * <p>
 * <b>Note:</b> This interface is not intended to be used directly, but rather
 * via subinterfaces which introduces additional semantics to allocated
 * transactions.
 * <ul>
 * <li> {@link AsyncDataBroker}
 * <li> {@link TransactionChain}
 * </ul>
 *
 * <p>
 * All operations on the data tree are performed via one of the transactions:
 * <ul>
 * <li>Read-Only - allocated using {@link #newReadOnlyTransaction()}
 * <li>Write-Only - allocated using {@link #newWriteOnlyTransaction()}
 * <li>Read-Write - allocated using {@link #newReadWriteTransaction()}
 * </ul>
 *
 * <p>
 * These transactions provides a stable isolated view of the data tree, which is
 * guaranteed to be not affected by other concurrent transactions, until
 * transaction is committed.
 *
 * <p>
 * For a detailed explanation of how transaction are isolated and how transaction-local
 * changes are committed to global data tree, see
 * {@link AsyncReadTransaction}, {@link AsyncWriteTransaction},
 * {@link AsyncReadWriteTransaction} and {@link AsyncWriteTransaction#commit()}.
 *
 * <p>
 * It is strongly recommended to use the type of transaction, which
 * provides only the minimal capabilities you need. This allows for
 * optimizations at the data broker / data store level. For example,
 * implementations may optimize the transaction for reading if they know ahead
 * of time that you only need to read data - such as not keeping additional meta-data,
 * which may be required for write transactions.
 *<p>
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
     * Allocates a new read-only transaction which provides an immutable snapshot of
     * the data tree.
     *<p>
     * The view of data tree is an immutable snapshot of current data tree state when
     * transaction was allocated.
     *
     * @return new read-only transaction
     */
    AsyncReadOnlyTransaction<P, D> newReadOnlyTransaction();

    /**
     * Allocates new read-write transaction which provides a mutable view of the data
     * tree.
     *
     * <p>
     * Preconditions for mutation of data tree are captured from the snapshot of
     * data tree state, when the transaction is allocated. If data was
     * changed during transaction in an incompatible way then the commit of this transaction
     * will fail. See {@link AsyncWriteTransaction#commit()} for more
     * details about conflicting and not-conflicting changes and
     * failure scenarios.
     *
     * @return new read-write transaction
     */
    AsyncReadWriteTransaction<P, D> newReadWriteTransaction();

    /**
     * Allocates new write-only transaction based on latest state of data
     * tree.
     *
     * <p>
     * Preconditions for mutation of data tree are captured from the snapshot of
     * data tree state, when the transaction is allocated. If data was
     * changed during transaction in an incompatible way then the commit of this transaction
     * will fail. See {@link AsyncWriteTransaction#commit()} for more
     * details about conflicting and not-conflicting changes and
     * failure scenarios.
     *
     * <p>
     * Since this transaction does not provide a view of the data it SHOULD BE
     * used only by callers which are exclusive writers (exporters of data)
     * to the subtree they modify. This prevents optimistic
     * lock failures as described in {@link AsyncWriteTransaction#commit()}.
     * <p>
     * Exclusivity of writers to particular subtree SHOULD BE enforced by
     * external locking mechanism.
     *
     * @return new write-only transaction
     */
    AsyncWriteTransaction<P, D> newWriteOnlyTransaction();

}
