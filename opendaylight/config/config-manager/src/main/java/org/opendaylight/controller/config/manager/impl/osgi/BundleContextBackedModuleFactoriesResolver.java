/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.ModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieves list of currently registered Module Factories using bundlecontext.
 */
public class BundleContextBackedModuleFactoriesResolver implements
        ModuleFactoriesResolver {
    private static final Logger LOG = LoggerFactory
            .getLogger(BundleContextBackedModuleFactoriesResolver.class);
    private final BundleContext bundleContext;

    public BundleContextBackedModuleFactoriesResolver(
            BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public Map<String, Map.Entry<ModuleFactory, BundleContext>> getAllFactories() {
        Collection<ServiceReference<ModuleFactory>> serviceReferences;
        try {
            serviceReferences = bundleContext.getServiceReferences(
                    ModuleFactory.class, null);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e);
        }
        Map<String, Map.Entry<ModuleFactory, BundleContext>> result = new HashMap<>(serviceReferences.size());
        for (ServiceReference<ModuleFactory> serviceReference : serviceReferences) {
            ModuleFactory factory = bundleContext.getService(serviceReference);
            // null if the service is not registered, the service object
            // returned by a ServiceFactory does not
            // implement the classes under which it was registered or the
            // ServiceFactory threw an exception.
            if(factory == null) {
                throw new NullPointerException("ServiceReference of class" + serviceReference.getClass() + "not found.");
            }

            String moduleName = factory.getImplementationName();
            if (moduleName == null || moduleName.isEmpty()) {
                throw new IllegalStateException(
                        "Invalid implementation name for " + factory);
            }
            if (serviceReference.getBundle() == null || serviceReference.getBundle().getBundleContext() == null) {
                throw new NullPointerException("Bundle context of " + factory + " ModuleFactory not found.");
            }
            LOG.debug("Reading factory {} {}", moduleName, factory);

            Map.Entry<ModuleFactory, BundleContext> conflicting = result.get(moduleName);
            if (conflicting != null) {
                String error = String
                        .format("Module name is not unique. Found two conflicting factories with same name '%s': '%s' '%s'",
                                moduleName, conflicting.getKey(), factory);
                LOG.error(error);
                throw new IllegalArgumentException(error);
            } else {
                result.put(moduleName, new AbstractMap.SimpleImmutableEntry<>(factory,
                        serviceReference.getBundle().getBundleContext()));
            }
        }
        return result;
    }
}
