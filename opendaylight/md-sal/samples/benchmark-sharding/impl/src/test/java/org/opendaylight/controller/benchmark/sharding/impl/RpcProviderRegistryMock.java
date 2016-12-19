/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.benchmark.sharding.impl;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

public class RpcProviderRegistryMock implements RpcProviderRegistry {

    @Override
    public <T extends RpcService> T getRpcService(final Class<T> serviceInterface) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <L extends RouteChangeListener<RpcContextIdentifier,
        InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
            final L listener) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends RpcService> RpcRegistration<T> addRpcImplementation(final Class<T> serviceInterface,
            final T implementation)
            throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(final Class<T> serviceInterface,
            final T implementation) throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

}
