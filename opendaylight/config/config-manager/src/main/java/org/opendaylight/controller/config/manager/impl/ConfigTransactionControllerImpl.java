/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.dependencyresolver.DependencyResolverManager;
import org.opendaylight.controller.config.manager.impl.dependencyresolver.ModuleInternalTransactionalInfo;
import org.opendaylight.controller.config.manager.impl.dynamicmbean.DynamicWritableWrapper;
import org.opendaylight.controller.config.manager.impl.dynamicmbean.ReadOnlyAtomicBoolean;
import org.opendaylight.controller.config.manager.impl.dynamicmbean.ReadOnlyAtomicBoolean.ReadOnlyAtomicBooleanImpl;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HierarchicalConfigMBeanFactoriesHolder;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionModuleJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionModuleJMXRegistrator.TransactionModuleJMXRegistration;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * This is a JMX bean representing current transaction. It contains
 * transaction identifier, unique version and parent version for
 * optimistic locking.
 */
class ConfigTransactionControllerImpl implements
        ConfigTransactionControllerInternal,
        ConfigTransactionControllerImplMXBean,
        Identifiable<TransactionIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigTransactionControllerImpl.class);

    private final ConfigTransactionLookupRegistry txLookupRegistry;
    private final ObjectName controllerON;

    private final long parentVersion, currentVersion;
    private final HierarchicalConfigMBeanFactoriesHolder factoriesHolder;
    private final DependencyResolverManager dependencyResolverManager;
    private final TransactionStatus transactionStatus;
    private final MBeanServer transactionsMBeanServer;
    private final Map<String, Map.Entry<ModuleFactory, BundleContext>> currentlyRegisteredFactories;

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

    private final boolean blankTransaction;

    @GuardedBy("this")
    private final SearchableServiceReferenceWritableRegistry writableSRRegistry;

    public ConfigTransactionControllerImpl(ConfigTransactionLookupRegistry txLookupRegistry,
                                           long parentVersion, CodecRegistry codecRegistry, long currentVersion,
                                           Map<String, Entry<ModuleFactory, BundleContext>> currentlyRegisteredFactories,
                                           MBeanServer transactionsMBeanServer, MBeanServer configMBeanServer,
                                           boolean blankTransaction, SearchableServiceReferenceWritableRegistry  writableSRRegistry) {
        this.txLookupRegistry = txLookupRegistry;
        String transactionName = txLookupRegistry.getTransactionIdentifier().getName();
        this.controllerON = ObjectNameUtil.createTransactionControllerON(transactionName);
        this.parentVersion = parentVersion;
        this.currentVersion = currentVersion;
        this.currentlyRegisteredFactories = currentlyRegisteredFactories;
        this.factoriesHolder = new HierarchicalConfigMBeanFactoriesHolder(currentlyRegisteredFactories);
        this.transactionStatus = new TransactionStatus();
        this.dependencyResolverManager = new DependencyResolverManager(txLookupRegistry.getTransactionIdentifier(),
                transactionStatus, writableSRRegistry, codecRegistry, transactionsMBeanServer);
        this.transactionsMBeanServer = transactionsMBeanServer;
        this.configMBeanServer = configMBeanServer;
        this.blankTransaction = blankTransaction;
        this.writableSRRegistry = writableSRRegistry;
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
        Set<ModuleFactory> newSet = new HashSet<>(factoriesHolder.getModuleFactories());

        List<ModuleFactory> toBeAdded = new ArrayList<>();
        List<ModuleFactory> toBeRemoved = new ArrayList<>();
        for (ModuleFactory moduleFactory : factoriesHolder.getModuleFactories()) {
            if (oldSet.contains(moduleFactory) == false) {
                toBeAdded.add(moduleFactory);
            }
        }
        for (ModuleFactory moduleFactory : lastListOfFactories) {
            if (newSet.contains(moduleFactory) == false) {
                toBeRemoved.add(moduleFactory);
            }
        }
        // add default modules
        for (ModuleFactory moduleFactory : toBeAdded) {
            BundleContext bundleContext = getModuleFactoryBundleContext(moduleFactory.getImplementationName());
            Set<? extends Module> defaultModules = moduleFactory.getDefaultModules(dependencyResolverManager,
                    bundleContext);
            for (Module module : defaultModules) {
                // ensure default module to be registered to jmx even if its module factory does not use dependencyResolverFactory
                DependencyResolver dependencyResolver = dependencyResolverManager.getOrCreate(module.getIdentifier());
                try {
                    boolean defaultBean = true;
                    putConfigBeanToJMXAndInternalMaps(module.getIdentifier(), module, moduleFactory, null,
                            dependencyResolver, defaultBean, bundleContext);
                } catch (InstanceAlreadyExistsException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        // remove modules belonging to removed factories
        for (ModuleFactory removedFactory : toBeRemoved) {
            List<ModuleIdentifier> modulesOfRemovedFactory = dependencyResolverManager.findAllByFactory(removedFactory);
            for (ModuleIdentifier name : modulesOfRemovedFactory) {
                destroyModule(name);
            }
        }
    }


    private synchronized void copyExistingModule(ModuleInternalInfo oldConfigBeanInfo) throws InstanceAlreadyExistsException {

        transactionStatus.checkNotCommitStarted();
        transactionStatus.checkNotAborted();
        ModuleIdentifier moduleIdentifier = oldConfigBeanInfo.getIdentifier();
        dependencyResolverManager.assertNotExists(moduleIdentifier);

        ModuleFactory moduleFactory;
        BundleContext bc;
        try {
            moduleFactory = factoriesHolder.findByModuleName(moduleIdentifier.getFactoryName());
            bc = getModuleFactoryBundleContext(moduleFactory.getImplementationName());
        } catch (InstanceNotFoundException e) {
            throw new IllegalStateException(e);
        }

        Module module;
        DependencyResolver dependencyResolver = dependencyResolverManager.getOrCreate(moduleIdentifier);
        try {

            module = moduleFactory.createModule(
                    moduleIdentifier.getInstanceName(), dependencyResolver,
                    oldConfigBeanInfo.getReadableModule(), bc);
        } catch (Exception e) {
            throw new IllegalStateException(format(
                    "Error while copying old configuration from %s to %s",
                    oldConfigBeanInfo, moduleFactory), e);
        }
        putConfigBeanToJMXAndInternalMaps(moduleIdentifier, module, moduleFactory, oldConfigBeanInfo, dependencyResolver,
                oldConfigBeanInfo.isDefaultBean(), bc);
    }

    @Override
    public synchronized ObjectName createModule(String factoryName,
                                                String instanceName) throws InstanceAlreadyExistsException {

        transactionStatus.checkNotCommitStarted();
        transactionStatus.checkNotAborted();
        ModuleIdentifier moduleIdentifier = new ModuleIdentifier(factoryName, instanceName);
        dependencyResolverManager.assertNotExists(moduleIdentifier);

        // find factory
        ModuleFactory moduleFactory;
        try {
            moduleFactory = factoriesHolder.findByModuleName(factoryName);
        } catch (InstanceNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        DependencyResolver dependencyResolver = dependencyResolverManager.getOrCreate(moduleIdentifier);
        BundleContext bundleContext = getModuleFactoryBundleContext(moduleFactory.getImplementationName());
        Module module = moduleFactory.createModule(instanceName, dependencyResolver,
                bundleContext);
        boolean defaultBean = false;
        return putConfigBeanToJMXAndInternalMaps(moduleIdentifier, module,
                moduleFactory, null, dependencyResolver, defaultBean, bundleContext);
    }

    private synchronized ObjectName putConfigBeanToJMXAndInternalMaps(
            ModuleIdentifier moduleIdentifier, Module module,
            ModuleFactory moduleFactory,
            @Nullable ModuleInternalInfo maybeOldConfigBeanInfo, DependencyResolver dependencyResolver,
            boolean isDefaultBean, BundleContext bundleContext)
            throws InstanceAlreadyExistsException {

        LOG.debug("Adding module {} to transaction {}", moduleIdentifier, this);
        if (moduleIdentifier.equals(module.getIdentifier()) == false) {
            throw new IllegalStateException("Incorrect name reported by module. Expected "
                    + moduleIdentifier + ", got " + module.getIdentifier());
        }
        if (dependencyResolver.getIdentifier().equals(moduleIdentifier) == false) {
            throw new IllegalStateException("Incorrect name reported by dependency resolver. Expected "
                    + moduleIdentifier + ", got " + dependencyResolver.getIdentifier());
        }
        DynamicMBean writableDynamicWrapper = new DynamicWritableWrapper(
                module, moduleIdentifier, getTransactionIdentifier(),
                readOnlyAtomicBoolean, transactionsMBeanServer,
                configMBeanServer);

        ObjectName writableON = ObjectNameUtil.createTransactionModuleON(
                getTransactionIdentifier().getName(), moduleIdentifier);
        // put wrapper to jmx
        TransactionModuleJMXRegistration transactionModuleJMXRegistration = getTxModuleJMXRegistrator()
                .registerMBean(writableDynamicWrapper, writableON);

        dependencyResolverManager.put(
                moduleIdentifier, module, moduleFactory,
                maybeOldConfigBeanInfo, transactionModuleJMXRegistration, isDefaultBean, bundleContext);
        return writableON;
    }

    @Override
    public synchronized void destroyModule(ObjectName objectName) throws InstanceNotFoundException {
        checkTransactionName(objectName);
        ObjectNameUtil.checkDomain(objectName);
        ModuleIdentifier moduleIdentifier = ObjectNameUtil.fromON(objectName,
                ObjectNameUtil.TYPE_MODULE);
        destroyModule(moduleIdentifier);
    }

    private void checkTransactionName(ObjectName objectName) {
        String foundTransactionName = ObjectNameUtil
                .getTransactionName(objectName);
        if (getTransactionIdentifier().getName().equals(foundTransactionName) == false) {
            throw new IllegalArgumentException("Wrong transaction name "
                    + objectName);
        }
    }

    private synchronized void destroyModule(ModuleIdentifier moduleIdentifier) {
        LOG.debug("Destroying module {} in transaction {}", moduleIdentifier, this);
        transactionStatus.checkNotAborted();

        ModuleInternalTransactionalInfo found = dependencyResolverManager.findModuleInternalTransactionalInfo(moduleIdentifier);
        if (blankTransaction == false &&
                found.isDefaultBean()) {
            LOG.warn("Warning: removing default bean. This will be forbidden in next version of config-subsystem");
        }
        // first remove refNames, it checks for objectname existence

        try {
            writableSRRegistry.removeServiceReferences(
                    ObjectNameUtil.createTransactionModuleON(getTransactionName(), moduleIdentifier));
        } catch (InstanceNotFoundException e) {
            LOG.error("Possible code error: cannot find {} in {}", moduleIdentifier, writableSRRegistry);
            throw new IllegalStateException("Possible code error: cannot find " + moduleIdentifier, e);
        }

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
        if (configBeanModificationDisabled.get()) {
            throw new IllegalStateException("Cannot start validation");
        }
        configBeanModificationDisabled.set(true);
        try {
            validateNoLocks();
        } finally {
            configBeanModificationDisabled.set(false);
        }
    }

    private void validateNoLocks() throws ValidationException {
        transactionStatus.checkNotAborted();
        LOG.trace("Validating transaction {}", getTransactionIdentifier());
        // call validate()
        List<ValidationException> collectedExceptions = new ArrayList<>();
        for (Entry<ModuleIdentifier, Module> entry : dependencyResolverManager
                .getAllModules().entrySet()) {
            ModuleIdentifier name = entry.getKey();
            Module module = entry.getValue();
            try {
                module.validate();
            } catch (Exception e) {
                LOG.warn("Validation exception in {}", getTransactionName(),
                        e);
                collectedExceptions.add(ValidationException
                        .createForSingleException(name, e));
            }
        }
        if (!collectedExceptions.isEmpty()) {
            throw ValidationException
                    .createFromCollectedValidationExceptions(collectedExceptions);
        }
        LOG.trace("Validated transaction {}", getTransactionIdentifier());
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
            validateNoLocks();
        } catch (ValidationException e) {
            LOG.trace("Commit failed on validation");
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

        LOG.trace("Committing transaction {}", getTransactionIdentifier());

        Map<ModuleIdentifier, Module> allModules = dependencyResolverManager.getAllModules();

        // call getInstance() on all Modules from top to bottom (from source to target of the dependency relation)
        // The source of a dependency closes itself and calls getInstance recursively on the dependencies (in case of reconfiguration)
        // This makes close() calls from top to bottom while createInstance() calls are performed bottom to top
        List<ModuleIdentifier> sortedModuleIdentifiers = Lists.reverse(dependencyResolverManager.getSortedModuleIdentifiers());
        for (ModuleIdentifier moduleIdentifier : sortedModuleIdentifiers) {
            Module module = allModules.get(moduleIdentifier);

            try {
                LOG.debug("About to commit {} in transaction {}",
                        moduleIdentifier, getTransactionIdentifier());
                AutoCloseable instance = module.getInstance();
                checkNotNull(instance, "Instance is null:{} in transaction {}", moduleIdentifier, getTransactionIdentifier());
            } catch (Exception e) {
                LOG.error("Commit failed on {} in transaction {}", moduleIdentifier,
                        getTransactionIdentifier(), e);
                internalAbort();
                throw new IllegalStateException(
                        format("Error - getInstance() failed for %s in transaction %s",
                                moduleIdentifier, getTransactionIdentifier()), e);
            }
        }

        LOG.trace("Committed configuration {}", getTransactionIdentifier());
        transactionStatus.setCommitted();

        return sortedModuleIdentifiers;
    }

    @Override
    public synchronized void abortConfig() {
        transactionStatus.checkNotCommitStarted();
        transactionStatus.checkNotAborted();
        internalAbort();
    }

    private void internalAbort() {
        LOG.trace("Aborting {}", this);
        transactionStatus.setAborted();
        close();
    }

    public void close() {
        dependencyResolverManager.close();
        txLookupRegistry.close();
    }

    @Override
    public ObjectName getControllerObjectName() {
        return controllerON;
    }

    @Override
    public String getTransactionName() {
        return getTransactionIdentifier().getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ObjectName> lookupConfigBeans() {
        return txLookupRegistry.lookupConfigBeans();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName) {
        return txLookupRegistry.lookupConfigBeans(moduleName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectName lookupConfigBean(String moduleName, String instanceName)
            throws InstanceNotFoundException {
        return txLookupRegistry.lookupConfigBean(moduleName, instanceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName, String instanceName) {
        return txLookupRegistry.lookupConfigBeans(moduleName, instanceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkConfigBeanExists(ObjectName objectName) throws InstanceNotFoundException {
        txLookupRegistry.checkConfigBeanExists(objectName);
    }
    // --

    /**
     * {@inheritDoc}
     */
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
        return txLookupRegistry.getTxModuleJMXRegistrator();
    }

    public TransactionIdentifier getName() {
        return getTransactionIdentifier();
    }

    @Override
    public List<ModuleFactory> getCurrentlyRegisteredFactories() {
        return new ArrayList<>(factoriesHolder.getModuleFactories());
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return getTransactionIdentifier();
    }

    @Override
    public BundleContext getModuleFactoryBundleContext(String factoryName) {
        Map.Entry<ModuleFactory, BundleContext> factoryBundleContextEntry = this.currentlyRegisteredFactories.get(factoryName);
        if (factoryBundleContextEntry == null || factoryBundleContextEntry.getValue() == null) {
            throw new NullPointerException("Bundle context of " + factoryName + " ModuleFactory not found.");
        }
        return factoryBundleContextEntry.getValue();
    }

    // service reference functionality:


    @Override
    public synchronized ObjectName lookupConfigBeanByServiceInterfaceName(String serviceInterfaceQName, String refName) {
        return writableSRRegistry.lookupConfigBeanByServiceInterfaceName(serviceInterfaceQName, refName);
    }

    @Override
    public synchronized Map<String, Map<String, ObjectName>> getServiceMapping() {
        return writableSRRegistry.getServiceMapping();
    }

    @Override
    public synchronized Map<String, ObjectName> lookupServiceReferencesByServiceInterfaceName(String serviceInterfaceQName) {
        return writableSRRegistry.lookupServiceReferencesByServiceInterfaceName(serviceInterfaceQName);
    }

    @Override
    public synchronized Set<String> lookupServiceInterfaceNames(ObjectName objectName) throws InstanceNotFoundException {
        return writableSRRegistry.lookupServiceInterfaceNames(objectName);
    }

    @Override
    public synchronized String getServiceInterfaceName(String namespace, String localName) {
        return writableSRRegistry.getServiceInterfaceName(namespace, localName);
    }

    @Override
    public synchronized ObjectName saveServiceReference(String serviceInterfaceName, String refName, ObjectName moduleON) throws InstanceNotFoundException {
        return writableSRRegistry.saveServiceReference(serviceInterfaceName, refName, moduleON);
    }

    @Override
    public synchronized void removeServiceReference(String serviceInterfaceName, String refName) throws InstanceNotFoundException {
        writableSRRegistry.removeServiceReference(serviceInterfaceName, refName);
    }

    @Override
    public synchronized void removeAllServiceReferences() {
        writableSRRegistry.removeAllServiceReferences();
    }

    @Override
    public boolean removeServiceReferences(ObjectName objectName) throws InstanceNotFoundException {
        return writableSRRegistry.removeServiceReferences(objectName);
    }

    @Override
    public SearchableServiceReferenceWritableRegistry  getWritableRegistry() {
        return writableSRRegistry;
    }

    @Override
    public TransactionIdentifier getTransactionIdentifier() {
        return txLookupRegistry.getTransactionIdentifier();
    }

    @Override
    public Set<String> getAvailableModuleFactoryQNames() {
        return txLookupRegistry.getAvailableModuleFactoryQNames();
    }

    @Override
    public void checkServiceReferenceExists(ObjectName objectName) throws InstanceNotFoundException {
        writableSRRegistry.checkServiceReferenceExists(objectName);
    }

    @Override
    public ObjectName getServiceReference(String serviceInterfaceQName, String refName) throws InstanceNotFoundException {
        return writableSRRegistry.getServiceReference(serviceInterfaceQName, refName);
    }
}
