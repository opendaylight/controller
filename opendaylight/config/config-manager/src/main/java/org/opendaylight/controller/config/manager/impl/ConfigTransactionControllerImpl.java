/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.dependencyresolver.DependencyResolverManager;
import org.opendaylight.controller.config.manager.impl.dynamicmbean.DynamicWritableWrapper;
import org.opendaylight.controller.config.manager.impl.dynamicmbean.ReadOnlyAtomicBoolean;
import org.opendaylight.controller.config.manager.impl.dynamicmbean.ReadOnlyAtomicBoolean.ReadOnlyAtomicBooleanImpl;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HierarchicalConfigMBeanFactoriesHolder;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionModuleJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionModuleJMXRegistrator.TransactionModuleJMXRegistration;
import org.opendaylight.controller.config.manager.impl.util.LookupBeansUtil;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;

/**
 * This is a JMX bean representing current transaction. It contains
 * {@link #transactionIdentifier}, unique version and parent version for
 * optimistic locking.
 */
class ConfigTransactionControllerImpl implements
        ConfigTransactionControllerInternal,
        ConfigTransactionControllerImplMXBean,
        Identifiable<TransactionIdentifier>{
    private static final Logger logger = LoggerFactory.getLogger(ConfigTransactionControllerImpl.class);

    private final TransactionIdentifier transactionIdentifier;
    private final ObjectName controllerON;
    private final TransactionJMXRegistrator transactionRegistrator;
    private final TransactionModuleJMXRegistrator txModuleJMXRegistrator;
    private final long parentVersion, currentVersion;
    private final HierarchicalConfigMBeanFactoriesHolder factoriesHolder;
    private final DependencyResolverManager dependencyResolverManager;
    private final TransactionStatus transactionStatus;
    private final MBeanServer transactionsMBeanServer;
    private final List<ModuleFactory> currentlyRegisteredFactories;

    /**
     * Disables ability of {@link DynamicWritableWrapper} to change attributes
     * during validation.
     */
    @GuardedBy("this")
    private final AtomicBoolean configBeanModificationDisabled = new AtomicBoolean(
            false);
    private final ReadOnlyAtomicBoolean readOnlyAtomicBoolean = new ReadOnlyAtomicBooleanImpl(
            configBeanModificationDisabled);
    private final MBeanServer configMBeanServer;

    private final BundleContext bundleContext;

    public ConfigTransactionControllerImpl(String transactionName,
                                           TransactionJMXRegistrator transactionRegistrator,
                                           long parentVersion, long currentVersion,
                                           List<ModuleFactory> currentlyRegisteredFactories,
                                           MBeanServer transactionsMBeanServer, MBeanServer configMBeanServer, BundleContext bundleContext) {

        this.transactionIdentifier = new TransactionIdentifier(transactionName);
        this.controllerON = ObjectNameUtil
                .createTransactionControllerON(transactionName);
        this.transactionRegistrator = transactionRegistrator;
        txModuleJMXRegistrator = transactionRegistrator
                .createTransactionModuleJMXRegistrator();
        this.parentVersion = parentVersion;
        this.currentVersion = currentVersion;
        this.currentlyRegisteredFactories = currentlyRegisteredFactories;
        this.factoriesHolder = new HierarchicalConfigMBeanFactoriesHolder(currentlyRegisteredFactories);
        this.transactionStatus = new TransactionStatus();
        this.dependencyResolverManager = new DependencyResolverManager(transactionName, transactionStatus);
        this.transactionsMBeanServer = transactionsMBeanServer;
        this.configMBeanServer = configMBeanServer;
        this.bundleContext = bundleContext;
    }

    @Override
    public void copyExistingModulesAndProcessFactoryDiff(Collection<ModuleInternalInfo> existingModules, List<ModuleFactory> lastListOfFactories) {
        // copy old configuration to this server
        for (ModuleInternalInfo oldConfigInfo : existingModules) {
            try {
                copyExistingModule(oldConfigInfo);
            } catch (InstanceAlreadyExistsException e) {
                throw new IllegalStateException("Error while copying " + oldConfigInfo, e);
            }
        }
        processDefaultBeans(lastListOfFactories);
    }

    private synchronized void processDefaultBeans(List<ModuleFactory> lastListOfFactories) {
        transactionStatus.checkNotCommitStarted();
        transactionStatus.checkNotAborted();

        Set<ModuleFactory> oldSet = new HashSet<>(lastListOfFactories);
        Set<ModuleFactory> newSet = new HashSet<>(currentlyRegisteredFactories);

        List<ModuleFactory> toBeAdded = new ArrayList<>();
        List<ModuleFactory> toBeRemoved = new ArrayList<>();
        for(ModuleFactory moduleFactory: currentlyRegisteredFactories) {
            if (oldSet.contains(moduleFactory) == false){
                toBeAdded.add(moduleFactory);
            }
        }
        for(ModuleFactory moduleFactory: lastListOfFactories){
            if (newSet.contains(moduleFactory) == false) {
                toBeRemoved.add(moduleFactory);
            }
        }
        // add default modules
        for (ModuleFactory moduleFactory : toBeAdded) {
            Set<? extends Module> defaultModules = moduleFactory.getDefaultModules(dependencyResolverManager, bundleContext);
            for (Module module : defaultModules) {
                // ensure default module to be registered to jmx even if its module factory does not use dependencyResolverFactory
                DependencyResolver dependencyResolver = dependencyResolverManager.getOrCreate(module.getIdentifier());
                try {
                    putConfigBeanToJMXAndInternalMaps(module.getIdentifier(), module, moduleFactory, null, dependencyResolver);
                } catch (InstanceAlreadyExistsException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        // remove modules belonging to removed factories
        for(ModuleFactory removedFactory: toBeRemoved){
            List<ModuleIdentifier> modulesOfRemovedFactory = dependencyResolverManager.findAllByFactory(removedFactory);
            for (ModuleIdentifier name : modulesOfRemovedFactory) {
                destroyModule(name);
            }
        }
    }


    private synchronized void copyExistingModule(
            ModuleInternalInfo oldConfigBeanInfo)
            throws InstanceAlreadyExistsException {
        transactionStatus.checkNotCommitStarted();
        transactionStatus.checkNotAborted();
        ModuleIdentifier moduleIdentifier = oldConfigBeanInfo.getName();
        dependencyResolverManager.assertNotExists(moduleIdentifier);

        ModuleFactory moduleFactory = factoriesHolder
                .findByModuleName(moduleIdentifier.getFactoryName());

        Module module;
        DependencyResolver dependencyResolver = dependencyResolverManager
                .getOrCreate(moduleIdentifier);
        try {
            module = moduleFactory.createModule(
                    moduleIdentifier.getInstanceName(), dependencyResolver,
                    oldConfigBeanInfo.getReadableModule(), bundleContext);
        } catch (Exception e) {
            throw new IllegalStateException(format(
                    "Error while copying old configuration from %s to %s",
                    oldConfigBeanInfo, moduleFactory), e);
        }
        putConfigBeanToJMXAndInternalMaps(moduleIdentifier, module, moduleFactory, oldConfigBeanInfo, dependencyResolver);
    }

    @Override
    public synchronized ObjectName createModule(String factoryName,
            String instanceName) throws InstanceAlreadyExistsException {

        transactionStatus.checkNotCommitStarted();
        transactionStatus.checkNotAborted();
        ModuleIdentifier moduleIdentifier = new ModuleIdentifier(factoryName, instanceName);
        dependencyResolverManager.assertNotExists(moduleIdentifier);

        // find factory
        ModuleFactory moduleFactory = factoriesHolder.findByModuleName(factoryName);
        DependencyResolver dependencyResolver = dependencyResolverManager.getOrCreate(moduleIdentifier);
        Module module = moduleFactory.createModule(instanceName, dependencyResolver, bundleContext);
        return putConfigBeanToJMXAndInternalMaps(moduleIdentifier, module,
                moduleFactory, null, dependencyResolver);
    }

    private synchronized ObjectName putConfigBeanToJMXAndInternalMaps(
            ModuleIdentifier moduleIdentifier, Module module,
            ModuleFactory moduleFactory,
            @Nullable ModuleInternalInfo maybeOldConfigBeanInfo, DependencyResolver dependencyResolver)
            throws InstanceAlreadyExistsException {

        logger.debug("Adding module {} to transaction {}", moduleIdentifier, this);
        if (moduleIdentifier.equals(module.getIdentifier())==false) {
            throw new IllegalStateException("Incorrect name reported by module. Expected "
             + moduleIdentifier + ", got " + module.getIdentifier());
        }
        if (dependencyResolver.getIdentifier().equals(moduleIdentifier) == false ) {
            throw new IllegalStateException("Incorrect name reported by dependency resolver. Expected "
                    + moduleIdentifier + ", got " + dependencyResolver.getIdentifier());
        }
        DynamicMBean writableDynamicWrapper = new DynamicWritableWrapper(
                module, moduleIdentifier, transactionIdentifier,
                readOnlyAtomicBoolean, transactionsMBeanServer,
                configMBeanServer);

        ObjectName writableON = ObjectNameUtil.createTransactionModuleON(
                transactionIdentifier.getName(), moduleIdentifier);
        // put wrapper to jmx
        TransactionModuleJMXRegistration transactionModuleJMXRegistration = txModuleJMXRegistrator
                .registerMBean(writableDynamicWrapper, writableON);
        ModuleInternalTransactionalInfo moduleInternalTransactionalInfo = new ModuleInternalTransactionalInfo(
                moduleIdentifier, module, moduleFactory,
                maybeOldConfigBeanInfo, transactionModuleJMXRegistration);

        dependencyResolverManager.put(moduleInternalTransactionalInfo);
        return writableON;
    }

    @Override
    public void destroyModule(ObjectName objectName)
            throws InstanceNotFoundException {
        String foundTransactionName = ObjectNameUtil
                .getTransactionName(objectName);
        if (transactionIdentifier.getName().equals(foundTransactionName) == false) {
            throw new IllegalArgumentException("Wrong transaction name "
                    + objectName);
        }
        ObjectNameUtil.checkDomain(objectName);
        ModuleIdentifier moduleIdentifier = ObjectNameUtil.fromON(objectName,
                ObjectNameUtil.TYPE_MODULE);
        destroyModule(moduleIdentifier);
    }

    private void destroyModule(ModuleIdentifier moduleIdentifier) {
        logger.debug("Destroying module {} in transaction {}", moduleIdentifier, this);
        transactionStatus.checkNotAborted();
        ModuleInternalTransactionalInfo removedTInfo = dependencyResolverManager.destroyModule(moduleIdentifier);
        // remove from jmx
        removedTInfo.getTransactionModuleJMXRegistration().close();
    }

    @Override
    public long getParentVersion() {
        return parentVersion;
    }

    @Override
    public long getVersion() {
        return currentVersion;
    }

    @Override
    public synchronized void validateConfig() throws ValidationException {
        if (configBeanModificationDisabled.get())
            throw new IllegalStateException("Cannot start validation");
        configBeanModificationDisabled.set(true);
        try {
            validate_noLocks();
        } finally {
            configBeanModificationDisabled.set(false);
        }
    }

    private void validate_noLocks() throws ValidationException {
        transactionStatus.checkNotAborted();
        logger.info("Validating transaction {}", transactionIdentifier);
        // call validate()
        List<ValidationException> collectedExceptions = new ArrayList<>();
        for (Entry<ModuleIdentifier, Module> entry : dependencyResolverManager
                .getAllModules().entrySet()) {
            ModuleIdentifier name = entry.getKey();
            Module module = entry.getValue();
            try {
                module.validate();
            } catch (Exception e) {
                logger.warn("Validation exception in {}", getTransactionName(),
                        e);
                collectedExceptions.add(ValidationException
                        .createForSingleException(name, e));
            }
        }
        if (collectedExceptions.size() > 0) {
            throw ValidationException
                    .createFromCollectedValidationExceptions(collectedExceptions);
        }
        logger.info("Validated transaction {}", transactionIdentifier);
    }

    /**
     * If this method passes validation, it will grab
     * {@link TransactionStatus#secondPhaseCommitStarted} lock. This lock will
     * prevent calling @{link #validateBeforeCommitAndLockTransaction},
     * effectively only allowing to call {@link #secondPhaseCommit} after
     * successful return of this method.
     */
    @Override
    public synchronized CommitInfo validateBeforeCommitAndLockTransaction()
            throws ValidationException {
        transactionStatus.checkNotAborted();
        transactionStatus.checkNotCommitStarted();
        configBeanModificationDisabled.set(true);
        try {
            validate_noLocks();
        } catch (ValidationException e) {
            logger.info("Commit failed on validation");
            configBeanModificationDisabled.set(false); // recoverable error
            throw e;
        }
        // errors in this state are not recoverable. modules are not mutable
        // anymore.
        transactionStatus.setSecondPhaseCommitStarted();
        return dependencyResolverManager.toCommitInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<ModuleIdentifier> secondPhaseCommit() {
        transactionStatus.checkNotAborted();
        transactionStatus.checkCommitStarted();
        if (configBeanModificationDisabled.get() == false) {
            throw new IllegalStateException(
                    "Internal error - validateBeforeCommitAndLockTransaction should be called "
                            + "to obtain a lock");
        }

        logger.info("Committing transaction {}", transactionIdentifier);

        // call getInstance()
        for (Entry<ModuleIdentifier, Module> entry : dependencyResolverManager
                .getAllModules().entrySet()) {
            Module module = entry.getValue();
            ModuleIdentifier name = entry.getKey();
            try {
                logger.debug("About to commit {} in transaction {}",
                        name, transactionIdentifier);
                module.getInstance();
            } catch (Exception e) {
                logger.error("Commit failed on {} in transaction {}", name,
                        transactionIdentifier, e);
                internalAbort();
                throw new RuntimeException(
                        format("Error - getInstance() failed for %s in transaction %s",
                                name, transactionIdentifier), e);
            }
        }

        // count dependency order

        logger.info("Committed configuration {}", transactionIdentifier);
        transactionStatus.setCommitted();
        // unregister this and all modules from jmx
        close();

        return dependencyResolverManager.getSortedModuleIdentifiers();
    }

    @Override
    public synchronized void abortConfig() {
        transactionStatus.checkNotCommitStarted();
        transactionStatus.checkNotAborted();
        internalAbort();
    }

    private void internalAbort() {
        transactionStatus.setAborted();
        close();
    }

    private void close() {
        transactionRegistrator.close();
    }

    @Override
    public ObjectName getControllerObjectName() {
        return controllerON;
    }

    @Override
    public String getTransactionName() {
        return transactionIdentifier.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ObjectName> lookupConfigBeans() {
        return lookupConfigBeans("*", "*");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName) {
        return lookupConfigBeans(moduleName, "*");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectName lookupConfigBean(String moduleName, String instanceName)
            throws InstanceNotFoundException {
        return LookupBeansUtil.lookupConfigBean(this, moduleName, instanceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName,
            String instanceName) {
        ObjectName namePattern = ObjectNameUtil.createModulePattern(moduleName,
                instanceName, transactionIdentifier.getName());
        return txModuleJMXRegistrator.queryNames(namePattern, null);
    }

    @Override
    public Set<String> getAvailableModuleNames() {
        return factoriesHolder.getModuleNames();
    }

    @Override
    public boolean isClosed() {
        return transactionStatus.isAbortedOrCommitted();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("transactionName=");
        sb.append(getTransactionName());
        return sb.toString();
    }

    // @VisibleForTesting

    TransactionModuleJMXRegistrator getTxModuleJMXRegistrator() {
        return txModuleJMXRegistrator;
    }

    public TransactionIdentifier getName() {
        return transactionIdentifier;
    }

    @Override
    public List<ModuleFactory> getCurrentlyRegisteredFactories() {
        return currentlyRegisteredFactories;
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return transactionIdentifier;
    }
}
