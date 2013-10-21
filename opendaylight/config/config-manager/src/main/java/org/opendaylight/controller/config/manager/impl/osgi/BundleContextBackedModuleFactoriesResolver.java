/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opendaylight.controller.config.manager.impl.factoriesresolver.ModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Retrieves list of currently registered Module Factories using bundlecontext.
 */
public class BundleContextBackedModuleFactoriesResolver implements
        ModuleFactoriesResolver {
    private final BundleContext bundleContext;

    public BundleContextBackedModuleFactoriesResolver(
            BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public List<ModuleFactory> getAllFactories() {
        Collection<ServiceReference<ModuleFactory>> serviceReferences;
        try {
            serviceReferences = bundleContext.getServiceReferences(
                    ModuleFactory.class, null);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e);
        }
        List<ModuleFactory> result = new ArrayList<>(serviceReferences.size());
        for (ServiceReference<ModuleFactory> serviceReference : serviceReferences) {
            ModuleFactory service = bundleContext.getService(serviceReference);
            // null if the service is not registered, the service object
            // returned by a ServiceFactory does not
            // implement the classes under which it was registered or the
            // ServiceFactory threw an exception.
            if (service != null) {
                result.add(service);
            }
        }
        return result;
    }
}
