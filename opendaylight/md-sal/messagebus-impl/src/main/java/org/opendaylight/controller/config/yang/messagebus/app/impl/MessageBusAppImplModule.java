/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.messagebus.app.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.mdsal.ioc.DataStore;
import org.opendaylight.controller.mdsal.ioc.MdSALServiceInjector;
import org.opendaylight.controller.messagebus.app.impl.EventAggregator;
import org.opendaylight.controller.messagebus.app.impl.EventSourceManager;
import org.opendaylight.controller.messagebus.app.impl.EventSourceTopology;
import org.osgi.framework.BundleContext;

public class MessageBusAppImplModule extends org.opendaylight.controller.config.yang.messagebus.app.impl.AbstractMessageBusAppImplModule {
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
    protected void customValidation() {
        // Add custom validation for module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        Map<Class<?>, Object> injectionContext = getInjectionContext();

        final MdSALServiceInjector.BindingAware bap = new MdSALServiceInjector.BindingAware(injectionContext);
        final MdSALServiceInjector.BindingIndependent bip = new MdSALServiceInjector.BindingIndependent(injectionContext);

        getBindingBrokerDependency().registerProvider(bap, getBundleContext());
        getDomBrokerDependency().registerProvider(bip);

        AutoCloseable closer = new AutoCloseable() {
            @Override public void close() throws Exception {
                bap.close();
                bip.close();
            }
        };

        return closer;
    }

    private Map<Class<?>, Object> getInjectionContext() {
        List<Class<?>> injectableClasses = resolveSupportedServices();
        Map<Class<?>, Object> context = new HashMap<>(injectableClasses.size());

        for (Class injectableClass : injectableClasses) {
            Object injectable = null;

            try {
                injectable = injectableClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            context.put(injectableClass, injectable);
        }

        return context;
    }

    // TODO: Dummy method simulating retrieving service classes from some framework context
    private List<Class<?>> resolveSupportedServices() {
        return new ArrayList<Class<?>>() {{
            add(DataStore.class);
            add(EventSourceTopology.class);
            add(EventSourceManager.class);
            add(EventAggregator.class);
        }};
    }
}
