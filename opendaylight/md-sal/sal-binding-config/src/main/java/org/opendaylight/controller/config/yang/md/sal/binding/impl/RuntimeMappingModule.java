/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import com.google.common.base.Preconditions;
import java.util.Hashtable;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
**/
public final class RuntimeMappingModule extends AbstractRuntimeMappingModule {

    private BundleContext bundleContext;

    public RuntimeMappingModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public RuntimeMappingModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final RuntimeMappingModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkNotNull(bundleContext);
        // Add custom validation for module attributes here.
    }

    @Override
    public boolean canReuseInstance(final AbstractRuntimeMappingModule oldModule) {
        return true;
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final GeneratedClassLoadingStrategy classLoading = getGlobalClassLoadingStrategy();
        final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(SingletonHolder.JAVASSIST));
        final BindingToNormalizedNodeCodec instance = new BindingToNormalizedNodeCodec(classLoading, codecRegistry,getWaitForSchema());
        bundleContext.registerService(SchemaContextListener.class, instance, new Hashtable<String,String>());
        return instance;
    }

    private GeneratedClassLoadingStrategy getGlobalClassLoadingStrategy() {
        final ServiceReference<GeneratedClassLoadingStrategy> ref = bundleContext.getServiceReference(GeneratedClassLoadingStrategy.class);
        return bundleContext.getService(ref);
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
