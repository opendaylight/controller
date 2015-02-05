/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import javax.annotation.Nonnull;

/**
 * A data producer context. It allows transactions to be submitted to the subtrees
 * specified at instantiation time. At any given time there may be a single transaction
 * open. It needs to be either submitted or cancelled before another one can be open.
 * Once a transaction is submitted, it will proceed to be committed asynchronously.
 *
 * Each instance has  an upper bound on the number of transactions which can be in-flight,
 * once that capacity is exceeded, an attempt to create a new transaction will block
 * until some transactions complete.
 */
public interface DOMDataTreeProducer {
    /**
     * Allocate a new open transaction on this producer. Any and all transactions
     * previously allocated must have been either submitted or cancelled by the
     * time this method is invoked.
     *
     * @return A new {@link DOMDataWriteTransaction}
     * @throws IllegalStateException if a previous transaction was not closed.
     */
    @Nonnull DOMDataWriteTransaction newTransaction();
}
