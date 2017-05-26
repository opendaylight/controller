/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.dom.broker.osgi;

import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.osgi.framework.ServiceReference;

public class DOMDataBrokerProxy extends AbstractBrokerServiceProxy<DOMDataBroker> implements DOMDataBroker {

    public DOMDataBrokerProxy(final ServiceReference<DOMDataBroker> ref, final DOMDataBroker delegate) {
        super(ref, delegate);
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return getDelegate().newReadOnlyTransaction();
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return getDelegate().newReadWriteTransaction();
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return getDelegate().newWriteOnlyTransaction();
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store,
            final YangInstanceIdentifier path, final DOMDataChangeListener listener,
            final org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope triggeringScope) {
        return getDelegate().registerDataChangeListener(store, path, listener, triggeringScope);
    }

    @Override
    public DOMTransactionChain createTransactionChain(final TransactionChainListener listener) {
        return getDelegate().createTransactionChain(listener);
    }

    @Override
    public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
        return getDelegate().getSupportedExtensions();
    }
}
