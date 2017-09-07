/*
 * Copyright (c) 2013, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import com.google.common.base.Preconditions;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.manager.impl.util.InterfacesHelper;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;

/**
 * Creates new modules by reflection. Provided class must have this constructor:
 * ctor(DynamicMBeanWithInstance.class, ModuleIdentifier.class). When
 * reconfiguring, both parameters will be non null. When creating new instance
 * first parameter will be null.
 *
 */
public class ClassBasedModuleFactory implements ModuleFactory {
    private final String implementationName;
    private final Class<? extends Module> configBeanClass;

    /**
     * Module factory constructor.
     *
     * @param implementationName
     *            name of the implementation
     * @param configBeanClass
     *            class that will be instantiated when createModule is called. This
     *            class must implement Module interface and all exported interfaces.
     */
    public ClassBasedModuleFactory(final String implementationName, final Class<? extends Module> configBeanClass) {
        this.implementationName = implementationName;
        this.configBeanClass = configBeanClass;
    }

    @Override
    public String getImplementationName() {
        return implementationName;
    }

    @Override
    public Module createModule(final String instanceName, final DependencyResolver dependencyResolver,
            final DynamicMBeanWithInstance old, final BundleContext bundleContext) throws Exception {
        Preconditions.checkNotNull(old);
        return constructModule(instanceName, dependencyResolver, old);
    }

    @Override
    public Module createModule(final String instanceName, final DependencyResolver dependencyResolver,
            final BundleContext bundleContext) {
        try {
            return constructModule(instanceName, dependencyResolver, null);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private Module constructModule(final String instanceName, final DependencyResolver dependencyResolver,
            final DynamicMBeanWithInstance old)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Preconditions.checkNotNull(dependencyResolver);
        ModuleIdentifier moduleIdentifier = new ModuleIdentifier(implementationName, instanceName);
        Constructor<? extends Module> declaredConstructor;
        try {
            declaredConstructor = configBeanClass.getDeclaredConstructor(DynamicMBeanWithInstance.class,
                    ModuleIdentifier.class);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Did not find constructor with parameters (DynamicMBeanWithInstance) in " + configBeanClass, e);
        }
        Preconditions.checkState(declaredConstructor != null);
        return declaredConstructor.newInstance(old, moduleIdentifier);
    }

    @Override
    public boolean isModuleImplementingServiceInterface(
            final Class<? extends AbstractServiceInterface> serviceInterface) {
        Class<?>[] classes = configBeanClass.getInterfaces();
        List<Class<?>> ifc = Arrays.asList(classes);
        if (ifc.contains(serviceInterface)) {
            return true;
        }
        for (Class<?> c : classes) {
            ifc = Arrays.asList(c.getInterfaces());
            if (ifc.contains(serviceInterface)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Module> getDefaultModules(final DependencyResolverFactory dependencyResolverFactory,
            final BundleContext bundleContext) {
        return new HashSet<>();
    }

    @Override
    public Set<Class<? extends AbstractServiceInterface>> getImplementedServiceIntefaces() {
        return InterfacesHelper.getAllAbstractServiceClasses(configBeanClass);
    }
}
