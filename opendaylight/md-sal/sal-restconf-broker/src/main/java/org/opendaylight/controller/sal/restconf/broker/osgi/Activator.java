/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker.osgi;

import java.util.Hashtable;
import javassist.ClassPool;
import org.opendaylight.yangtools.sal.binding.generator.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private ServiceRegistration<BindingIndependentMappingService> mappingReg;

    @Override
    public void start(BundleContext context) throws Exception {
        RuntimeGeneratedMappingServiceImpl service = new RuntimeGeneratedMappingServiceImpl();
        service.setPool(new ClassPool());
        service.init();
        startRuntimeMappingService(service, context);
    }

    private void startRuntimeMappingService(RuntimeGeneratedMappingServiceImpl service, BundleContext context) {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        mappingReg = context.registerService(BindingIndependentMappingService.class, service, properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if(mappingReg != null) {
            mappingReg.unregister();
        }
    }
}
