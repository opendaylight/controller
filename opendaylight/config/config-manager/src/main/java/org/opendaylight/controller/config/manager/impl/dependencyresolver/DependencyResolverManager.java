/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dependencyresolver;

import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.RuntimeBeanRegistratorAwareModule;
import org.opendaylight.controller.config.api.ServiceReferenceReadableRegistry;
import org.opendaylight.controller.config.manager.impl.CommitInfo;
import org.opendaylight.controller.config.manager.impl.DeadlockMonitor;
import org.opendaylight.controller.config.manager.impl.ModuleInternalInfo;
import org.opendaylight.controller.config.manager.impl.TransactionIdentifier;
import org.opendaylight.controller.config.manager.impl.TransactionStatus;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionModuleJMXRegistrator.TransactionModuleJMXRegistration;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;

import javax.annotation.concurrent.GuardedBy;
import javax.management.InstanceAlreadyExistsException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/**
 * Holds information about modules being created and destroyed within this
 * transaction. Observes usage of DependencyResolver within modules to figure
 * out dependency tree.
 */
public class DependencyResolverManager implements DependencyResolverFactory, AutoCloseable {
    @GuardedBy("this")
    private final Map<ModuleIdentifier, DependencyResolverImpl> moduleIdentifiersToDependencyResolverMap = new HashMap<>();
    private final ModulesHolder modulesHolder;
    private final TransactionStatus transactionStatus;
    private final ServiceReferenceReadableRegistry readableRegistry;
    private final CodecRegistry codecRegistry;
    private final DeadlockMonitor deadlockMonitor;

    public DependencyResolverManager(TransactionIdentifier transactionIdentifier,
                                     TransactionStatus transactionStatus, ServiceReferenceReadableRegistry readableRegistry, CodecRegistry codecRegistry) {
        this.modulesHolder = new ModulesHolder(transactionIdentifier);
        this.transactionStatus = transactionStatus;
        this.readableRegistry = readableRegistry;
        this.codecRegistry = codecRegistry;
        this.deadlockMonitor = new DeadlockMonitor(transactionIdentifier);
    }

    @Override
    public DependencyResolver createDependencyResolver(ModuleIdentifier moduleIdentifier) {
        return getOrCreate(moduleIdentifier);
    }

    public synchronized DependencyResolverImpl getOrCreate(ModuleIdentifier name) {
        DependencyResolverImpl dependencyResolver = moduleIdentifiersToDependencyResolverMap.get(name);
        if (dependencyResolver == null) {
            transactionStatus.checkNotCommitted();
            dependencyResolver = new DependencyResolverImpl(name, transactionStatus, modulesHolder, readableRegistry, codecRegistry);
            moduleIdentifiersToDependencyResolverMap.put(name, dependencyResolver);
        }
        return dependencyResolver;
    }

    /**
     * Get all dependency resolvers, including those that belong to destroyed
     * things?
     */
    private List<DependencyResolverImpl> getAllSorted() {
        transactionStatus.checkCommitted();
        List<DependencyResolverImpl> sorted = new ArrayList<>(
                moduleIdentifiersToDependencyResolverMap.values());
        for (DependencyResolverImpl dri : sorted) {
            dri.countMaxDependencyDepth(this);
        }
        Collections.sort(sorted);
        return sorted;
    }

    public List<ModuleIdentifier> getSortedModuleIdentifiers() {
        List<ModuleIdentifier> result = new ArrayList<>(
                moduleIdentifiersToDependencyResolverMap.size());
        for (DependencyResolverImpl dri : getAllSorted()) {
            ModuleIdentifier driName = dri.getIdentifier();
            result.add(driName);
        }
        return result;
    }

    public ModuleInternalTransactionalInfo destroyModule(
            ModuleIdentifier moduleIdentifier) {
        transactionStatus.checkNotCommitted();
        ModuleInternalTransactionalInfo found = modulesHolder
                .destroyModule(moduleIdentifier);
        moduleIdentifiersToDependencyResolverMap.remove(moduleIdentifier);
        return found;
    }

    // protect write access

    public void put(
            final ModuleIdentifier moduleIdentifier,
            final Module module,
            ModuleFactory moduleFactory,
            ModuleInternalInfo maybeOldInternalInfo,
            TransactionModuleJMXRegistration transactionModuleJMXRegistration,
            boolean isDefaultBean) {
        transactionStatus.checkNotCommitted();

        Class<? extends Module> moduleClass = Module.class;
        if (module instanceof RuntimeBeanRegistratorAwareModule) {
            moduleClass = RuntimeBeanRegistratorAwareModule.class;
        }
        Module proxiedModule = Reflection.newProxy(moduleClass, new AbstractInvocationHandler() {
            // optimization: subsequent calls to getInstance MUST return the same value during transaction,
            // so it is safe to cache the response
            private Object cachedInstance;

            @Override
            protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
                boolean isGetInstance = method.getName().equals("getInstance");
                if (isGetInstance) {
                    if (cachedInstance != null) {
                        return cachedInstance;
                    }

                    checkState(deadlockMonitor.isAlive(), "Deadlock monitor is not alive");
                    deadlockMonitor.setCurrentlyInstantiatedModule(moduleIdentifier);
                }
                try {
                    Object response = method.invoke(module, args);
                    if (isGetInstance) {
                        cachedInstance = response;
                    }
                    return response;
                } catch(InvocationTargetException e) {
                    throw e.getCause();
                } finally {
                    if (isGetInstance) {
                        deadlockMonitor.setCurrentlyInstantiatedModule(null);
                    }
                }
            }
        });


        ModuleInternalTransactionalInfo moduleInternalTransactionalInfo = new ModuleInternalTransactionalInfo(
                moduleIdentifier, proxiedModule, moduleFactory,
                maybeOldInternalInfo, transactionModuleJMXRegistration, isDefaultBean, module);
        modulesHolder.put(moduleInternalTransactionalInfo);
    }

    // wrapped methods:

    public CommitInfo toCommitInfo() {
        return modulesHolder.toCommitInfo();
    }

    public Module findModule(ModuleIdentifier moduleIdentifier,
                             JmxAttribute jmxAttributeForReporting) {
        return modulesHolder.findModule(moduleIdentifier,
                jmxAttributeForReporting);
    }

    public ModuleInternalTransactionalInfo findModuleInternalTransactionalInfo(ModuleIdentifier moduleIdentifier) {
        return modulesHolder.findModuleInternalTransactionalInfo(moduleIdentifier);
    }

    public ModuleFactory findModuleFactory(ModuleIdentifier moduleIdentifier,
                                           JmxAttribute jmxAttributeForReporting) {
        return modulesHolder.findModuleFactory(moduleIdentifier,
                jmxAttributeForReporting);
    }

    public Map<ModuleIdentifier, Module> getAllModules() {
        return modulesHolder.getAllModules();
    }

    public void assertNotExists(ModuleIdentifier moduleIdentifier)
            throws InstanceAlreadyExistsException {
        modulesHolder.assertNotExists(moduleIdentifier);
    }

    public List<ModuleIdentifier> findAllByFactory(ModuleFactory factory) {
        List<ModuleIdentifier> result = new ArrayList<>();
        for (ModuleInternalTransactionalInfo info : modulesHolder.getAllInfos()) {
            if (factory.equals(info.getModuleFactory())) {
                result.add(info.getIdentifier());
            }
        }
        return result;
    }

    public void close() {
        deadlockMonitor.close();
    }

}
