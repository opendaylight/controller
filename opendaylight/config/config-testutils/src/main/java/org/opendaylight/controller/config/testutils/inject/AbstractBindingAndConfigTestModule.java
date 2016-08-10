/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.testutils.inject;

import static org.mockito.Mockito.mock;
import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.exception;
import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import dagger.Provides;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.mockito.Mockito;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.testutils.ObjectRegistry;
import org.opendaylight.controller.config.testutils.ObjectRegistryBuilder;
import org.opendaylight.controller.config.testutils.ObjectRepositoryDependencyResolver;
import org.opendaylight.controller.config.testutils.ObjectRepositoryProviderContext;
import org.opendaylight.controller.config.testutils.ObjectRepositoryRpcProviderRegistry;
import org.opendaylight.controller.config.testutils.TestBindingAwareBroker;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.osgi.framework.BundleContext;

@dagger.Module
public abstract class AbstractBindingAndConfigTestModule {

    @Provides
    @Singleton
    ObjectRegistry.Builder objectRegistryBuilder(DataBroker dataBroker) {
        ObjectRegistryBuilder builder = new ObjectRegistryBuilder();
        builder.putInstance(dataBroker, DataBroker.class);
        return builder;
    }

    @Provides
    @Singleton
    ObjectRegistry objectRegistry(ObjectRegistry.Builder registryBuilder) {
        return registryBuilder.build();
    }

    @Provides
    @Singleton
    BindingAwareBroker bindingAwareBroker(ObjectRegistry.Builder registryBuilder) {
        TestBindingAwareBroker bindingAwareBroker = mock(TestBindingAwareBroker.class, realOrException());
        registryBuilder.putInstance(bindingAwareBroker, BindingAwareBroker.class);
        return bindingAwareBroker;
    }

    @Provides
    @Singleton
    RpcProviderRegistry rpcProviderRegistry(Provider<ObjectRegistry> objectRepositoryProvider,
            ObjectRegistry.Builder registryBuilder) {
        ObjectRepositoryRpcProviderRegistry rpcProviderRegistry = Mockito
                .mock(ObjectRepositoryRpcProviderRegistry.class, realOrException());
        rpcProviderRegistry.setObjectRegistryProvider(objectRepositoryProvider);
        registryBuilder.putInstance(rpcProviderRegistry, RpcProviderRegistry.class);
        return rpcProviderRegistry;
    }

    @Provides
    @Singleton
    DependencyResolver dependencyResolver(Provider<ObjectRegistry> objectRepositoryProvider) {
        ObjectRepositoryDependencyResolver dependencyResolver = mock(ObjectRepositoryDependencyResolver.class, realOrException());
        dependencyResolver.setObjectRegistryProvider(objectRepositoryProvider);
        return dependencyResolver;
    }

    @Provides
    @Singleton
    ProviderContext providerContext(Provider<ObjectRegistry> objectRepositoryProvider) {
        ObjectRepositoryProviderContext sessionProviderContext = mock(ObjectRepositoryProviderContext.class, realOrException());
        sessionProviderContext.setObjectRegistryProvider(objectRepositoryProvider);
        return sessionProviderContext;
    }

    // The registryBuilder & bindingAwareBroker ARE dependencies of the Module
    // (Instance), just not explicit, but dynamic. By listing them here anyway,
    // even though not used in the body, DI engine can figure out requirements
    // and correct ordering.

    @Provides
    @Singleton
    Module module(ModuleFactory moduleFactory, DependencyResolver dependencyResolver,
            BindingAwareBroker bindingAwareBroker) {
        BundleContext bundleContext = mock(BundleContext.class, exception());
        return moduleFactory.createModule("TEST", dependencyResolver, bundleContext);
    }

    @Provides
    @Singleton
    AutoCloseable moduleInstance(Module module, ProviderContext providerContext,
            RpcProviderRegistry rpcProviderRegistry) {
        AutoCloseable moduleInstance = module.getInstance();
        BindingAwareProvider serviceProviderAsBindingAwareProvider = (BindingAwareProvider) moduleInstance;
        serviceProviderAsBindingAwareProvider.onSessionInitiated(providerContext);
        return moduleInstance;
    }

}
