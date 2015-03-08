/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;


import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMAdapterBuilder.Factory;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;

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


    static final Factory<DataBroker> BUILDER_FACTORY = new BindingDOMAdapterBuilder.Factory<DataBroker>() {

        @Override
        public BindingDOMAdapterBuilder<DataBroker> newBuilder() {
            return new Builder();
        }

    };

    public ForwardedBindingDataBroker(final DOMDataBroker domDataBroker, final BindingToNormalizedNodeCodec codec) {
        super(domDataBroker, codec);
    }

    @Deprecated
    public ForwardedBindingDataBroker(final DOMDataBroker domDataBroker, final BindingToNormalizedNodeCodec codec, final SchemaService schemaService) {
        super(domDataBroker, codec,schemaService);
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

    private static class Builder extends BindingDOMAdapterBuilder<DataBroker> {

        @Override
        public Set<? extends Class<? extends DOMService>> getRequiredDelegates() {
            return ImmutableSet.of(DOMDataBroker.class);
        }

        @Override
        protected DataBroker createInstance(BindingToNormalizedNodeCodec codec,
                ClassToInstanceMap<DOMService> delegates) {
            DOMDataBroker domDataBroker = delegates.getInstance(DOMDataBroker.class);
            return new ForwardedBindingDataBroker(domDataBroker, codec);
        }



    }
}
