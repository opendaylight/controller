/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

public class HeliumRpcProviderRegistry implements RpcProviderRegistry {

    private final RpcConsumerRegistry consumerRegistry;
    private final BindingDOMRpcProviderServiceAdapter providerAdapter;

    public HeliumRpcProviderRegistry(final RpcConsumerRegistry consumerRegistry,
            final BindingDOMRpcProviderServiceAdapter providerAdapter) {
        this.consumerRegistry = consumerRegistry;
        this.providerAdapter = providerAdapter;
    }

    @Override
    public <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(final Class<T> type, final T impl)
            throws IllegalStateException {
        return new CompositeRoutedRpcRegistration<>(type,impl,providerAdapter);
    }

    @Override
    public <T extends RpcService> RpcRegistration<T> addRpcImplementation(final Class<T> type, final T impl)
            throws IllegalStateException {
        final ObjectRegistration<T> reg = providerAdapter.registerRpcImplementation(type, impl);
        return new DelegatedRootRpcRegistration<>(type,reg);
    }

    @Override
    public <T extends RpcService> T getRpcService(final Class<T> type) {
        return consumerRegistry.getRpcService(type);
    }

    @Override
    public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
            final L arg0) {
        // FIXME: Implement this only if necessary
        return null;
    }

}
