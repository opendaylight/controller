/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.util;

import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.util.Collection;

public class ModuleFactoryBundleContextHelper {

    public static BundleContext getModuleFactoryBundleContext(BundleContext bundleContext, String implementationName) {
        try {
            Collection<ServiceReference<ModuleFactory>> serviceReferences = bundleContext.getServiceReferences(ModuleFactory.class, null);
            for(ServiceReference<ModuleFactory> serviceReference : serviceReferences) {
                ModuleFactory service = bundleContext.getService(serviceReference);
                if (service != null) {
                    if(service.getImplementationName().equals(implementationName)) {
                        if (serviceReference.getBundle() != null && serviceReference.getBundle().getBundleContext() != null) {
                            return serviceReference.getBundle().getBundleContext();
                        }
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e);
        }
        throw new NullPointerException("Bundle context of " + implementationName + " ModuleFactory not found.");
    }
}
