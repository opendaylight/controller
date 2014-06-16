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
 * Transaction factory which allocates new transactions.
 *
 * <b>Note:</b> This interface is not intended to be used
 * directly, but rather via subinterfaces which introduces
 * additional semantics to allocated transanctions.
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
     * Allocates new Read-only transaction which provides immutable view of data
     * tree, which could be consumed by clients.
     *
     * View of data tree is immutable snapshot of current data tree state when
     * transaction was allocated.
     *
     * @return new read-only transaction
     */
    AsyncReadTransaction<P, D> newReadOnlyTransaction();

    /**
     * Allocates new write-only transaction provides mutable view of data tree,
     * which could be consumed by clients.
     *
     * Preconditions for mutation of data tree are captured from snapshot of
     * data tree state, when transaction was allocated.
     *
     * @return new read-write transaction
     */
    AsyncReadWriteTransaction<P, D> newReadWriteTransaction();

    /**
     * Allocates new write-only transaction provides mutable view of data tree,
     * which could be consumed by clients.
     *
     * Preconditions for mutation of data tree are captured from snapshot of
     * data tree state, when transaction was allocated.
     *
     * @return new write-only transaction
     */
    AsyncWriteTransaction<P, D> newWriteOnlyTransaction();

}
