/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.impl.osgi.mapping;

import java.util.Dictionary;
import java.util.Hashtable;
import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.api.ModuleInfoRegistry;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update SchemaContext service in Service Registry each time new YangModuleInfo is added or removed.
 */
public class RefreshingSCPModuleInfoRegistry implements ModuleInfoRegistry, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(RefreshingSCPModuleInfoRegistry.class);

    private final ModuleInfoRegistry moduleInfoRegistry;
    private final SchemaContextProvider schemaContextProvider;
    private final SchemaSourceProvider<YangTextSchemaSource> sourceProvider;
    private final BindingContextProvider bindingContextProvider;
    private final ClassLoadingStrategy classLoadingStrat;

    private volatile ServiceRegistration<SchemaContextProvider> osgiReg;

    public RefreshingSCPModuleInfoRegistry(final ModuleInfoRegistry moduleInfoRegistry,
        final SchemaContextProvider schemaContextProvider, final ClassLoadingStrategy classLoadingStrat,
        final SchemaSourceProvider<YangTextSchemaSource> sourceProvider, final BindingContextProvider bindingContextProvider,
        final BundleContext bundleContext) {

        this.moduleInfoRegistry = moduleInfoRegistry;
        this.schemaContextProvider = schemaContextProvider;
        this.classLoadingStrat = classLoadingStrat;
        this.sourceProvider = sourceProvider;
        this.bindingContextProvider = bindingContextProvider;
        this.osgiReg = bundleContext
            .registerService(SchemaContextProvider.class, schemaContextProvider, new Hashtable<String, String>());
    }

    public void updateService() {
        if(this.osgiReg != null) {
            try {
                this.bindingContextProvider.update(this.classLoadingStrat, this.schemaContextProvider);

                final Dictionary<String, Object> props = new Hashtable<>();
                props.put(BindingRuntimeContext.class.getName(), this.bindingContextProvider.getBindingContext());
                props.put(SchemaSourceProvider.class.getName(), this.sourceProvider);
                // send modifiedService event
                this.osgiReg.setProperties(props);
            } catch (final RuntimeException e) {
                // The ModuleInfoBackedContext throws a RuntimeException if it can't create the schema context.
                LOG.warn("Error updating the BindingContextProvider", e);
            }
        }
    }

    @Override
    public ObjectRegistration<YangModuleInfo> registerModuleInfo(final YangModuleInfo yangModuleInfo) {
        final ObjectRegistration<YangModuleInfo> yangModuleInfoObjectRegistration = this.moduleInfoRegistry.registerModuleInfo(yangModuleInfo);
        return new ObjectRegistrationWrapper(yangModuleInfoObjectRegistration);
    }

    @Override
    public void close() throws Exception {
        if(this.osgiReg != null) {
            this.osgiReg.unregister();
        }

        this.osgiReg = null;
    }

    private class ObjectRegistrationWrapper implements ObjectRegistration<YangModuleInfo> {
        private final ObjectRegistration<YangModuleInfo> inner;

        private ObjectRegistrationWrapper(final ObjectRegistration<YangModuleInfo> inner) {
            this.inner = inner;
        }

        @Override
        public YangModuleInfo getInstance() {
            return this.inner.getInstance();
        }

        @Override
        public void close() throws Exception {
            this.inner.close();
            // send modify event when a bundle disappears
            updateService();
        }

        @Override
        public String toString() {
            return this.inner.toString();
        }
    }
}
