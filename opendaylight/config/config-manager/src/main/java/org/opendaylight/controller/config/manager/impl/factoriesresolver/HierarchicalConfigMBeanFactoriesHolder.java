/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.factoriesresolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.opendaylight.controller.config.spi.ModuleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hold sorted ConfigMBeanFactories by their module names. Check that module
 * names are globally unique.
 */
public class HierarchicalConfigMBeanFactoriesHolder {
    private static final Logger logger = LoggerFactory
            .getLogger(HierarchicalConfigMBeanFactoriesHolder.class);

    private final Map<String, ModuleFactory> moduleNamesToConfigBeanFactories;
    private final Set<String> moduleNames;

    /**
     * Create instance.
     *
     * @throws IllegalArgumentException
     *             if unique constraint on module names is violated
     */
    public HierarchicalConfigMBeanFactoriesHolder(
            List<? extends ModuleFactory> list) {
        Map<String, ModuleFactory> moduleNamesToConfigBeanFactories = new HashMap<>();
        StringBuffer errors = new StringBuffer();
        for (ModuleFactory factory : list) {
            String moduleName = factory.getImplementationName();
            if (moduleName == null || moduleName.isEmpty()) {
                throw new IllegalStateException(
                        "Invalid implementation name for " + factory);
            }
            logger.debug("Reading factory {} {}", moduleName, factory);
            String error = null;
            ModuleFactory conflicting = moduleNamesToConfigBeanFactories
                    .get(moduleName);
            if (conflicting != null) {
                error = String
                        .format("Module name is not unique. Found two conflicting factories with same name '%s': " +
                                "\n\t%s\n\t%s\n", moduleName, conflicting, factory);

            }

            if (error == null) {
                moduleNamesToConfigBeanFactories.put(moduleName, factory);
            } else {
                errors.append(error);
            }

        }
        if (errors.length() > 0) {
            throw new IllegalArgumentException(errors.toString());
        }
        this.moduleNamesToConfigBeanFactories = Collections
                .unmodifiableMap(moduleNamesToConfigBeanFactories);
        moduleNames = Collections.unmodifiableSet(new TreeSet<>(
                moduleNamesToConfigBeanFactories.keySet()));
    }

    /**
     * Get ModuleFactory by their name.
     *
     * @throws IllegalArgumentException
     *             if factory is not found
     */
    public ModuleFactory findByModuleName(String moduleName) {
        ModuleFactory result = moduleNamesToConfigBeanFactories.get(moduleName);
        if (result == null) {
            throw new IllegalArgumentException(
                    "ModuleFactory not found with module name: " + moduleName);
        }
        return result;
    }

    public Set<String> getModuleNames() {
        return moduleNames;
    }

}
