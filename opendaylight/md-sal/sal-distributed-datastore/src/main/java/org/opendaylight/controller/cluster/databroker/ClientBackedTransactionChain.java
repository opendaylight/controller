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
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

/**
 * An implementation of {@link DOMStoreTransactionChain} backed by a {@link ClientLocalHistory}.
 *
 * @author Robert Varga
 */
final class ClientBackedTransactionChain implements DOMStoreTransactionChain {
    private final ClientLocalHistory history;
    private RefcountedReadTransaction readTx;

    ClientBackedTransactionChain(final ClientLocalHistory history) {
        this.history = Preconditions.checkNotNull(history);
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        if (readTx == null) {
            readTx = new RefcountedReadTransaction(this, createTransaction());
        }

        return readTx.newTransaction();
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        abortReadTx();
        return new ClientBackedReadWriteTransaction(createTransaction());
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        abortReadTx();
        return new ClientBackedWriteTransaction(createTransaction());
    }

    @Override
    public void close() {
        abortReadTx();
        history.close();
    }

    private void abortReadTx() {
        if (readTx != null) {
            readTx.abort();
            readTx = null;
        }
    }

    private ClientTransaction createTransaction() {
        try {
            return history.createTransaction();
        } catch (org.opendaylight.mdsal.common.api.TransactionChainClosedException e) {
            throw new TransactionChainClosedException("Transaction chain has been closed", e);
        }
    }

    void finishReadTx(final RefcountedReadTransaction finished) {
        if (readTx == finished) {
            readTx = null;
        }
    }
}
