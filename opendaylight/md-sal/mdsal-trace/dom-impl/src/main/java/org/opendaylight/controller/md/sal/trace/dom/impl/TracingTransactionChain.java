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

class TracingTransactionChain implements DOMTransactionChain {

    private final DOMTransactionChain delegate;
    private final TracingBroker tracingBroker;

    TracingTransactionChain(DOMTransactionChain delegate, TracingBroker tracingBroker) {
        this.delegate = Objects.requireNonNull(delegate);
        this.tracingBroker = Objects.requireNonNull(tracingBroker);
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return new TracingReadOnlyTransaction(delegate.newReadOnlyTransaction(), tracingBroker);
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return new TracingReadWriteTransaction(delegate.newReadWriteTransaction(), tracingBroker);
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return new TracingWriteTransaction(delegate.newWriteOnlyTransaction(), tracingBroker);
    }

    @Override
    public void close() {
        delegate.close();
    }

}
