/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi.mapping;

import javassist.ClassPool;
import org.opendaylight.yangtools.sal.binding.generator.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Hashtable;

public class RuntimeGeneratedMappingServiceActivator implements AutoCloseable {

    private static final ClassPool CLASS_POOL = ClassPool.getDefault();

    private ServiceRegistration<SchemaServiceListener> listenerReg;
    private ServiceRegistration<BindingIndependentMappingService> mappingReg;
    private ModuleInfoBundleTracker moduleInfoBundleTracker;

    public RuntimeGeneratedMappingServiceActivator(ModuleInfoBundleTracker moduleInfoBundleTracker) {
        this.moduleInfoBundleTracker = moduleInfoBundleTracker;
    }

    public RuntimeGeneratedMappingServiceImpl startRuntimeMappingService(BundleContext context) {
        RuntimeGeneratedMappingServiceImpl service = new RuntimeGeneratedMappingServiceImpl(moduleInfoBundleTracker.getModuleInfoLoadingStrategy());
        service.setPool(CLASS_POOL);
        service.init();
        startRuntimeMappingService(service, context);
        return service;
    }

    private void startRuntimeMappingService(RuntimeGeneratedMappingServiceImpl service, BundleContext context) {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        listenerReg = context.registerService(SchemaServiceListener.class, service, properties);
        mappingReg = context.registerService(BindingIndependentMappingService.class, service, properties);

    }

    @Override
    public void close() throws Exception {
        mappingReg.unregister();
        listenerReg.unregister();
    }
}
