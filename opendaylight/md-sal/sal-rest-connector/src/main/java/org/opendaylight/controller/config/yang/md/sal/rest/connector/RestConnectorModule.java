/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.rest.connector;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.rest.RestConnectorProvider;
import org.opendaylight.controller.sal.restconf.impl.RestconfProviderImpl;
import org.osgi.framework.BundleContext;


/**
 * RestConnectorModule - config-subsystem module loader for Restconf-connector
 */
public class RestConnectorModule extends org.opendaylight.controller.config.yang.md.sal.rest.connector.AbstractRestConnectorModule {

    private static RestConnectorRuntimeRegistration runtimeRegistration;
    private BundleContext bundleContext;

    /**
     * @param identifier
     * @param dependencyResolver
     */
    public RestConnectorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    /**
     * Constructor is made to hold BundleContext for {@link RestconfProviderImpl}
     *
     * @param identifier
     * @param dependencyResolver
     * @param bundleContext2
     * @param oldInstance
     * @param oldModule
     */
    public RestConnectorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final RestConnectorModule oldModule, final AutoCloseable oldInstance, final BundleContext bundleContext) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
        this.bundleContext = bundleContext;
    }

    /**
     * @param identifier
     * @param dependencyResolver
     * @param oldModule
     * @param oldInstance
     */
    public RestConnectorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final org.opendaylight.controller.config.yang.md.sal.rest.connector.RestConnectorModule oldModule,
            final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    /**
     * Constructor is made to hold BundleContext for {@link RestconfProviderImpl}
     *
     * @param identifier
     * @param dependencyResolver
     * @param bundleContext
     */
    public RestConnectorModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver,
            final BundleContext bundleContext) {
        this(identifier, dependencyResolver);
        this.bundleContext = bundleContext;
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
        Preconditions.checkArgument(bundleContext != null, "BundleContext was not properly set up!");
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // Create an instance of our provider
        // final RestconfProviderImpl restconfProvider = new RestconfProviderImpl(bundleContext, getWebsocketPort());
        final RestConnectorProvider restconfProvider = new RestConnectorProvider(bundleContext, getWebsocketPort());
        // Register it with the Broker
        getDomBrokerDependency().registerProvider(restconfProvider);

        if(runtimeRegistration != null){
            runtimeRegistration.close();
        }

        runtimeRegistration = getRootRuntimeBeanRegistratorWrapper().register(restconfProvider);

        return restconfProvider;
    }
}

