/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

/**
 * TransactionChainProxy acts as a proxy for a DOMStoreTransactionChain created on a remote shard
 */
public class TransactionChainProxy implements DOMStoreTransactionChain{
    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        throw new UnsupportedOperationException("newReadOnlyTransaction");
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        throw new UnsupportedOperationException("newReadWriteTransaction");
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        throw new UnsupportedOperationException("newWriteOnlyTransaction");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("close");
    }
}
