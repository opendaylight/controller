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

import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.sal.core.api.Broker;
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

    protected boolean removeRpcImplementation(final RpcRegistrationWrapper implToRemove) {
        return registrations.remove(implToRemove);
    }

    @Override
    public RpcRegistration addRpcImplementation(final QName rpcType, final RpcImplementation implementation) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public RoutedRpcRegistration addRoutedRpcImplementation(final QName rpcType, final RpcImplementation implementation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RoutedRpcRegistration addMountedRpcImplementation(final QName rpcType, final RpcImplementation implementation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        for (final RpcRegistrationWrapper reg : registrations) {
            reg.close();
        }
    }

    @Override
    public Set<QName> getSupportedRpcs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(final RpcRegistrationListener listener) {
        return null;
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
