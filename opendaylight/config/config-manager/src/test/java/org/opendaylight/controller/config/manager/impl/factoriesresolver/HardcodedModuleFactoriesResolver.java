/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.factoriesresolver;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.Closeable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class HardcodedModuleFactoriesResolver implements ModuleFactoriesResolver {
    private Map<String, Map.Entry<ModuleFactory, BundleContext>> factories;

    public HardcodedModuleFactoriesResolver(BundleContext bundleContext, ModuleFactory... list) {
        List<ModuleFactory> factoryList = Arrays.asList(list);
        this.factories = new HashMap<>(factoryList.size());
        for (ModuleFactory moduleFactory : list) {
            StringBuffer errors = new StringBuffer();
            String moduleName = moduleFactory.getImplementationName();
            if (moduleName == null || moduleName.isEmpty()) {
                throw new IllegalStateException(
                        "Invalid implementation name for " + moduleFactory);
            }
            String error = null;
            Map.Entry<ModuleFactory, BundleContext> conflicting = factories.get(moduleName);
            if (conflicting != null) {
                error = String
                        .format("Module name is not unique. Found two conflicting factories with same name '%s': " +
                                "\n\t%s\n\t%s\n", moduleName, conflicting.getKey(), moduleFactory);

            }

            if (error == null) {
                factories.put(moduleName, new AbstractMap.SimpleEntry<>(moduleFactory,
                        bundleContext));
            } else {
                errors.append(error);
            }
            if (errors.length() > 0) {
                throw new IllegalArgumentException(errors.toString());
            }
        }
    }

    private static BundleContext mockBundleContext() {
        BundleContext bundleContext = Mockito.mock(BundleContext.class);
        ServiceRegistration<ModuleFactory> serviceRegistration = mock(ServiceRegistration.class);
        doNothing().when(serviceRegistration).unregister();
        doReturn(serviceRegistration).when(bundleContext).registerService(
                Matchers.any(String[].class), any(Closeable.class),
                any(Dictionary.class));
        return bundleContext;
    }

    @Override
    public Map<String, Map.Entry<ModuleFactory, BundleContext>> getAllFactories() {
        return factories;
    }

}
