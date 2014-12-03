/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dependencyresolver;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.impl.ModuleInternalInfo;
import org.opendaylight.controller.config.manager.impl.dynamicmbean.DynamicReadableWrapper;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionModuleJMXRegistrator.TransactionModuleJMXRegistration;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.osgi.framework.BundleContext;

public class ModuleInternalTransactionalInfo implements Identifiable<ModuleIdentifier> {
    private final ModuleIdentifier name;
    private final Module proxiedModule, realModule;
    private final ModuleFactory moduleFactory;
    @Nullable
    private final ModuleInternalInfo maybeOldInternalInfo;
    private final TransactionModuleJMXRegistration transactionModuleJMXRegistration;
    private final boolean isDefaultBean;
    private final BundleContext bundleContext;

    public ModuleInternalTransactionalInfo(ModuleIdentifier name, Module proxiedModule,
                                           ModuleFactory moduleFactory,
                                           ModuleInternalInfo maybeOldInternalInfo,
                                           TransactionModuleJMXRegistration transactionModuleJMXRegistration,
                                           boolean isDefaultBean, Module realModule, BundleContext bundleContext) {
        this.name = name;
        this.proxiedModule = proxiedModule;
        this.moduleFactory = moduleFactory;
        this.maybeOldInternalInfo = maybeOldInternalInfo;
        this.transactionModuleJMXRegistration = transactionModuleJMXRegistration;
        this.isDefaultBean = isDefaultBean;
        this.realModule = realModule;
        this.bundleContext = bundleContext;
    }


    public boolean hasOldModule() {
        return maybeOldInternalInfo != null;
    }

    public DestroyedModule toDestroyedModule() {
        if (maybeOldInternalInfo == null) {
            throw new IllegalStateException("Cannot destroy uncommitted module");
        }
        DynamicReadableWrapper oldModule = maybeOldInternalInfo
                .getReadableModule();
        return new DestroyedModule(name, oldModule.getInstance(),
                maybeOldInternalInfo.getModuleJMXRegistrator(),
                maybeOldInternalInfo.getOsgiRegistration(),
                maybeOldInternalInfo.getOrderingIdx());
    }


    public Module getProxiedModule() {
        return proxiedModule;
    }

    public ModuleFactory getModuleFactory() {
        return moduleFactory;
    }

    @Nullable
    public ModuleInternalInfo getOldInternalInfo() {
        return Preconditions.checkNotNull(maybeOldInternalInfo);
    }

    public TransactionModuleJMXRegistration getTransactionModuleJMXRegistration() {
        return transactionModuleJMXRegistration;
    }

    @Override
    public ModuleIdentifier getIdentifier() {
        return name;
    }

    public boolean isDefaultBean() {
        return isDefaultBean;
    }

    public Module getRealModule() {
        return realModule;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }
}
