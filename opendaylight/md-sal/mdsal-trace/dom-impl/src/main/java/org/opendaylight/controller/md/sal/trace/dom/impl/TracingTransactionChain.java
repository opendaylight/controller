/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.dom.impl;

import java.util.Objects;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.AbstractCloseTracked;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTrackedRegistry;

class TracingTransactionChain extends AbstractCloseTracked<TracingTransactionChain> implements DOMTransactionChain {

    private final DOMTransactionChain delegate;
    private final TracingBroker tracingBroker;
    private final CloseTrackedRegistry<TracingReadOnlyTransaction> readOnlyTransactionsRegistry;
    private final CloseTrackedRegistry<TracingWriteTransaction> writeTransactionsRegistry;
    private final CloseTrackedRegistry<TracingReadWriteTransaction> readWriteTransactionsRegistry;

    TracingTransactionChain(DOMTransactionChain delegate, TracingBroker tracingBroker,
            CloseTrackedRegistry<TracingTransactionChain> transactionChainsRegistry) {
        super(transactionChainsRegistry);
        this.delegate = Objects.requireNonNull(delegate);
        this.tracingBroker = Objects.requireNonNull(tracingBroker);

        final boolean isDebugging = transactionChainsRegistry.isDebugContextEnabled();
        this.readOnlyTransactionsRegistry  = new CloseTrackedRegistry<>(this, "newReadOnlyTransaction", isDebugging);
        this.writeTransactionsRegistry     = new CloseTrackedRegistry<>(this, "newWriteOnlyTransaction", isDebugging);
        this.readWriteTransactionsRegistry = new CloseTrackedRegistry<>(this, "newReadWriteTransaction", isDebugging);
    }

    @Override
    @SuppressWarnings("resource")
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        final DOMDataReadOnlyTransaction tx = delegate.newReadOnlyTransaction();
        return new TracingReadOnlyTransaction(tx, tracingBroker, readOnlyTransactionsRegistry);
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return new TracingReadWriteTransaction(delegate.newReadWriteTransaction(), tracingBroker,
                readWriteTransactionsRegistry);
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        final DOMDataWriteTransaction tx = delegate.newWriteOnlyTransaction();
        return new TracingWriteTransaction(tx, tracingBroker, writeTransactionsRegistry);
    }

    @Override
    public void close() {
        delegate.close();
        super.removeFromTrackedRegistry();
    }

}
