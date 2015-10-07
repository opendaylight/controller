/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.factoriesresolver;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.opendaylight.controller.config.api.ModuleFactoryNotFoundException;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;

/**
 * Hold sorted ConfigMBeanFactories by their module names. Check that module
 * names are globally unique.
 */
public class HierarchicalConfigMBeanFactoriesHolder {

    private final Map<String, Map.Entry<ModuleFactory, BundleContext>> moduleNamesToConfigBeanFactories;
    private final Set<String> moduleNames;
    private final List<ModuleFactory> moduleFactories;

    /**
     * Create instance.
     *
     * @throws IllegalArgumentException
     *             if unique constraint on module names is violated
     */
    public HierarchicalConfigMBeanFactoriesHolder(
            Map<String, Map.Entry<ModuleFactory, BundleContext>> factoriesMap) {
        this.moduleNamesToConfigBeanFactories = Collections
                .unmodifiableMap(factoriesMap);
        moduleNames = Collections.unmodifiableSet(new TreeSet<>(
                moduleNamesToConfigBeanFactories.keySet()));
        List<ModuleFactory> factories = new ArrayList<>(this.moduleNamesToConfigBeanFactories.size());
        Collection<Map.Entry<ModuleFactory, BundleContext>> entryCollection = this.moduleNamesToConfigBeanFactories.values();
        for (Map.Entry<ModuleFactory, BundleContext> entry : entryCollection) {
            factories.add(entry.getKey());
        }
        this.moduleFactories = Collections.unmodifiableList(factories);
    }

    /**
     * Get ModuleFactory by their name.
     *
     * @throws IllegalArgumentException
     *             if factory is not found
     */
    public ModuleFactory findByModuleName(String moduleName) throws ModuleFactoryNotFoundException {
        Map.Entry<ModuleFactory, BundleContext> result = moduleNamesToConfigBeanFactories.get(moduleName);
        if (result == null) {
            throw new ModuleFactoryNotFoundException(moduleName);
        }
        return result.getKey();
    }

    public Set<String> getModuleNames() {
        return moduleNames;
    }

    public List<ModuleFactory> getModuleFactories() {
        return moduleFactories;
    }

}
