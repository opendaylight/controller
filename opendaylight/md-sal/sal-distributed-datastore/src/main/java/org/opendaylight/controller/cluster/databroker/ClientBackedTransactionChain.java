/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link DOMStoreTransactionChain} backed by a {@link ClientLocalHistory}.
 *
 * @author Robert Varga
 */
final class ClientBackedTransactionChain implements DOMStoreTransactionChain {
    private static final Logger LOG = LoggerFactory.getLogger(ClientBackedTransactionChain.class);

    @GuardedBy("this")
    private final Map<ClientTransaction, Boolean> openSnapshots = new WeakHashMap<>();

    private final ClientLocalHistory history;

    ClientBackedTransactionChain(final ClientLocalHistory history) {
        this.history = Preconditions.checkNotNull(history);
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new ClientBackedReadTransaction(createTransaction(), this);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new ClientBackedReadWriteTransaction(createTransaction());
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new ClientBackedWriteTransaction(createTransaction());
    }

    @Override
    public synchronized void close() {
        for (ClientTransaction snap : openSnapshots.keySet()) {
            LOG.warn("Aborting unclosed transaction {}", snap.getIdentifier());
            snap.abort();
        }
        openSnapshots.clear();

        history.close();
    }

    synchronized void snapshotClosed(final ClientTransaction clientTransaction) {
        openSnapshots.remove(clientTransaction);
    }

    private ClientTransaction createTransaction() {
        final ClientTransaction ret;

        try {
            ret = history.createTransaction();
        } catch (org.opendaylight.mdsal.common.api.TransactionChainClosedException e) {
            throw new TransactionChainClosedException("Transaction chain has been closed", e);
        }


        synchronized (this) {
            openSnapshots.put(ret, Boolean.TRUE);
        }
        return ret;
    }
}
