/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import java.util.EventListener;

/**
 * Listener for transaction chain events.
 */
public interface TransactionChainListener extends EventListener {
    /**
     * Invoked if when a transaction in the chain fails. All other transactions are automatically cancelled by the time
     * this notification is invoked. Implementations should invoke chain.close() to close the chain.
     *
     * @param chain Transaction chain which failed
     * @param transaction Transaction which caused the chain to fail
     * @param cause The cause of transaction failure
     */
    void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction, Throwable cause);

    /**
     * Invoked when a transaction chain is completed. A transaction chain is considered completed when it has been
     * closed and all its instructions have completed successfully.
     *
     * @param chain Transaction chain which completed
     */
    void onTransactionChainSuccessful(TransactionChain<?, ?> chain);
}

