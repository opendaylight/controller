/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.util.Set;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

public class BindingRpcProviderServiceAdapter {
    private final org.opendaylight.mdsal.binding.api.RpcProviderService delegate;

    public BindingRpcProviderServiceAdapter(final org.opendaylight.mdsal.binding.api.RpcProviderService delegate) {
        this.delegate = delegate;
    }

    public <S extends RpcService, T extends S> ObjectRegistration<T> registerRpcImplementation(final Class<S> type,
            final T implementation) {
        return delegate.registerRpcImplementation(type, implementation);
    }

    public <S extends RpcService, T extends S> ObjectRegistration<T> registerRpcImplementation(final Class<S> type,
            final T implementation, final Set<InstanceIdentifier<?>> paths) {
        return delegate.registerRpcImplementation(type, implementation, paths);
    }
}
