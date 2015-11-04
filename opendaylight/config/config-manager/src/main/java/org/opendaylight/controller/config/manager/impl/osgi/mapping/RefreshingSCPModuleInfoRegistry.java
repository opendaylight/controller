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
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.api.ModuleInfoRegistry;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
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
        osgiReg = bundleContext
            .registerService(SchemaContextProvider.class, schemaContextProvider, new Hashtable<String, String>());
    }

    public void updateService() {
        if(osgiReg != null) {
            try {
                bindingContextProvider.update(classLoadingStrat, schemaContextProvider);

                final Dictionary<String, Object> props = new Hashtable<>();
                props.put(BindingRuntimeContext.class.getName(), bindingContextProvider.getBindingContext());
                props.put(SchemaSourceProvider.class.getName(), sourceProvider);
                osgiReg.setProperties(props); // send modifiedService event
            } catch (RuntimeException e) {
                // The ModuleInfoBackedContext throws a RuntimeException if it can't create the schema context.
                LOG.warn("Error updating the BindingContextProvider", e);
            }
        }
    }

    @Override
    public ObjectRegistration<YangModuleInfo> registerModuleInfo(final YangModuleInfo yangModuleInfo) {
        ObjectRegistration<YangModuleInfo> yangModuleInfoObjectRegistration = moduleInfoRegistry.registerModuleInfo(yangModuleInfo);
        ObjectRegistrationWrapper wrapper = new ObjectRegistrationWrapper(yangModuleInfoObjectRegistration);
        return wrapper;
    }

    @Override
    public void close() throws Exception {
        if(osgiReg != null) {
            osgiReg.unregister();
        }

        osgiReg = null;
    }

    private class ObjectRegistrationWrapper implements ObjectRegistration<YangModuleInfo> {
        private final ObjectRegistration<YangModuleInfo> inner;

        private ObjectRegistrationWrapper(final ObjectRegistration<YangModuleInfo> inner) {
            this.inner = inner;
        }

        @Override
        public YangModuleInfo getInstance() {
            return inner.getInstance();
        }

        @Override
        public void close() throws Exception {
            inner.close();
            updateService();// send modify event when a bundle disappears
        }

        @Override
        public String toString() {
            return inner.toString();
        }
    }
}
