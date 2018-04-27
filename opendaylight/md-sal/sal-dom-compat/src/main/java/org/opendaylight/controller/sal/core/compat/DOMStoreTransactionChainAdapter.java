/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ForwardingObject;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

public class DOMStoreTransactionChainAdapter extends ForwardingObject implements DOMStoreTransactionChain {
    private final org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain delegate;

    public DOMStoreTransactionChainAdapter(
            final org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    protected org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain delegate() {
        return delegate;
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new DOMStoreWriteTransactionAdapter(delegate.newWriteOnlyTransaction());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new DOMStoreReadWriteTransactionAdapter(delegate.newReadWriteTransaction());
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new DOMStoreReadTransactionAdapter<>(delegate.newReadOnlyTransaction());
    }

    @Override
    public void close() {
        delegate.close();
    }
}
