/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingAdapterBuilder.Factory;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * The DataBrokerImpl simply defers to the DOMDataBroker for all its operations.
 * All transactions and listener registrations are wrapped by the DataBrokerImpl
 * to allow binding aware components to use the DataBroker transparently.
 *
 * <p>
 * Besides this the DataBrokerImpl and it's collaborators also cache data that
 * is already transformed from the binding independent to binding aware format
 */
public class BindingDataBrokerAdapter implements DataBroker {
    static final Factory<DataBroker> BUILDER_FACTORY = Builder::new;

    private final org.opendaylight.mdsal.binding.api.DataBroker delegate;

    public BindingDataBrokerAdapter(final org.opendaylight.mdsal.binding.api.DataBroker delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public ReadOnlyTransaction newReadOnlyTransaction() {
        return new BindingReadTransactionAdapter(delegate.newReadOnlyTransaction());
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        return new BindingReadWriteTransactionAdapter(delegate.newReadWriteTransaction());
    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        return new BindingWriteTransactionAdapter<>(delegate.newWriteOnlyTransaction());
    }

    @Override
    public BindingTransactionChain createTransactionChain(final TransactionChainListener listener) {
        return new BindingTransactionChainAdapter(delegate, listener);
    }

    @Override
    public <T extends DataObject, L extends DataTreeChangeListener<T>> ListenerRegistration<L>
            registerDataTreeChangeListener(final DataTreeIdentifier<T> treeId, final L listener) {
        final org.opendaylight.mdsal.binding.api.DataTreeIdentifier<T> delegateIdentifier =
                org.opendaylight.mdsal.binding.api.DataTreeIdentifier.create(
                    AbstractBindingTransactionAdapter.convert(treeId.getDatastoreType()), treeId.getRootIdentifier());

        @SuppressWarnings({ "rawtypes", "unchecked" })
        final BindingDataTreeChangeListenerAdapter<T> delegateListener =
            listener instanceof ClusteredDataTreeChangeListener
                    ? new BindingClusteredDOMDataTreeChangeListenerAdapter<>((ClusteredDataTreeChangeListener) listener)
                    : new BindingDataTreeChangeListenerAdapter<>(listener);

        ListenerRegistration<org.opendaylight.mdsal.binding.api.DataTreeChangeListener<T>> reg =
                delegate.registerDataTreeChangeListener(delegateIdentifier, delegateListener);
        return new BindingDataTreeChangeListenerRegistration<>(listener, reg);
    }

    @Override
    public String toString() {
        return "BindingDataBrokerAdapter for " + delegate;
    }

    private static class Builder extends BindingAdapterBuilder<DataBroker> {
        @Override
        public Set<? extends Class<? extends org.opendaylight.mdsal.binding.api.BindingService>>
                getRequiredDelegates() {
            return ImmutableSet.of(org.opendaylight.mdsal.binding.api.DataBroker.class);
        }

        @Override
        protected DataBroker createInstance(
                final ClassToInstanceMap<org.opendaylight.mdsal.binding.api.BindingService> delegates) {
            return new BindingDataBrokerAdapter(delegates.getInstance(
                    org.opendaylight.mdsal.binding.api.DataBroker.class));
        }
    }
}
