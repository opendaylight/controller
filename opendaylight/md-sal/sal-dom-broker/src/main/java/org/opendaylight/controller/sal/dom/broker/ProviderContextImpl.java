/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;

class ProviderContextImpl extends ConsumerContextImpl implements ProviderSession {
    private final Set<RpcRegistrationWrapper> registrations = new HashSet<>();
    private final Provider provider;

    public ProviderContextImpl(final Provider provider, final BrokerImpl broker) {
        super(null, broker);
        this.provider = provider;
    }

    @Override
    public RpcRegistrationWrapper addRpcImplementation(final QName rpcType,
            final RpcImplementation implementation) throws IllegalArgumentException {
        final RpcRegistration origReg = getBrokerChecked().getRouter()
                .addRpcImplementation(rpcType, implementation);
        final RpcRegistrationWrapper newReg = new RpcRegistrationWrapper(
                origReg);
        registrations.add(newReg);
        return newReg;
    }

    protected boolean removeRpcImplementation(final RpcRegistrationWrapper implToRemove) {
        return registrations.remove(implToRemove);
    }

    @Override
    public void close() {
        for (final RpcRegistrationWrapper reg : registrations) {
            reg.close();
        }
    }

    @Override
    public RoutedRpcRegistration addMountedRpcImplementation(
            final QName rpcType, final RpcImplementation implementation) {
        throw new UnsupportedOperationException(
                "TODO: auto-generated method stub");

    }

    @Override
    public RoutedRpcRegistration addRoutedRpcImplementation(
            final QName rpcType, final RpcImplementation implementation) {
        return getBrokerChecked().getRouter().addRoutedRpcImplementation(rpcType, implementation);
    }

    @Override
    public Set<QName> getSupportedRpcs() {
        return getBrokerChecked().getRouter().getSupportedRpcs();
    }

    @Override
    public ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(
            final RpcRegistrationListener listener) {
        return getBrokerChecked().getRouter().addRpcRegistrationListener(listener);
    }

    /**
     * @return the provider
     */
    public Provider getProvider() {
        return provider;
    }

    /**
     * @param provider
     *            the provider to set
     */
}
