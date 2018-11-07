/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ForwardingObject;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;

@Deprecated
public class DOMDataBrokerAdapter extends ForwardingObject implements org.opendaylight.mdsal.dom.api.DOMDataBroker {
    private final DOMDataBroker delegate;

    public DOMDataBrokerAdapter(final DOMDataBroker delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public DOMDataTreeReadTransaction newReadOnlyTransaction() {
        return new DOMDataTreeReadTransactionAdapter(delegate.newReadOnlyTransaction());
    }

    @Override
    public DOMDataTreeWriteTransaction newWriteOnlyTransaction() {
        return new DOMDataTreeWriteTransactionAdapter(delegate.newWriteOnlyTransaction());
    }

    @Override
    public DOMDataTreeReadWriteTransaction newReadWriteTransaction() {
        return new DOMDataTreeReadWriteTransactionAdapter(delegate.newReadWriteTransaction());
    }

    @Override
    public ClassToInstanceMap<DOMDataBrokerExtension> getExtensions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DOMTransactionChain createTransactionChain(final DOMTransactionChainListener listener) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected DOMDataBroker delegate() {
        return delegate;
    }
}
