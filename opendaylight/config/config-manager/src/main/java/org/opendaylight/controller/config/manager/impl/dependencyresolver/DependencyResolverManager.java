/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dependencyresolver;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ServiceReferenceReadableRegistry;
import org.opendaylight.controller.config.manager.impl.CommitInfo;
import org.opendaylight.controller.config.manager.impl.ModuleInternalTransactionalInfo;
import org.opendaylight.controller.config.manager.impl.TransactionStatus;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;

import javax.annotation.concurrent.GuardedBy;
import javax.management.InstanceAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds information about modules being created and destroyed within this
 * transaction. Observes usage of DependencyResolver within modules to figure
 * out dependency tree.
 */
public class DependencyResolverManager implements TransactionHolder, DependencyResolverFactory {
    @GuardedBy("this")
    private final Map<ModuleIdentifier, DependencyResolverImpl> moduleIdentifiersToDependencyResolverMap = new HashMap<>();
    private final ModulesHolder modulesHolder;
    private final TransactionStatus transactionStatus;
    private final ServiceReferenceReadableRegistry readableRegistry;
    private final CodecRegistry codecRegistry;

    public DependencyResolverManager(String transactionName,
                                     TransactionStatus transactionStatus, ServiceReferenceReadableRegistry readableRegistry, CodecRegistry codecRegistry) {
        this.modulesHolder = new ModulesHolder(transactionName);
        this.transactionStatus = transactionStatus;
        this.readableRegistry = readableRegistry;
        this.codecRegistry = codecRegistry;
    }

    @Override
    public DependencyResolver createDependencyResolver(ModuleIdentifier moduleIdentifier) {
        return getOrCreate(moduleIdentifier);
    }

    @Override
    public DependencyResolver createTemporaryDependencyResolver() {
        return new DependencyResolverImpl(null, transactionStatus, modulesHolder, readableRegistry, codecRegistry);
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

    @Override
    public ModuleInternalTransactionalInfo destroyModule(
            ModuleIdentifier moduleIdentifier) {
        transactionStatus.checkNotCommitted();
        ModuleInternalTransactionalInfo found = modulesHolder
                .destroyModule(moduleIdentifier);
        moduleIdentifiersToDependencyResolverMap.remove(moduleIdentifier);
        return found;
    }

    // protect write access
    @Override
    public void put(
            ModuleInternalTransactionalInfo moduleInternalTransactionalInfo) {
        transactionStatus.checkNotCommitted();
        modulesHolder.put(moduleInternalTransactionalInfo);
    }

    // wrapped methods:

    @Override
    public CommitInfo toCommitInfo() {
        return modulesHolder.toCommitInfo();
    }

    @Override
    public Module findModule(ModuleIdentifier moduleIdentifier,
            JmxAttribute jmxAttributeForReporting) {
        return modulesHolder.findModule(moduleIdentifier,
                jmxAttributeForReporting);
    }

    @Override
    public ModuleInternalTransactionalInfo findModuleInternalTransactionalInfo(ModuleIdentifier moduleIdentifier) {
        return modulesHolder.findModuleInternalTransactionalInfo(moduleIdentifier);
    }

    @Override
    public ModuleFactory findModuleFactory(ModuleIdentifier moduleIdentifier,
            JmxAttribute jmxAttributeForReporting) {
        return modulesHolder.findModuleFactory(moduleIdentifier,
                jmxAttributeForReporting);
    }

    @Override
    public Map<ModuleIdentifier, Module> getAllModules() {
        return modulesHolder.getAllModules();
    }

    @Override
    public void assertNotExists(ModuleIdentifier moduleIdentifier)
            throws InstanceAlreadyExistsException {
        modulesHolder.assertNotExists(moduleIdentifier);
    }

    public List<ModuleIdentifier> findAllByFactory(ModuleFactory factory) {
        List<ModuleIdentifier> result = new ArrayList<>();
        for( ModuleInternalTransactionalInfo  info : modulesHolder.getAllInfos()) {
            if (factory.equals(info.getModuleFactory())) {
                result.add(info.getIdentifier());
            }
        }
        return result;
    }

    @Override
    public Optional<ModuleInternalTransactionalInfo> findInfo(ModuleIdentifier moduleIdentifier) {
        return modulesHolder.findInfo(moduleIdentifier);
    }
}
