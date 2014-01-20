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
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.sal.binding.generator.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
*
*/
public final class RuntimeMappingModule extends
        org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractRuntimeMappingModule {

    private BundleContext bundleContext;

    public RuntimeMappingModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public RuntimeMappingModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            RuntimeMappingModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkNotNull(bundleContext);
        // Add custom validation for module attributes here.
    }

    @Override
    public boolean canReuseInstance(AbstractRuntimeMappingModule oldModule) {
        return true;
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        
        RuntimeGeneratedMappingServiceProxy potential = tryToReuseGlobalInstance();
        if(potential != null) {
            return potential;
        }
        RuntimeGeneratedMappingServiceImpl service = new RuntimeGeneratedMappingServiceImpl();
        service.setPool(SingletonHolder.CLASS_POOL);
        service.init();
        bundleContext.registerService(SchemaServiceListener.class, service, new Hashtable<String,String>());
        return service;
    }

    private RuntimeGeneratedMappingServiceProxy tryToReuseGlobalInstance() {
        ServiceReference<BindingIndependentMappingService> serviceRef = getBundleContext().getServiceReference(BindingIndependentMappingService.class);
        if(serviceRef == null) {
            return null;
        }

        BindingIndependentMappingService delegate = bundleContext.getService(serviceRef);
        if (delegate == null) {
            return null;
        }
        return new RuntimeGeneratedMappingServiceProxy(getBundleContext(),serviceRef,delegate);
    }

    private BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private static final class RuntimeGeneratedMappingServiceProxy implements //
    BindingIndependentMappingService, //
    Delegator<BindingIndependentMappingService>, //
    AutoCloseable {
        
        private BindingIndependentMappingService delegate;
        private ServiceReference<BindingIndependentMappingService> reference;
        private BundleContext bundleContext;

        public RuntimeGeneratedMappingServiceProxy(BundleContext bundleContext,
                ServiceReference<BindingIndependentMappingService> serviceRef,
                BindingIndependentMappingService delegate) {
            this.bundleContext = Preconditions.checkNotNull(bundleContext);
            this.reference = Preconditions.checkNotNull(serviceRef);
            this.delegate = Preconditions.checkNotNull(delegate);
        }

        public CodecRegistry getCodecRegistry() {
            return delegate.getCodecRegistry();
        }

        public CompositeNode toDataDom(DataObject data) {
            return delegate.toDataDom(data);
        }

        public Entry<InstanceIdentifier, CompositeNode> toDataDom(
                Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject> entry) {
            return delegate.toDataDom(entry);
        }

        public InstanceIdentifier toDataDom(
                org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> path) {
            return delegate.toDataDom(path);
        }

        public DataObject dataObjectFromDataDom(
                org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> path,
                CompositeNode result) throws DeserializationException {
            return delegate.dataObjectFromDataDom(path, result);
        }

        public org.opendaylight.yangtools.yang.binding.InstanceIdentifier<?> fromDataDom(InstanceIdentifier entry)
                throws DeserializationException {
            return delegate.fromDataDom(entry);
        }

        public Set<QName> getRpcQNamesFor(Class<? extends RpcService> service) {
            return delegate.getRpcQNamesFor(service);
        }

        @Override
        public Optional<Class<? extends RpcService>> getRpcServiceClassFor(String namespace, String revision) {
            return delegate.getRpcServiceClassFor(namespace,revision);
        }

        public DataContainer dataObjectFromDataDom(Class<? extends DataContainer> inputClass, CompositeNode domInput) {
            return delegate.dataObjectFromDataDom(inputClass, domInput);
        }
        
        @Override
        public void close() throws Exception {
            if(delegate != null) {
                delegate = null;
                bundleContext.ungetService(reference);
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
