/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class AbstractBindingAwareProvider extends AbstractBrokerAwareActivator implements BindingAwareProvider {
    
    @Override
    protected final void onBrokerAvailable(BindingAwareBroker broker, BundleContext context) {
        ProviderContext ctx = broker.registerProvider(this, context);
        registerRpcImplementations(ctx);
        registerFunctionality(ctx);
    }

    private void registerFunctionality(ProviderContext ctx) {
        Collection<? extends ProviderFunctionality> functionality = this.getFunctionality();
        if (functionality == null || functionality.isEmpty()) {
            return;
        }
        for (ProviderFunctionality providerFunctionality : functionality) {
            ctx.registerFunctionality(providerFunctionality);
        }

    }

    private void registerRpcImplementations(ProviderContext ctx) {
        Collection<? extends RpcService> rpcs = this.getImplementations();
        if (rpcs == null || rpcs.isEmpty()) {
            return;
        }
        for (RpcService rpcService : rpcs) {
            // ctx.addRpcImplementation(type, implementation);
        }

    }

    @Override
    public Collection<? extends ProviderFunctionality> getFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public Collection<? extends RpcService> getImplementations() {
        return Collections.emptySet();
    }
    
    /**
     * Initialization of consumer context.
     * 
     * {@link ProviderContext} is replacement of {@link ConsumerContext}
     * so this method is not needed in case of Provider.
     * 
     */
    @Deprecated
    @Override
    public final void onSessionInitialized(ConsumerContext session) {
        // NOOP
    }
}
