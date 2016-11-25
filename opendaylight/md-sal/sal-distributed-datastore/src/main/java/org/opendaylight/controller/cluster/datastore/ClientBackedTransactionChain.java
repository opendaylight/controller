/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
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

    ClientBackedTransactionChain(final ClientLocalHistory history) {
        this.history = Preconditions.checkNotNull(history);
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new ClientBackedReadTransaction(history.createTransaction());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new ClientBackedReadWriteTransaction(history.createTransaction());
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new ClientBackedWriteTransaction(history.createTransaction());
    }

    @Override
    public void close() {
        history.close();
    }
}
