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
import org.opendaylight.controller.config.manager.impl.dynamicmbean.DynamicReadableWrapper;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionModuleJMXRegistrator
        .TransactionModuleJMXRegistration;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.yangtools.concepts.Identifiable;

public class ModuleInternalTransactionalInfo implements Identifiable<ModuleIdentifier> {
    private final ModuleIdentifier name;
    private final Module module;
    private final ModuleFactory moduleFactory;
    @Nullable
    private final ModuleInternalInfo maybeOldInternalInfo;
    private final TransactionModuleJMXRegistration transactionModuleJMXRegistration;

    ModuleInternalTransactionalInfo(ModuleIdentifier name, Module module,
            ModuleFactory moduleFactory,
            ModuleInternalInfo maybeOldInternalInfo,
            TransactionModuleJMXRegistration transactionModuleJMXRegistration) {
        this.name = name;
        this.module = module;
        this.moduleFactory = moduleFactory;
        this.maybeOldInternalInfo = maybeOldInternalInfo;
        this.transactionModuleJMXRegistration = transactionModuleJMXRegistration;
    }


    /**
     * Use {@link #getIdentifier()} instead.
     */
    @Deprecated
    public ModuleIdentifier getName() {
        return name;
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


    public Module getModule() {
        return module;
    }

    public ModuleFactory getModuleFactory() {
        return moduleFactory;
    }

    @Nullable
    public ModuleInternalInfo getOldInternalInfo() {
        if (maybeOldInternalInfo == null)
            throw new NullPointerException();
        return maybeOldInternalInfo;
    }

    public TransactionModuleJMXRegistration getTransactionModuleJMXRegistration() {
        return transactionModuleJMXRegistration;
    }

    @Override
    public ModuleIdentifier getIdentifier() {
        return name;
    }
}
