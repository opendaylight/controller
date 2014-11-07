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
import javassist.ClassPool;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
*
*/
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
        final BindingIndependentMappingService legacyMapping = getGlobalLegacyMappingService(classLoading);
        BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(SingletonHolder.JAVASSIST));
        BindingToNormalizedNodeCodec instance = new BindingToNormalizedNodeCodec(classLoading, legacyMapping, codecRegistry);
        bundleContext.registerService(SchemaContextListener.class, instance, new Hashtable<String,String>());
        return instance;
    }

    private BindingIndependentMappingService getGlobalLegacyMappingService(final GeneratedClassLoadingStrategy classLoading) {
        BindingIndependentMappingService potential = tryToReuseGlobalMappingServiceInstance();
        if(potential == null) {
            potential =  new RuntimeGeneratedMappingServiceImpl(ClassPool.getDefault(),classLoading);
            bundleContext.registerService(SchemaContextListener.class, (SchemaContextListener) potential, new Hashtable<String,String>());
        }
        return potential;
    }

    private GeneratedClassLoadingStrategy getGlobalClassLoadingStrategy() {
        ServiceReference<GeneratedClassLoadingStrategy> ref = bundleContext.getServiceReference(GeneratedClassLoadingStrategy.class);
        return bundleContext.getService(ref);
    }

    private BindingIndependentMappingService tryToReuseGlobalMappingServiceInstance() {
        ServiceReference<BindingIndependentMappingService> serviceRef = getBundleContext().getServiceReference(BindingIndependentMappingService.class);
        if(serviceRef == null) {
            return null;
        }
        return bundleContext.getService(serviceRef);

    }

    private BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
