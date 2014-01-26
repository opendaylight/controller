/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import java.util.Collections;
import java.util.Set;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.spi.Module;
import org.osgi.framework.BundleContext;

/**
*
*/
public class SchemaServiceImplSingletonModuleFactory extends
        org.opendaylight.controller.config.yang.md.sal.dom.impl.AbstractSchemaServiceImplSingletonModuleFactory {

    private static final ModuleIdentifier IDENTIFIER = new ModuleIdentifier(NAME, "yang-schema-service");
    public static SchemaServiceImplSingletonModule SINGLETON;

    @Override
    public Module createModule(String instanceName, DependencyResolver dependencyResolver, BundleContext bundleContext) {
        throw new UnsupportedOperationException("Only default instance supported.");
    }

    @Override
    public Set<SchemaServiceImplSingletonModule> getDefaultModules(DependencyResolverFactory dependencyResolverFactory,
            BundleContext bundleContext) {
        DependencyResolver dependencyResolver = dependencyResolverFactory.createDependencyResolver(IDENTIFIER);

        if (SINGLETON == null) {
            SINGLETON = new SchemaServiceImplSingletonModule(IDENTIFIER, dependencyResolver);
            SINGLETON.setBundleContext(bundleContext);
        }
        return Collections.singleton(SINGLETON);
    }
}
