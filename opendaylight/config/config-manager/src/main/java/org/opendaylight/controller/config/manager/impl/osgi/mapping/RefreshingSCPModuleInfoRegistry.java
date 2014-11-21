/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.impl.osgi.mapping;

import java.util.Hashtable;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.sal.binding.generator.api.ModuleInfoRegistry;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Update SchemaContext service in Service Registry each time new YangModuleInfo is added or removed.
 */
public class RefreshingSCPModuleInfoRegistry implements ModuleInfoRegistry, AutoCloseable {

    private final ModuleInfoRegistry moduleInfoRegistry;
    private final ServiceRegistration<SchemaContextProvider> osgiReg;

    public RefreshingSCPModuleInfoRegistry(ModuleInfoRegistry moduleInfoRegistry,
                                           SchemaContextProvider schemaContextProvider, BundleContext bundleContext) {
        this.moduleInfoRegistry = moduleInfoRegistry;
        osgiReg = bundleContext.registerService(SchemaContextProvider.class, schemaContextProvider, new Hashtable<String, String>());
    }

    private void updateService() {
        osgiReg.setProperties(null); // send modifiedService event
    }

    @Override
    public ObjectRegistration<YangModuleInfo> registerModuleInfo(YangModuleInfo yangModuleInfo) {
        ObjectRegistration<YangModuleInfo> yangModuleInfoObjectRegistration = moduleInfoRegistry.registerModuleInfo(yangModuleInfo);
        ObjectRegistrationWrapper wrapper = new ObjectRegistrationWrapper(yangModuleInfoObjectRegistration);
        updateService();
        return wrapper;
    }


    @Override
    public void close() {
        osgiReg.unregister();
    }

    private class ObjectRegistrationWrapper implements ObjectRegistration<YangModuleInfo> {
        private final ObjectRegistration<YangModuleInfo> inner;

        private ObjectRegistrationWrapper(ObjectRegistration<YangModuleInfo> inner) {
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
