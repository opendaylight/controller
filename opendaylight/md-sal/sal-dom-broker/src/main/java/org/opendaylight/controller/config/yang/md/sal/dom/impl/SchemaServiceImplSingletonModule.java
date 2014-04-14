/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.dom.broker.GlobalBundleScanningSchemaServiceImpl;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
*
*/
public final class SchemaServiceImplSingletonModule extends
        org.opendaylight.controller.config.yang.md.sal.dom.impl.AbstractSchemaServiceImplSingletonModule {

    BundleContext bundleContext;

    public SchemaServiceImplSingletonModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SchemaServiceImplSingletonModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            SchemaServiceImplSingletonModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public boolean canReuseInstance(AbstractSchemaServiceImplSingletonModule oldModule) {
        return true;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public void validate() {
        super.validate();
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        ServiceReference<SchemaService> ref = getBundleContext().getServiceReference(SchemaService.class);
        if (ref != null) {
            return new GlobalSchemaServiceProxy(getBundleContext(), ref);
        }

        GlobalBundleScanningSchemaServiceImpl newInstance = new GlobalBundleScanningSchemaServiceImpl();
        newInstance.setContext(getBundleContext());
        newInstance.start();
        return newInstance;
    }

    public class GlobalSchemaServiceProxy implements AutoCloseable, SchemaService, Delegator<SchemaService> {

        private BundleContext bundleContext;
        private ServiceReference<SchemaService> reference;
        private SchemaService delegate;

        public GlobalSchemaServiceProxy(BundleContext bundleContext, ServiceReference<SchemaService> ref) {
            this.bundleContext = bundleContext;
            this.reference = ref;
            this.delegate = bundleContext.getService(reference);
        }

        @Override
        public void close() throws Exception {
            if (delegate != null) {
                delegate = null;

                try {
                    bundleContext.ungetService(reference);
                } catch (IllegalStateException e) {
                    // Ignore - this means the service was already unregistred which can happen normally
                    // on shutdown.
                }

                reference = null;
                bundleContext = null;
            }
        }

        @Override
        public void addModule(Module arg0) {
            delegate.addModule(arg0);
        }

        @Override
        public SchemaContext getGlobalContext() {
            return delegate.getGlobalContext();
        }

        @Override
        public SchemaContext getSessionContext() {
            return delegate.getSessionContext();
        }

        @Override
        public ListenerRegistration<SchemaServiceListener> registerSchemaServiceListener(SchemaServiceListener arg0) {
            return delegate.registerSchemaServiceListener(arg0);
        }

        @Override
        public void removeModule(Module arg0) {
            delegate.removeModule(arg0);
        }

        @Override
        public SchemaService getDelegate() {
            return delegate;
        }

    }
}
