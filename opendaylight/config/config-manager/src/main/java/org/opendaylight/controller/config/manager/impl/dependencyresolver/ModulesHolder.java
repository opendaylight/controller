/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dependencyresolver;

import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.impl.CommitInfo;
import org.opendaylight.controller.config.manager.impl.TransactionIdentifier;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;

import javax.annotation.concurrent.GuardedBy;
import javax.management.InstanceAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents modules to be committed.
 */
class ModulesHolder {
    private final TransactionIdentifier transactionIdentifier;
    @GuardedBy("this")
    private final Map<ModuleIdentifier, ModuleInternalTransactionalInfo> commitMap = new HashMap<>();

    @GuardedBy("this")
    private final Set<ModuleInternalTransactionalInfo> unorderedDestroyedFromPreviousTransactions = new HashSet<>();

    ModulesHolder(TransactionIdentifier transactionIdentifier) {
        this.transactionIdentifier = transactionIdentifier;
    }


    public CommitInfo toCommitInfo() {
        List<DestroyedModule> orderedDestroyedFromPreviousTransactions = new ArrayList<>(
                unorderedDestroyedFromPreviousTransactions.size());
        for (ModuleInternalTransactionalInfo toBeDestroyed : unorderedDestroyedFromPreviousTransactions) {
            orderedDestroyedFromPreviousTransactions.add(toBeDestroyed
                    .toDestroyedModule());
        }
        Collections.sort(orderedDestroyedFromPreviousTransactions);
        return new CommitInfo(orderedDestroyedFromPreviousTransactions,
                commitMap);
    }

    private ModuleInternalTransactionalInfo findModuleInternalTransactionalInfo(
            ModuleIdentifier moduleIdentifier,
            JmxAttribute jmxAttributeForReporting) {
        ModuleInternalTransactionalInfo moduleInternalTransactionalInfo = commitMap
                .get(moduleIdentifier);
        JmxAttributeValidationException.checkNotNull(
                moduleInternalTransactionalInfo, "Module " + moduleIdentifier
                        + "" + " not found in transaction " + transactionIdentifier,
                jmxAttributeForReporting);
        return moduleInternalTransactionalInfo;
    }

    public Module findModule(ModuleIdentifier moduleIdentifier,
            JmxAttribute jmxAttributeForReporting) {
        return findModuleInternalTransactionalInfo(moduleIdentifier,
                jmxAttributeForReporting).getProxiedModule();
    }

    public ModuleFactory findModuleFactory(ModuleIdentifier moduleIdentifier,
            JmxAttribute jmxAttributeForReporting) {
        return findModuleInternalTransactionalInfo(moduleIdentifier,
                jmxAttributeForReporting).getModuleFactory();
    }

    public Map<ModuleIdentifier, Module> getAllModules() {
        Map<ModuleIdentifier, Module> result = new HashMap<>();
        for (ModuleInternalTransactionalInfo entry : commitMap.values()) {
            ModuleIdentifier name = entry.getIdentifier();
            result.put(name, entry.getProxiedModule());
        }
        return result;
    }

    public void put(
            ModuleInternalTransactionalInfo moduleInternalTransactionalInfo) {
        commitMap.put(moduleInternalTransactionalInfo.getIdentifier(),
                moduleInternalTransactionalInfo);
    }

    public ModuleInternalTransactionalInfo destroyModule(
            ModuleIdentifier moduleIdentifier) {
        ModuleInternalTransactionalInfo found = commitMap.remove(moduleIdentifier);
        if (found == null) {
            throw new IllegalStateException("Not found:" + moduleIdentifier);
        }
        if (found.hasOldModule()) {
            unorderedDestroyedFromPreviousTransactions.add(found);
        }
        return found;
    }

    public void assertNotExists(ModuleIdentifier moduleIdentifier)
            throws InstanceAlreadyExistsException {
        if (commitMap.containsKey(moduleIdentifier)) {
            throw new InstanceAlreadyExistsException(
                    "There is an instance registered with name " + moduleIdentifier);
        }
    }

    public Collection<ModuleInternalTransactionalInfo> getAllInfos(){
        return commitMap.values();
    }

    public ModuleInternalTransactionalInfo findModuleInternalTransactionalInfo(ModuleIdentifier moduleIdentifier) {
        ModuleInternalTransactionalInfo found = commitMap.get(moduleIdentifier);
        if (found == null) {
            throw new IllegalStateException("Not found:" + moduleIdentifier);
        }
        return found;
    }
}
