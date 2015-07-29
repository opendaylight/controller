/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.messagebus.app.impl;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.messagebus.app.impl.EventSourceTopology;
import org.opendaylight.controller.messagebus.app.util.Providers;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageBusAppImplModule extends org.opendaylight.controller.config.yang.messagebus.app.impl.AbstractMessageBusAppImplModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageBusAppImplModule.class);

    private BundleContext bundleContext;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public MessageBusAppImplModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MessageBusAppImplModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver,
            final MessageBusAppImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final ProviderContext bindingCtx = getBindingBrokerDependency().registerProvider(new Providers.BindingAware());
        final DataBroker dataBroker = bindingCtx.getSALService(DataBroker.class);
        final RpcProviderRegistry rpcRegistry = bindingCtx.getSALService(RpcProviderRegistry.class);
        final EventSourceTopology eventSourceTopology = new EventSourceTopology(dataBroker, rpcRegistry);
        LOGGER.info("Messagebus initialized");
        return eventSourceTopology;
    }

}
