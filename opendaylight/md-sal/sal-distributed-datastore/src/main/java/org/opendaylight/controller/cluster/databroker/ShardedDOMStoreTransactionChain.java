/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;

/**
 * Implementation of {@link DOMStoreTransactionChain} backed by a {@link ClientLocalHistory}. It wraps
 * {@link ClientTransaction} into proxies like {@link ShardedDOMStoreReadTransaction} to provide isolation.
 *
 * @author Robert Varga
 */
final class ShardedDOMStoreTransactionChain implements DOMStoreTransactionChain {
    private final ClientLocalHistory history;

    ShardedDOMStoreTransactionChain(final ClientLocalHistory history) {
        this.history = Preconditions.checkNotNull(history);
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new ShardedDOMStoreReadTransaction(history.createTransaction());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new ShardedDOMStoreReadWriteTransaction(history.createTransaction());
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new ShardedDOMStoreWriteTransaction(history.createTransaction());
    }

    @Override
    public void close() {
        history.close();
    }
}
