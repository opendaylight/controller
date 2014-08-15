/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;


import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

/**
 * The DataBrokerImpl simply defers to the DOMDataBroker for all its operations.
 * All transactions and listener registrations are wrapped by the DataBrokerImpl
 * to allow binding aware components to use the DataBroker transparently.
 *
 * Besides this the DataBrokerImpl and it's collaborators also cache data that
 * is already transformed from the binding independent to binding aware format
 *

 */
public class ForwardedBindingDataBroker extends AbstractForwardedDataBroker implements DataBroker {

    public ForwardedBindingDataBroker(final DOMDataBroker domDataBroker, final BindingIndependentMappingService mappingService, final SchemaService schemaService) {
        super(domDataBroker, mappingService,schemaService);
    }

    @Override

    public ReadOnlyTransaction newReadOnlyTransaction() {
        return new BindingDataReadTransactionImpl(getDelegate().newReadOnlyTransaction(),getCodec());
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        return new BindingDataReadWriteTransactionImpl(getDelegate().newReadWriteTransaction(),getCodec());
    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        return new BindingDataWriteTransactionImpl<>(getDelegate().newWriteOnlyTransaction(),getCodec());
    }

    @Override
    public BindingTransactionChain createTransactionChain(final TransactionChainListener listener) {
        return new BindingTranslatedTransactionChain(getDelegate(), getCodec(), listener);
    }
}
