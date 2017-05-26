/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import javax.annotation.Nullable;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.impl.dependencyresolver.DestroyedModule;
import org.opendaylight.controller.config.manager.impl.dynamicmbean.DynamicReadableWrapper;
import org.opendaylight.controller.config.manager.impl.jmx.ModuleJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.RootRuntimeBeanRegistratorImpl;
import org.opendaylight.controller.config.manager.impl.osgi.BeanToOsgiServiceManager.OsgiRegistration;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.osgi.framework.BundleContext;

/**
 * Provides metadata about Module from controller to registry.
 */
public class ModuleInternalInfo implements Comparable<ModuleInternalInfo>,
                Identifiable<ModuleIdentifier>{

    private final ModuleIdentifier name;
    // this registrator is passed to runtime bean registrator and config
    // registry to register read only module.
    // writable modules are registered using TransactionJMXRegistrator
    @Nullable
    private final DynamicReadableWrapper readableModule;

    private final RootRuntimeBeanRegistratorImpl runtimeBeanRegistrator;
    // added when bean instance is registered to Osgi
    // can be unregistered using this registration
    private final OsgiRegistration osgiRegistration;
    private final ModuleJMXRegistrator moduleJMXRegistrator;
    private final int orderingIdx;
    private final boolean isDefaultBean;
    private final ModuleFactory moduleFactory;
    private final BundleContext bundleContext;

    public ModuleInternalInfo(ModuleIdentifier name,
            @Nullable DynamicReadableWrapper readableModule,
            OsgiRegistration osgiRegistration,
            @Nullable RootRuntimeBeanRegistratorImpl runtimeBeanRegistrator,
            ModuleJMXRegistrator moduleJMXRegistrator, int orderingIdx,
            boolean isDefaultBean, ModuleFactory moduleFactory, BundleContext bundleContext) {

        if (osgiRegistration == null) {
            throw new IllegalArgumentException(
                    "Parameter 'osgiRegistration' is missing");
        }
        this.readableModule = readableModule;
        this.osgiRegistration = osgiRegistration;
        this.runtimeBeanRegistrator = runtimeBeanRegistrator;
        this.name = name;
        this.moduleJMXRegistrator = moduleJMXRegistrator;
        this.orderingIdx = orderingIdx;
        this.isDefaultBean = isDefaultBean;
        this.moduleFactory = moduleFactory;
        this.bundleContext = bundleContext;
    }

    public DynamicReadableWrapper getReadableModule() {
        return readableModule;
    }

    public ModuleJMXRegistrator getModuleJMXRegistrator() {
        return moduleJMXRegistrator;
    }

    /**
     *
     * @return iif an running instance exists in the system.
     */
    public boolean hasReadableModule() {
        return readableModule != null;
    }

    @Override
    public String toString() {
        return "ModuleInternalInfo [name=" + name + "]";
    }

    public RootRuntimeBeanRegistratorImpl getRuntimeBeanRegistrator() {
        return runtimeBeanRegistrator;
    }

    public OsgiRegistration getOsgiRegistration() {
        return osgiRegistration;
    }

    /**
     * Get index representing dependency ordering within a transaction.
     */
    public int getOrderingIdx() {
        return orderingIdx;
    }

    /**
     * Compare using orderingIdx
     */
    @Override
    public int compareTo(ModuleInternalInfo o) {
        return Integer.compare(orderingIdx, o.orderingIdx);
    }

    public DestroyedModule toDestroyedModule() {
        return new DestroyedModule(getIdentifier(),
                getReadableModule().getInstance(), getModuleJMXRegistrator(),
                getOsgiRegistration(), getOrderingIdx(), runtimeBeanRegistrator);
    }

    @Override
    public ModuleIdentifier getIdentifier() {
        return name;
    }

    public boolean isDefaultBean() {
        return isDefaultBean;
    }

    public ModuleFactory getModuleFactory() {
        return moduleFactory;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }
}
