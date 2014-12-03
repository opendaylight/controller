/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dependencyresolver;

import static com.google.common.base.Preconditions.checkState;
import com.google.common.base.Preconditions;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
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
import org.osgi.framework.BundleContext;

/**
 * Holds information about modules being created and destroyed within this
 * transaction. Observes usage of DependencyResolver within modules to figure
 * out dependency tree.
 */
public class DependencyResolverManager implements DependencyResolverFactory, AutoCloseable {
    @GuardedBy("this")
    private final Map<ModuleIdentifier, DependencyResolverImpl> moduleIdentifiersToDependencyResolverMap = new HashMap<>();
    private final TransactionIdentifier transactionIdentifier;
    private final ModulesHolder modulesHolder;
    private final TransactionStatus transactionStatus;
    private final ServiceReferenceReadableRegistry readableRegistry;
    private final CodecRegistry codecRegistry;
    private final DeadlockMonitor deadlockMonitor;
    private final MBeanServer mBeanServer;

    public DependencyResolverManager(final TransactionIdentifier transactionIdentifier,
                                     final TransactionStatus transactionStatus,
                                     final ServiceReferenceReadableRegistry readableRegistry, final CodecRegistry codecRegistry,
                                     final MBeanServer mBeanServer) {
        this.transactionIdentifier = transactionIdentifier;
        this.modulesHolder = new ModulesHolder(transactionIdentifier);
        this.transactionStatus = transactionStatus;
        this.readableRegistry = readableRegistry;
        this.codecRegistry = codecRegistry;
        this.deadlockMonitor = new DeadlockMonitor(transactionIdentifier);
        this.mBeanServer = mBeanServer;
    }

    @Override
    public DependencyResolver createDependencyResolver(final ModuleIdentifier moduleIdentifier) {
        return getOrCreate(moduleIdentifier);
    }

    public synchronized DependencyResolverImpl getOrCreate(final ModuleIdentifier name) {
        DependencyResolverImpl dependencyResolver = moduleIdentifiersToDependencyResolverMap.get(name);
        if (dependencyResolver == null) {
            transactionStatus.checkNotCommitted();
            dependencyResolver = new DependencyResolverImpl(name, transactionStatus, modulesHolder, readableRegistry,
                    codecRegistry, transactionIdentifier.getName(), mBeanServer);
            moduleIdentifiersToDependencyResolverMap.put(name, dependencyResolver);
        }
        return dependencyResolver;
    }

    /**
     * Get all dependency resolvers, including those that belong to destroyed
     * things?
     */
    private List<DependencyResolverImpl> getAllSorted() {
        transactionStatus.checkCommitStarted();
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
            final ModuleIdentifier moduleIdentifier) {
        transactionStatus.checkNotCommitted();
        ModuleInternalTransactionalInfo found = modulesHolder
                .destroyModule(moduleIdentifier);
        moduleIdentifiersToDependencyResolverMap.remove(moduleIdentifier);
        return found;
    }

    // protect write access

    private static final class ModuleInvocationHandler extends AbstractInvocationHandler {
        private final DeadlockMonitor deadlockMonitor;
        private final ModuleIdentifier moduleIdentifier;
        private final Module module;

        // optimization: subsequent calls to getInstance MUST return the same value during transaction,
        // so it is safe to cache the response
        private Object cachedInstance;

        ModuleInvocationHandler(final DeadlockMonitor deadlockMonitor, final ModuleIdentifier moduleIdentifier, final Module module) {
            this.deadlockMonitor = Preconditions.checkNotNull(deadlockMonitor);
            this.moduleIdentifier = Preconditions.checkNotNull(moduleIdentifier);
            this.module = Preconditions.checkNotNull(module);
        }

        @Override
        protected Object handleInvocation(final Object proxy, final Method method, final Object[] args) throws Throwable {
            boolean isGetInstance = "getInstance".equals(method.getName());
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
    }

    public void put(
            final ModuleIdentifier moduleIdentifier,
            final Module module,
            final ModuleFactory moduleFactory,
            final ModuleInternalInfo maybeOldInternalInfo,
            final TransactionModuleJMXRegistration transactionModuleJMXRegistration,
            final boolean isDefaultBean, final BundleContext bundleContext) {
        transactionStatus.checkNotCommitted();

        Class<? extends Module> moduleClass = Module.class;
        if (module instanceof RuntimeBeanRegistratorAwareModule) {
            moduleClass = RuntimeBeanRegistratorAwareModule.class;
        }
        Module proxiedModule = Reflection.newProxy(moduleClass, new ModuleInvocationHandler(deadlockMonitor, moduleIdentifier, module));
        ModuleInternalTransactionalInfo moduleInternalTransactionalInfo = new ModuleInternalTransactionalInfo(
                moduleIdentifier, proxiedModule, moduleFactory,
                maybeOldInternalInfo, transactionModuleJMXRegistration, isDefaultBean, module, bundleContext);
        modulesHolder.put(moduleInternalTransactionalInfo);
    }

    // wrapped methods:

    public CommitInfo toCommitInfo() {
        return modulesHolder.toCommitInfo();
    }

    public Module findModule(final ModuleIdentifier moduleIdentifier,
                             final JmxAttribute jmxAttributeForReporting) {
        return modulesHolder.findModule(moduleIdentifier,
                jmxAttributeForReporting);
    }

    public ModuleInternalTransactionalInfo findModuleInternalTransactionalInfo(final ModuleIdentifier moduleIdentifier) {
        return modulesHolder.findModuleInternalTransactionalInfo(moduleIdentifier);
    }

    public ModuleFactory findModuleFactory(final ModuleIdentifier moduleIdentifier,
                                           final JmxAttribute jmxAttributeForReporting) {
        return modulesHolder.findModuleFactory(moduleIdentifier,
                jmxAttributeForReporting);
    }

    public Map<ModuleIdentifier, Module> getAllModules() {
        return modulesHolder.getAllModules();
    }

    public void assertNotExists(final ModuleIdentifier moduleIdentifier)
            throws InstanceAlreadyExistsException {
        modulesHolder.assertNotExists(moduleIdentifier);
    }

    public List<ModuleIdentifier> findAllByFactory(final ModuleFactory factory) {
        List<ModuleIdentifier> result = new ArrayList<>();
        for (ModuleInternalTransactionalInfo info : modulesHolder.getAllInfos()) {
            if (factory.equals(info.getModuleFactory())) {
                result.add(info.getIdentifier());
            }
        }
        return result;
    }

    @Override
    public void close() {
        deadlockMonitor.close();
    }

}
