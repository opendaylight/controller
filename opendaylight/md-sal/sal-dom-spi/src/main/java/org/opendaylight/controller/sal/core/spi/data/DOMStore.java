/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * DOM Data Store
 *
 * <p>
 * DOM Data Store provides transactional tree-like storage for YANG-modeled
 * entities described by YANG schema and represented by {@link NormalizedNode}.
 *
 * <p>
 * Read and write access to stored data is provided only via transactions
 * created using {@link #newReadOnlyTransaction()},
 * {@link #newWriteOnlyTransaction()} and {@link #newReadWriteTransaction()}, or
 * by creating {@link org.opendaylight.controller.md.sal.common.api.data.TransactionChain}.
 *
 */
public interface DOMStore extends DOMStoreTransactionFactory {
    /**
     * Creates new transaction chain.
     *
     * <p>
     * Transactions in a chain need to be committed in sequence and each
     * transaction should see the effects of previous transactions as if they
     * happened.
     *
     * @see DOMStoreTransactionChain for more information.
     * @return Newly created transaction chain.
     */
    DOMStoreTransactionChain createTransactionChain();
}
