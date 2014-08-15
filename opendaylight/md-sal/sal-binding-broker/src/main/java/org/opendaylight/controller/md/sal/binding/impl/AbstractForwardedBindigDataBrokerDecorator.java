/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentConnector;
import org.opendaylight.controller.sal.binding.impl.forward.DomForwardedBroker;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public abstract class AbstractForwardedBindigDataBrokerDecorator implements DataBroker, Delegator<DOMDataBroker>, DomForwardedBroker, SchemaContextListener, AutoCloseable {

    protected ForwardedBindingDataBroker bindingDataBroker;
    
    public AbstractForwardedBindigDataBrokerDecorator(ForwardedBindingDataBroker bindingDataBroker) {
        this.bindingDataBroker = bindingDataBroker;
    }

    protected BindingToNormalizedNodeCodec getCodec() {
        return bindingDataBroker.getCodec();
    }
    
    @Override
    public ReadOnlyTransaction newReadOnlyTransaction() {
        return bindingDataBroker.newReadOnlyTransaction();
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        return bindingDataBroker.newReadWriteTransaction();
    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        return bindingDataBroker.newWriteOnlyTransaction();
    }

    @Override
    public BindingTransactionChain createTransactionChain(TransactionChainListener listener) {
        return bindingDataBroker.createTransactionChain(listener);
    }

    @Override
    public DOMDataBroker getDelegate() {
        return bindingDataBroker.getDelegate();
    }

    @Override
    public void onGlobalContextUpdated(SchemaContext ctx) {
        bindingDataBroker.onGlobalContextUpdated(ctx);
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(LogicalDatastoreType store, InstanceIdentifier<?> path, DataChangeListener listener, DataChangeScope triggeringScope) {
        return bindingDataBroker.registerDataChangeListener(store, path, listener, triggeringScope);
    }

    @Override
    public BindingIndependentConnector getConnector() {
        return bindingDataBroker.getConnector();
    }

    @Override
    public Broker.ProviderSession getDomProviderContext() {
        return bindingDataBroker.getDomProviderContext();
    }

    @Override
    public void setConnector(BindingIndependentConnector connector) {
        bindingDataBroker.setConnector(connector);
    }

    @Override
    public void setDomProviderContext(Broker.ProviderSession domProviderContext) {
        bindingDataBroker.setDomProviderContext(domProviderContext);
    }

    @Override
    public void startForwarding() {
        bindingDataBroker.startForwarding();
    }

    @Override
    public void close() throws Exception {
        bindingDataBroker.close();
    }
}
