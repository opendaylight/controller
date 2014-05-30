/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

/**
 * Factory for DOM Store Transactions
 *
 * <p>
 * Factory provides method to construct read-only, read-write and write-only
 * transactions, which may be used to retrieve and modify stored information in
 * Underlying {@link DOMStore} or {@link DOMStoreTransactionChain}.
 *
 * <p>
 * See {@link DOMStore} or {@link DOMStoreTransactionChain} for concrete
 * variations of this factory.
 *
 * <p>
 * <b>Note:</b> This interface is used only to define common functionality
 * between {@link DOMStore} and {@link DOMStoreTransactionChain}, which
 * further specify behaviour of returned transactions.
 *
 */
public interface DOMStoreTransactionFactory {

    /**
     *
     * Creates a read only transaction
     *
     * <p>
     * Creates a new read-only transaction, which provides read access to
     * snapshot of current state.
     *
     * See {@link DOMStoreReadTransaction} for more information.
     *
     * @return new {@link DOMStoreReadTransaction}
     * @throws IllegalStateException
     *             If state of factory prevents allocating new transaction.
     *
     */
    DOMStoreReadTransaction newReadOnlyTransaction();

    /**
     * Creates write only transaction
     *
     * <p>
     * See {@link DOMStoreWriteTransaction} for more information.
     *
     * @return new {@link DOMStoreWriteTransaction}
     * @throws IllegalStateException If state of factory prevents allocating new transaction.
     */
    DOMStoreWriteTransaction newWriteOnlyTransaction();

    /**
     * Creates Read-Write transaction
     *
     * <p>
     * See {@link DOMStoreReadWriteTransaction} for more information.
     *
     * @return  new {@link DOMStoreWriteTransaction}
     * @throws IllegalStateException If state of factory prevents allocating new transaction.
     */
    DOMStoreReadWriteTransaction newReadWriteTransaction();

}
