/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.model.YangTextSourceProvider;
import org.opendaylight.controller.sal.dom.broker.GlobalBundleScanningSchemaServiceImpl;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated Replaced by blueprint wiring
 */
@Deprecated
public final class SchemaServiceImplSingletonModule extends
org.opendaylight.controller.config.yang.md.sal.dom.impl.AbstractSchemaServiceImplSingletonModule {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaServiceImplSingletonModule.class);

    BundleContext bundleContext;

    public SchemaServiceImplSingletonModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SchemaServiceImplSingletonModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final SchemaServiceImplSingletonModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public boolean canReuseInstance(final AbstractSchemaServiceImplSingletonModule oldModule) {
        return true;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public void validate() {
        super.validate();
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new GlobalSchemaServiceProxy(GlobalBundleScanningSchemaServiceImpl.getInstance());
    }

    private static class GlobalSchemaServiceProxy implements AutoCloseable, SchemaService, YangTextSourceProvider,
            Delegator<SchemaService> {
        private final GlobalBundleScanningSchemaServiceImpl delegate;

        public GlobalSchemaServiceProxy(GlobalBundleScanningSchemaServiceImpl service) {
            this.delegate = service;
        }

        @Override
        public void close() {
            // Intentional noop as the life-cycle is controlled via blueprint.
        }

        @Override
        public void addModule(final Module arg0) {
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
        public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(final SchemaContextListener arg0) {
            return delegate.registerSchemaContextListener(arg0);
        }

        @Override
        public void removeModule(final Module arg0) {
            delegate.removeModule(arg0);
        }

        @Override
        public SchemaService getDelegate() {
            return delegate;
        }

        @Override
        public CheckedFuture<? extends YangTextSchemaSource, SchemaSourceException> getSource(
                SourceIdentifier sourceIdentifier) {
            return delegate.getSource(sourceIdentifier);
        }
    }
}
