/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.messagebus.app.impl;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.mdsal.InitializationContext;
import org.opendaylight.controller.mdsal.Providers;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MessageBusAppImplModule extends org.opendaylight.controller.config.yang.messagebus.app.impl.AbstractMessageBusAppImplModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageBusAppImplModule.class);

    private BundleContext bundleContext;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public MessageBusAppImplModule( ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MessageBusAppImplModule( ModuleIdentifier identifier,
                                    DependencyResolver dependencyResolver,
                                    MessageBusAppImplModule oldModule,
                                    java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {}

    @Override
    public java.lang.AutoCloseable createInstance() {
        List<NamespaceToStream> namespaceMapping = getNamespaceToStream();
        InitializationContext ic = new InitializationContext(namespaceMapping);

        final Providers.BindingAware bap = new Providers.BindingAware(ic);
        final Providers.BindingIndependent bip = new Providers.BindingIndependent(ic);

        getBindingBrokerDependency().registerProvider(bap, getBundleContext());
        getDomBrokerDependency().registerProvider(bip);

        AutoCloseable closer = new AutoCloseable() {
            @Override public void close()  {
                closeProvider(bap);
                closeProvider(bip);
            }
        };

        return closer;
    }

    private void closeProvider(AutoCloseable closable) {
        try {
            closable.close();
        } catch (Exception e) {
            LOGGER.error("Exception while closing: {}\n Exception: {}", closable, e);
        }
    }
}
