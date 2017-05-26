/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.model.YangTextSourceProvider;
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
    public AutoCloseable createInstance() {
        final WaitingServiceTracker<SchemaService> schemaServiceTracker =
                WaitingServiceTracker.create(SchemaService.class, bundleContext);
        final SchemaService schemaService = schemaServiceTracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);

        final WaitingServiceTracker<YangTextSourceProvider> sourceProviderTracker =
                WaitingServiceTracker.create(YangTextSourceProvider.class, bundleContext);
        final YangTextSourceProvider sourceProvider = sourceProviderTracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);

        class GlobalSchemaServiceProxy implements AutoCloseable, SchemaService, YangTextSourceProvider {
            @Override
            public void close() {
                schemaServiceTracker.close();
                sourceProviderTracker.close();
            }

            @Override
            public void addModule(final Module arg0) {
                schemaService.addModule(arg0);
            }

            @Override
            public SchemaContext getGlobalContext() {
                return schemaService.getGlobalContext();
            }

            @Override
            public SchemaContext getSessionContext() {
                return schemaService.getSessionContext();
            }

            @Override
            public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(final SchemaContextListener arg0) {
                return schemaService.registerSchemaContextListener(arg0);
            }

            @Override
            public void removeModule(final Module arg0) {
                schemaService.removeModule(arg0);
            }

            @Override
            public CheckedFuture<? extends YangTextSchemaSource, SchemaSourceException> getSource(
                    SourceIdentifier sourceIdentifier) {
                return sourceProvider.getSource(sourceIdentifier);
            }
        }

        return new GlobalSchemaServiceProxy();
    }
}
