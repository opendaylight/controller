/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

/**
 * Interface for creating transaction chains.
 */
public interface TransactionChainFactory<P extends Path<P>, D> {

    /**
     * Create a new transaction chain. The chain will be initialized to read
     * from its backing datastore, with no outstanding transaction. Listener
     * will be registered to handle chain-level events.
     *
     * @param listener Transaction chain event listener
     * @return A new transaction chain.
     */
    TransactionChain<P, D> createTransactionChain(TransactionChainListener listener);
}

