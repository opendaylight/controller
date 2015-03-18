/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.impl.osgi.mapping;

import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.api.ModuleInfoRegistry;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;

/**
 * Update SchemaContext service in Service Registry each time new YangModuleInfo is added or removed.
 */
public class RefreshingSCPModuleInfoRegistry implements ModuleInfoRegistry {

    private final ModuleInfoRegistry moduleInfoRegistry;
    private final SchemaContextProvider schemaContextProvider;
    private final BindingContextProvider bindingContextProvider;
    private final ClassLoadingStrategy classLoadingStrat;

    public RefreshingSCPModuleInfoRegistry(final ModuleInfoRegistry moduleInfoRegistry,
                                           final SchemaContextProvider schemaContextProvider, final ClassLoadingStrategy classLoadingStrat, final BindingContextProvider bindingContextProvider) {
        this.moduleInfoRegistry = moduleInfoRegistry;
        this.schemaContextProvider = schemaContextProvider;
        this.classLoadingStrat = classLoadingStrat;
        this.bindingContextProvider = bindingContextProvider;
    }

    private void updateService() {
        bindingContextProvider.update(classLoadingStrat, schemaContextProvider);
    }

    @Override
    public ObjectRegistration<YangModuleInfo> registerModuleInfo(YangModuleInfo yangModuleInfo) {
        ObjectRegistration<YangModuleInfo> yangModuleInfoObjectRegistration = moduleInfoRegistry.registerModuleInfo(yangModuleInfo);
        ObjectRegistrationWrapper wrapper = new ObjectRegistrationWrapper(yangModuleInfoObjectRegistration);
        updateService();
        return wrapper;
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
