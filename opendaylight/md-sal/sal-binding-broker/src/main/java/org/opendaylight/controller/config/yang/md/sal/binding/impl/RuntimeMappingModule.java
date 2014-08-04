/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;
import javassist.ClassPool;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*
*/
public final class RuntimeMappingModule extends
        org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractRuntimeMappingModule {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeMappingModule.class);

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
        BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(new StreamWriterGenerator(SingletonHolder.JAVASSIST));
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

    private static final class RuntimeGeneratedMappingServiceProxy implements //
    BindingIndependentMappingService, //
    Delegator<BindingIndependentMappingService>, //
    AutoCloseable {

        private BindingIndependentMappingService delegate;
        private ServiceReference<BindingIndependentMappingService> reference;
        private BundleContext bundleContext;

        public RuntimeGeneratedMappingServiceProxy(final BundleContext bundleContext,
                final ServiceReference<BindingIndependentMappingService> serviceRef,
                final BindingIndependentMappingService delegate) {
            this.bundleContext = Preconditions.checkNotNull(bundleContext);
            this.reference = Preconditions.checkNotNull(serviceRef);
            this.delegate = Preconditions.checkNotNull(delegate);
        }

        @Override
        public CodecRegistry getCodecRegistry() {
            return delegate.getCodecRegistry();
        }

        @Override
        public CompositeNode toDataDom(final DataObject data) {
            return delegate.toDataDom(data);
        }

        @Override
        public Entry<YangInstanceIdentifier, CompositeNode> toDataDom(
                final Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject> entry) {
            return delegate.toDataDom(entry);
        }

        @Override
        public YangInstanceIdentifier toDataDom(
                final org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> path) {
            return delegate.toDataDom(path);
        }

        @Override
        public DataObject dataObjectFromDataDom(
                final org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> path,
                final CompositeNode result) throws DeserializationException {
            return delegate.dataObjectFromDataDom(path, result);
        }

        @Override
        public org.opendaylight.yangtools.yang.binding.InstanceIdentifier<?> fromDataDom(final YangInstanceIdentifier entry)
                throws DeserializationException {
            return delegate.fromDataDom(entry);
        }

        @Override
        public Set<QName> getRpcQNamesFor(final Class<? extends RpcService> service) {
            return delegate.getRpcQNamesFor(service);
        }

        @Override
        public Optional<Class<? extends RpcService>> getRpcServiceClassFor(final String namespace, final String revision) {
            return delegate.getRpcServiceClassFor(namespace,revision);
        }

        @Override
        public DataContainer dataObjectFromDataDom(final Class<? extends DataContainer> inputClass, final CompositeNode domInput) {
            return delegate.dataObjectFromDataDom(inputClass, domInput);
        }

        @Override
        public void close() {
            if(delegate != null) {
                delegate = null;

                try {
                    bundleContext.ungetService(reference);
                } catch (IllegalStateException e) {
                    // Indicates the BundleContext is no longer valid which can happen normally on shutdown.
                    LOG.debug( "Error unregistering service", e );
                }

                bundleContext= null;
                reference = null;
            }
        }

        @Override
        public BindingIndependentMappingService getDelegate() {
            return delegate;
        }
    }
}
