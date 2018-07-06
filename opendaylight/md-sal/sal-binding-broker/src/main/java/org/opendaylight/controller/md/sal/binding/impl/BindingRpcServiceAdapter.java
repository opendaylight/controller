/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.impl.BindingAdapterBuilder.Factory;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.binding.api.BindingService;
import org.opendaylight.yangtools.yang.binding.RpcService;

public class BindingRpcServiceAdapter implements RpcConsumerRegistry {

    protected static final Factory<RpcConsumerRegistry> BUILDER_FACTORY = Builder::new;

    private final org.opendaylight.mdsal.binding.api.RpcConsumerRegistry delegate;

    public BindingRpcServiceAdapter(final org.opendaylight.mdsal.binding.api.RpcConsumerRegistry delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public <T extends RpcService> T getRpcService(final Class<T> rpcService) {
        Preconditions.checkArgument(rpcService != null, "Rpc Service needs to be specied.");
        return delegate.getRpcService(rpcService);
    }

    private static final class Builder extends BindingAdapterBuilder<RpcConsumerRegistry> {
        @Override
        public Set<? extends Class<? extends BindingService>> getRequiredDelegates() {
            return ImmutableSet.of(org.opendaylight.mdsal.binding.api.RpcConsumerRegistry.class);
        }

        @Override
        protected RpcConsumerRegistry createInstance(ClassToInstanceMap<BindingService> delegates) {
            return new BindingRpcServiceAdapter(delegates.getInstance(
                    org.opendaylight.mdsal.binding.api.RpcConsumerRegistry.class));
        }
    }
}
