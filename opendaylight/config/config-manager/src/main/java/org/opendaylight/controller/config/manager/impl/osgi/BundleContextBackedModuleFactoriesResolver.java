/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import com.google.common.annotations.VisibleForTesting;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.ModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieves list of currently registered Module Factories using bundlecontext.
 */
public class BundleContextBackedModuleFactoriesResolver implements
        ModuleFactoriesResolver {
    private static final Logger LOG = LoggerFactory
            .getLogger(BundleContextBackedModuleFactoriesResolver.class);
    private ModuleFactoryBundleTracker moduleFactoryBundleTracker;
    private int bundleContextTimeout = 60000;

    public BundleContextBackedModuleFactoriesResolver() {
    }

    public void setModuleFactoryBundleTracker(ModuleFactoryBundleTracker moduleFactoryBundleTracker) {
        this.moduleFactoryBundleTracker = moduleFactoryBundleTracker;
    }

    @VisibleForTesting
    public void setBundleContextTimeout(int bundleContextTimeout) {
        this.bundleContextTimeout = bundleContextTimeout;
    }

    @Override
    public Map<String, Map.Entry<ModuleFactory, BundleContext>> getAllFactories() {
        Map<String, Map.Entry<ModuleFactory, BundleContext>> result = new HashMap<>();
        for(Entry<ModuleFactory, BundleContext> entry: moduleFactoryBundleTracker.getModuleFactoryEntries()) {
            ModuleFactory factory = entry.getKey();
            BundleContext bundleContext = entry.getValue();
            String moduleName = factory .getImplementationName();
            if (moduleName == null || moduleName.isEmpty()) {
                throw new IllegalStateException("Invalid implementation name for " + factory);
            }

            LOG.debug("Processing factory {} {}", moduleName, factory);

            Map.Entry<ModuleFactory, BundleContext> conflicting = result.get(moduleName);
            if (conflicting != null) {
                String error = String
                        .format("Module name is not unique. Found two conflicting factories with same name '%s': '%s' '%s'",
                                moduleName, conflicting.getKey(), factory);
                LOG.error(error);
                throw new IllegalArgumentException(error);
            } else {
                result.put(moduleName, new AbstractMap.SimpleImmutableEntry<>(factory, bundleContext));
            }
        }

        return result;
    }
}
