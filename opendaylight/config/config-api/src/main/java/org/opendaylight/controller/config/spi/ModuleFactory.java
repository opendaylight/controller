/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.spi;

import java.util.Set;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.osgi.framework.BundleContext;

/**
 * Factory which creates {@link Module instances. An instance of this interface
 * needs to be exported into the OSGi Service Registry. Such an instance
 * provides metadata describing services which can be published from it.
 *
 * Each {@link Module } can optionally be instantiated with a
 * {@link DynamicMBean} which represents the configuration of the currently
 * running instance.
 */
public interface ModuleFactory {

    /**
     * Returns the human-friendly implementation name. This value needs to be
     * unique within all implementations of all interfaces returned by
     * getImplementedInterfaces().
     *
     * @return human-friendly implementation name
     */
    String getImplementationName();

    /**
     * Create a new Module instance. The returned object is expected to use the
     * dependencyResolver provided when resolving ObjectNames to actual Module
     * instances.
     *
     * @param dependencyResolver
     *            This resolver will return actual config mbean based on its
     *            ObjectName.
     * @param bundleContext Reference to OSGi bundleContext that can be used to
     *                      acquire OSGi services, startup configuration and other
     *                      OSGi related information.
     *
     * @return newly created module
     *
     */
    Module createModule(String instanceName,
            DependencyResolver dependencyResolver, BundleContext bundleContext);

    /**
     * Create a new Module instance. The returned object is expected to use the
     * dependencyResolver provided when resolving ObjectNames to actual Module
     * instances. A reference to an abstract view of the previous configuration
     * is also provided in the form of a {@link DynamicMBean}. Implementations
     * should use the MBeanInfo interface to understand the structure of the
     * configuration information.
     *
     * Structural information impacts hot-swap operations in that in order to
     * perform such a swap the newly loaded code needs to understand the
     * previously-running instance configuration layout and how to map it onto
     * itself.
     *
     * @param dependencyResolver
     *            This resolver will return actual config mbean based on its
     *            ObjectName.
     * @param old
     *            existing module from platform MBeanServer that is being
     *            reconfigured. Implementations should inspect its attributes
     *            using {@link DynamicMBean#getAttribute(String)} and set those
     *            attributes on newly created module. If reconfiguration of live
     *            instances is supported, this live instance can be retreived
     *            using
     *            {@link org.opendaylight.controller.config.api.DynamicMBeanWithInstance#getInstance()}
     *            . It is possible that casting this old instance throws
     *            {@link ClassCastException} when OSGi bundle is being updated.
     *            In this case, implementation should revert to creating new
     *            instance.
     * @param bundleContext Reference to OSGi bundleContext that can be used to
     *                      acquire OSGi services, startup configuration and other
     *                      OSGi related information.
     *
     * @return newly created module
     * @throws Exception
     *             if it is not possible to recover configuration from old. This
     *             leaves server in a running state but no configuration
     *             transaction can be created.
     */
    Module createModule(String instanceName, DependencyResolver dependencyResolver,
            DynamicMBeanWithInstance old, BundleContext bundleContext) throws Exception;

    boolean isModuleImplementingServiceInterface(
            Class<? extends AbstractServiceInterface> serviceInterface);

    Set<Class<? extends AbstractServiceInterface>> getImplementedServiceIntefaces();

    /**
     * Called when ModuleFactory is registered to config manager.
     * Useful for populating the registry with pre-existing state. Since
     * the method is called for each ModuleFactory separately and transaction
     * is committed automatically, returned modules MUST be valid and commitable
     * without any manual intervention.
     *
     * @param dependencyResolverFactory factory for getting dependency resolvers for each module.
     * @param bundleContext Reference to OSGi bundleContext that can be used to
     *                      acquire OSGi services, startup configuration and other
     *                      OSGi related information.
     *
     * @return set of default modules. Null is not allowed.
     */
    Set<? extends Module> getDefaultModules(DependencyResolverFactory dependencyResolverFactory,
            BundleContext bundleContext);

}
