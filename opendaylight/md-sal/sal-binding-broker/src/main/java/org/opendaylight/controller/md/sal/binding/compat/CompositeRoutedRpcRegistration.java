/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

final class CompositeRoutedRpcRegistration<T extends RpcService> implements RoutedRpcRegistration<T> {

    private final Class<T> type;
    private final T instance;
    private final BindingDOMRpcProviderServiceAdapter adapter;
    private final Map<InstanceIdentifier<?>, ObjectRegistration<T>> registrations = new HashMap<>(2);

    CompositeRoutedRpcRegistration(final Class<T> type, final T impl, final BindingDOMRpcProviderServiceAdapter providerAdapter) {
        this.type = type;
        this.instance = impl;
        this.adapter = providerAdapter;
    }

    @Override
    public Class<T> getServiceType() {
        return type;
    }

    @Override
    public T getInstance() {
        return instance;
    }

    @Deprecated
    @Override
    public void registerInstance(final Class<? extends BaseIdentity> context, final InstanceIdentifier<?> path) {
        registerPath(context, path);
    }

    @Override
    public synchronized void registerPath(final Class<? extends BaseIdentity> context, final InstanceIdentifier<?> path) {
        if(!registrations.containsKey(path)) {
            registrations.put(path, adapter.registerRpcImplementation(type, instance, ImmutableSet.<InstanceIdentifier<?>>of(path)));
        }
    }


    @Override
    @Deprecated
    public void unregisterInstance(final Class<? extends BaseIdentity> context, final InstanceIdentifier<?> path) {
        unregisterPath(context, path);
    }

    @Override
    public synchronized  void unregisterPath(final Class<? extends BaseIdentity> context, final InstanceIdentifier<?> path) {
        final ObjectRegistration<T> reg = registrations.remove(path);
        if(reg != null) {
            try {
                reg.close();
            } catch (final Exception e) {
                // FIXME: Once we have proper subclass of ObjectRegistrationo
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    public synchronized void close() {
        try {
            for(final ObjectRegistration<T> reg : registrations.values()) {
                    reg.close();
            }
        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
    }

}
