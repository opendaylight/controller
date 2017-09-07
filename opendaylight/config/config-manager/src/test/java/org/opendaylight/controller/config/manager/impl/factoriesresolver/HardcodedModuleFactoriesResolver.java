/*
 * Copyright (c) 2013, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.factoriesresolver;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;

public class HardcodedModuleFactoriesResolver implements ModuleFactoriesResolver {
    private final Map<String, Map.Entry<ModuleFactory, BundleContext>> factories;

    public HardcodedModuleFactoriesResolver(final BundleContext bundleContext, final ModuleFactory... list) {
        this.factories = new HashMap<>(list.length);
        for (ModuleFactory moduleFactory : list) {
            String moduleName = moduleFactory.getImplementationName();
            if (moduleName == null || moduleName.isEmpty()) {
                throw new IllegalStateException("Invalid implementation name for " + moduleFactory);
            }
            Map.Entry<ModuleFactory, BundleContext> conflicting = factories.get(moduleName);
            if (conflicting == null) {
                factories.put(moduleName, new AbstractMap.SimpleEntry<>(moduleFactory, bundleContext));
            } else {
                throw new IllegalArgumentException(String.format(
                        "Module name is not unique. Found two conflicting factories with same name '%s':\n\t%s\n\t%s\n",
                        moduleName, conflicting.getKey(), moduleFactory));
            }
        }
    }

    @Override
    public Map<String, Map.Entry<ModuleFactory, BundleContext>> getAllFactories() {
        return factories;
    }
}
