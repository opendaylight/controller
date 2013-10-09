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

public abstract class AbstractBindingAwareProvider implements BindingAwareProvider, BundleActivator {

    /**
     * Initializes the bundle.
     * 
     * Implementation of initialization get's the Binding Aware Broker from
     * service registry and register itself as a {@link BindingAwareProvider}
     * 
     * Callback order is:
     * <ol>
     * <li>{@link #startImpl(BundleContext)}
     * <li>{@link #onSessionInitiated(ProviderContext)}
     * <li>Registration of global {@link RpcService} implementations returned by
     * {@link #getImplementations()}
     * <li>Registration of {@link ProviderFunctionality} implementations
     * returned by {@link #getFunctionality()}
     * </ol>
     * 
     */
    @Override
    public final void start(BundleContext context) throws Exception {
        startImpl(context);
        ServiceReference<BindingAwareBroker> brokerRef = context.getServiceReference(BindingAwareBroker.class);
        BindingAwareBroker broker = context.getService(brokerRef);

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

    /**
     * Called when this bundle is started (before
     * {@link #onSessionInitiated(ProviderContext)} so the Framework can perform
     * the bundle-specific activities necessary to start this bundle. This
     * method can be used to register services or to allocate any resources that
     * this bundle needs.
     * 
     * <p>
     * This method must complete and return to its caller in a timely manner.
     * 
     * @param context
     *            The execution context of the bundle being started.
     * @throws Exception
     *             If this method throws an exception, this bundle is marked as
     *             stopped and the Framework will remove this bundle's
     *             listeners, unregister all services registered by this bundle,
     *             and release all services used by this bundle.
     */
    protected void startImpl(BundleContext context) {
        // NOOP
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle. In general, this
     * method should undo the work that the {@code BundleActivator.start} method
     * started. There should be no active threads that were started by this
     * bundle when this bundle returns. A stopped bundle must not call any
     * Framework objects.
     * 
     * <p>
     * This method must complete and return to its caller in a timely manner.
     * 
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is still
     *         marked as stopped, and the Framework will remove the bundle's
     *         listeners, unregister all services registered by the bundle, and
     *         release all services used by the bundle.
     */
    protected void stopImpl(BundleContext context) {
        // NOOP
    }

    /**
     * Bundle stop callback
     * 
     * 
     * Custom implementation of bundle stop could be carried by overriding
     * {@link #stopImpl(BundleContext)} method.
     * 
     */
    @Override
    public final void stop(BundleContext context) throws Exception {
        stopImpl(context);
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
