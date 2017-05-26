/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.RuntimeBeanRegistratorAwareModule;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.dependencyresolver.DestroyedModule;
import org.opendaylight.controller.config.manager.impl.dependencyresolver.ModuleInternalTransactionalInfo;
import org.opendaylight.controller.config.manager.impl.dynamicmbean.DynamicReadableWrapper;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HierarchicalConfigMBeanFactoriesHolder;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.ModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.impl.jmx.BaseJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.ModuleJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.RootRuntimeBeanRegistratorImpl;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.osgi.BeanToOsgiServiceManager;
import org.opendaylight.controller.config.manager.impl.osgi.BeanToOsgiServiceManager.OsgiRegistration;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.BindingContextProvider;
import org.opendaylight.controller.config.manager.impl.util.LookupBeansUtil;
import org.opendaylight.controller.config.manager.impl.util.ModuleQNameUtil;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton that is responsible for creating and committing Config
 * Transactions. It is registered in Platform MBean Server.
 */
@ThreadSafe
public class ConfigRegistryImpl implements AutoCloseable, ConfigRegistryImplMXBean {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigRegistryImpl.class);
    private static final ObjectName NOOP_TX_NAME = ObjectNameUtil.createTransactionControllerON("noop");

    private final ModuleFactoriesResolver resolver;
    private final MBeanServer configMBeanServer;
    private final BindingContextProvider bindingContextProvider;

    private volatile long version = 0;
    private volatile long versionCounter = 0;

    /**
     * Contains current configuration in form of {moduleName:{instanceName,read
     * only module}} for copying state to new transaction. Each running module
     * is present just once, no matter how many interfaces it exposes.
     */
    private final ConfigHolder currentConfig = new ConfigHolder();

    /**
     * Will return true unless there was a transaction that succeeded during
     * validation but failed in second phase of commit. In this case the server
     * is unstable and its state is undefined.
     */
    private volatile boolean isHealthy = true;

    /**
     * Holds Map<transactionName, transactionController> and purges it each time
     * its content is requested.
     */
    private final TransactionsHolder transactionsHolder = new TransactionsHolder();

    private final BaseJMXRegistrator baseJMXRegistrator;

    private final BeanToOsgiServiceManager beanToOsgiServiceManager;

    // internal jmx server for read only beans
    private final MBeanServer registryMBeanServer;
    // internal jmx server shared by all transactions
    private final MBeanServer transactionsMBeanServer;

    private final AtomicBoolean closed = new AtomicBoolean();

    private final Object readableSRRegistryLock = new Object();

    private final Lock configTransactionLock = new ReentrantLock();

    // Used for finding new factory instances for default module functionality
    @GuardedBy("configTransactionLock")
    private List<ModuleFactory> lastListOfFactories = Collections.emptyList();

    // switched in every 2ndPC
    @GuardedBy("readableSRRegistryLock")
    private CloseableServiceReferenceReadableRegistry readableSRRegistry =
            ServiceReferenceRegistryImpl.createInitialSRLookupRegistry();

    // constructor
    public ConfigRegistryImpl(ModuleFactoriesResolver resolver,
                              MBeanServer configMBeanServer, BindingContextProvider bindingContextProvider) {
        this(resolver, configMBeanServer,
                new BaseJMXRegistrator(configMBeanServer), bindingContextProvider);
    }

    // constructor
    public ConfigRegistryImpl(ModuleFactoriesResolver resolver,
                              MBeanServer configMBeanServer,
                              BaseJMXRegistrator baseJMXRegistrator, BindingContextProvider bindingContextProvider) {
        this.resolver = resolver;
        this.beanToOsgiServiceManager = new BeanToOsgiServiceManager();
        this.configMBeanServer = configMBeanServer;
        this.baseJMXRegistrator = baseJMXRegistrator;
        this.bindingContextProvider = bindingContextProvider;
        this.registryMBeanServer = MBeanServerFactory
                .createMBeanServer("ConfigRegistry" + configMBeanServer.getDefaultDomain());
        this.transactionsMBeanServer = MBeanServerFactory
                .createMBeanServer("ConfigTransactions" + configMBeanServer.getDefaultDomain());
    }

    /**
     * Create new {@link ConfigTransactionControllerImpl }
     */
    @Override
    public ObjectName beginConfig() {
        return beginConfig(false);
    }

    /**
     * @param blankTransaction true if this transaction is created automatically by
     *                         org.opendaylight.controller.config.manager.impl.osgi.BlankTransactionServiceTracker
     */
    public ObjectName beginConfig(boolean blankTransaction) {
        // If we're closed or in the process of closing then all modules are destroyed or being destroyed
        // so there's no point in trying to acquire the lock and beginning an actual transaction. Also we want
        // to avoid trying to lock as it may block the shutdown process if there is an outstanding transaction
        // attempting to be committed.
        //
        // We could throw an exception here to indicate this but that's not part of the original API contract
        // and callers may not be expecting an unchecked exception. Therefore we return a special transaction
        // handle that signifies a "no-op".
        if(closed.get()) {
            return NOOP_TX_NAME;
        }

        if(blankTransaction) {
            try {
                // For a "blank" transaction, we'll try to obtain the config lock but "blank" transactions
                // are initiated via OSGi events so we don't want to block indefinitely or for a long period
                // of time.
                if(!configTransactionLock.tryLock(5, TimeUnit.SECONDS)) {
                    LOG.debug("Timed out trying to obtain configTransactionLock");
                    return NOOP_TX_NAME;
                }
            } catch(InterruptedException e) {
                LOG.debug("Interrupted trying to obtain configTransactionLock", e);
                Thread.currentThread().interrupt();
                return NOOP_TX_NAME;
            }
        } else {
            configTransactionLock.lock();
        }

        try {
            return beginConfigSafe(blankTransaction).getControllerObjectName();
        } finally {
            configTransactionLock.unlock();
        }
    }

    @GuardedBy("configTransactionLock")
    private ConfigTransactionControllerInternal beginConfigSafe(boolean blankTransaction) {
        versionCounter++;
        final String transactionName = "ConfigTransaction-" + version + "-" + versionCounter;

        TransactionJMXRegistratorFactory factory = new TransactionJMXRegistratorFactory() {
            @Override
            public TransactionJMXRegistrator create() {
                return baseJMXRegistrator.createTransactionJMXRegistrator(transactionName);
            }
        };

        Map<String, Map.Entry<ModuleFactory, BundleContext>> allCurrentFactories = new HashMap<>(
                resolver.getAllFactories());

        // add all factories that disappeared from SR but are still committed
        for (ModuleInternalInfo moduleInternalInfo : currentConfig.getEntries()) {
            String name = moduleInternalInfo.getModuleFactory().getImplementationName();
            if (!allCurrentFactories.containsKey(name)) {
                LOG.trace("Factory {} not found in SR, using reference from previous commit", name);
                allCurrentFactories.put(name,
                        Maps.immutableEntry(moduleInternalInfo.getModuleFactory(), moduleInternalInfo.getBundleContext()));
            }
        }
        allCurrentFactories = Collections.unmodifiableMap(allCurrentFactories);

        // closed by transaction controller
        ConfigTransactionLookupRegistry txLookupRegistry = new ConfigTransactionLookupRegistry(new TransactionIdentifier(
                transactionName), factory, allCurrentFactories);
        SearchableServiceReferenceWritableRegistry writableRegistry = ServiceReferenceRegistryImpl.createSRWritableRegistry(
                readableSRRegistry, txLookupRegistry, allCurrentFactories);

        ConfigTransactionControllerInternal transactionController = new ConfigTransactionControllerImpl(
                txLookupRegistry, version, bindingContextProvider,
                versionCounter, allCurrentFactories, transactionsMBeanServer,
                configMBeanServer, blankTransaction, writableRegistry);
        try {
            txLookupRegistry.registerMBean(transactionController, transactionController.getControllerObjectName());
        } catch (InstanceAlreadyExistsException e) {
            throw new IllegalStateException(e);
        }
        transactionController.copyExistingModulesAndProcessFactoryDiff(currentConfig.getEntries(), lastListOfFactories);
        transactionsHolder.add(transactionName, transactionController, txLookupRegistry);
        return transactionController;
    }

    /**
     * {@inheritDoc}
     * @throws ConflictingVersionException
     */
    @Override
    public CommitStatus commitConfig(ObjectName transactionControllerON)
            throws ValidationException, ConflictingVersionException {
        if(NOOP_TX_NAME.equals(transactionControllerON) || closed.get()) {
            return new CommitStatus(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        configTransactionLock.lock();
        try {
            return commitConfigSafe(transactionControllerON);
        } finally {
            configTransactionLock.unlock();
        }
    }

    @GuardedBy("configTransactionLock")
    private CommitStatus commitConfigSafe(ObjectName transactionControllerON)
            throws ConflictingVersionException, ValidationException {
        final String transactionName = ObjectNameUtil
                .getTransactionName(transactionControllerON);
        LOG.trace("About to commit {}. Current parentVersion: {}, versionCounter {}", transactionName, version, versionCounter);

        // find ConfigTransactionController
        Map<String, Entry<ConfigTransactionControllerInternal, ConfigTransactionLookupRegistry>> transactions = transactionsHolder.getCurrentTransactions();
        Entry<ConfigTransactionControllerInternal, ConfigTransactionLookupRegistry> configTransactionControllerEntry = transactions.get(transactionName);
        if (configTransactionControllerEntry == null) {
            throw new IllegalArgumentException(String.format(
                    "Transaction with name '%s' not found", transactionName));
        }
        ConfigTransactionControllerInternal configTransactionController = configTransactionControllerEntry.getKey();
        // check optimistic lock
        if (version != configTransactionController.getParentVersion()) {
            throw new ConflictingVersionException(
                    String.format(
                            "Optimistic lock failed. Expected parent version %d, was %d",
                            version,
                            configTransactionController.getParentVersion()));
        }
        // optimistic lock ok

        CommitInfo commitInfo = configTransactionController.validateBeforeCommitAndLockTransaction();
        lastListOfFactories = Collections.unmodifiableList(configTransactionController.getCurrentlyRegisteredFactories());
        // non recoverable from here:
        try {
            return secondPhaseCommit(configTransactionController, commitInfo, configTransactionControllerEntry.getValue());
            // some libs throw Errors: e.g.
            // javax.xml.ws.spi.FactoryFinder$ConfigurationError
        } catch (Throwable t) {
            isHealthy = false;
            LOG.error("Configuration Transaction failed on 2PC, server is unhealthy", t);
            throw Throwables.propagate(t);
        }
    }

    @GuardedBy("configTransactionLock")
    private CommitStatus secondPhaseCommit(ConfigTransactionControllerInternal configTransactionController,
                                           CommitInfo commitInfo, ConfigTransactionLookupRegistry txLookupRegistry) {

        // close instances which were destroyed by the user, including
        // (hopefully) runtime beans
        for (DestroyedModule toBeDestroyed : commitInfo
                .getDestroyedFromPreviousTransactions()) {
            // closes instance (which should close
            // runtime jmx registrator),
            // also closes osgi registration and ModuleJMXRegistrator
            // registration
            toBeDestroyed.close();
            currentConfig.remove(toBeDestroyed.getIdentifier());
        }

        // set RuntimeBeanRegistrators on beans implementing
        // RuntimeBeanRegistratorAwareModule
        Map<ModuleIdentifier, RootRuntimeBeanRegistratorImpl> runtimeRegistrators = new HashMap<>();
        for (ModuleInternalTransactionalInfo entry : commitInfo.getCommitted()
                .values()) {
            // set runtime jmx registrator if required
            Module module = entry.getProxiedModule();
            RootRuntimeBeanRegistratorImpl runtimeBeanRegistrator = null;

            if (module instanceof RuntimeBeanRegistratorAwareModule) {

                if(entry.hasOldModule()) {

                    if(module.canReuse(entry.getOldInternalInfo().getReadableModule().getModule())) {
                        runtimeBeanRegistrator = entry.getOldInternalInfo().getRuntimeBeanRegistrator();
                        ((RuntimeBeanRegistratorAwareModule) module).setRuntimeBeanRegistrator(runtimeBeanRegistrator);
                    } else {
                        runtimeBeanRegistrator = baseJMXRegistrator.createRuntimeBeanRegistrator(entry.getIdentifier());
                        entry.getOldInternalInfo().getRuntimeBeanRegistrator().close();
                        ((RuntimeBeanRegistratorAwareModule) module).setRuntimeBeanRegistrator(runtimeBeanRegistrator);
                    }
                } else {
                    runtimeBeanRegistrator = baseJMXRegistrator.createRuntimeBeanRegistrator(entry.getIdentifier());
                    ((RuntimeBeanRegistratorAwareModule) module).setRuntimeBeanRegistrator(runtimeBeanRegistrator);
                }
            }
            // save it to info so it is accessible afterwards
            if(runtimeBeanRegistrator != null) {
                runtimeRegistrators.put(entry.getIdentifier(), runtimeBeanRegistrator);
            }
        }

        // can register runtime beans
        List<ModuleIdentifier> orderedModuleIdentifiers = configTransactionController.secondPhaseCommit();
        txLookupRegistry.close();
        configTransactionController.close();

        // copy configuration to read only mode
        List<ObjectName> newInstances = new LinkedList<>();
        List<ObjectName> reusedInstances = new LinkedList<>();
        List<ObjectName> recreatedInstances = new LinkedList<>();

        Map<Module, ModuleInternalInfo> newConfigEntries = new HashMap<>();

        int orderingIdx = 0;
        for (ModuleIdentifier moduleIdentifier : orderedModuleIdentifiers) {
            LOG.trace("Registering {}", moduleIdentifier);
            ModuleInternalTransactionalInfo entry = commitInfo.getCommitted()
                    .get(moduleIdentifier);
            if (entry == null) {
                throw new NullPointerException("Module not found "
                        + moduleIdentifier);
            }

            ObjectName primaryReadOnlyON = ObjectNameUtil
                    .createReadOnlyModuleON(moduleIdentifier);

            // determine if current instance was recreated or reused or is new

            // rules for closing resources:
            // osgi registration - will be reused if possible.
            // module jmx registration - will be (re)created every time, needs
            // to be closed here
            // runtime jmx registration - should be taken care of by module
            // itself
            // instance - is closed only if it was destroyed
            ModuleJMXRegistrator newModuleJMXRegistrator = baseJMXRegistrator
                    .createModuleJMXRegistrator();

            OsgiRegistration osgiRegistration = null;
            AutoCloseable instance = entry.getProxiedModule().getInstance();
            if (entry.hasOldModule()) {
                ModuleInternalInfo oldInternalInfo = entry.getOldInternalInfo();
                DynamicReadableWrapper oldReadableConfigBean = oldInternalInfo.getReadableModule();
                currentConfig.remove(entry.getIdentifier());

                // test if old instance == new instance
                if (oldReadableConfigBean.getInstance().equals(instance)) {
                    // reused old instance:
                    // wrap in readable dynamic mbean
                    reusedInstances.add(primaryReadOnlyON);
                    osgiRegistration = oldInternalInfo.getOsgiRegistration();
                } else {
                    // recreated instance:
                    // it is responsibility of module to call the old instance -
                    // we just need to unregister configbean
                    recreatedInstances.add(primaryReadOnlyON);

                    // close old osgi registration
                    oldInternalInfo.getOsgiRegistration().close();
                }

                // close old module jmx registrator
                oldInternalInfo.getModuleJMXRegistrator().close();

                // We no longer need old internal info. Clear it out, so we do not create a serial leak evidenced
                // by BUG-4514. The reason is that modules retain their resolver, which retains modules. If we retain
                // the old module, we would have the complete reconfiguration history held in heap for no good reason.
                entry.clearOldInternalInfo();
            } else {
                // new instance:
                // wrap in readable dynamic mbean
                newInstances.add(primaryReadOnlyON);
            }
            Module realModule = entry.getRealModule();

            DynamicReadableWrapper newReadableConfigBean = new DynamicReadableWrapper(
                    realModule, instance, moduleIdentifier,
                    registryMBeanServer, configMBeanServer);

            // register to JMX
            try {
                newModuleJMXRegistrator.registerMBean(newReadableConfigBean, primaryReadOnlyON);
            } catch (InstanceAlreadyExistsException e) {
                throw new IllegalStateException("Possible code error, already registered:" + primaryReadOnlyON,e);
            }

            // register services to OSGi
            Map<ServiceInterfaceAnnotation, String /* service ref name */> annotationMapping = configTransactionController.getWritableRegistry().findServiceInterfaces(moduleIdentifier);
            BundleContext bc = configTransactionController.getModuleFactoryBundleContext(
                    entry.getModuleFactory().getImplementationName());
            if (osgiRegistration == null) {
                osgiRegistration = beanToOsgiServiceManager.registerToOsgi(
                        newReadableConfigBean.getInstance(), moduleIdentifier, bc, annotationMapping);
            } else {
                osgiRegistration.updateRegistrations(annotationMapping, bc, instance);
            }

            RootRuntimeBeanRegistratorImpl runtimeBeanRegistrator = runtimeRegistrators
                    .get(entry.getIdentifier());
            ModuleInternalInfo newInfo = new ModuleInternalInfo(
                    entry.getIdentifier(), newReadableConfigBean, osgiRegistration,
                    runtimeBeanRegistrator, newModuleJMXRegistrator,
                    orderingIdx, entry.isDefaultBean(), entry.getModuleFactory(), entry.getBundleContext());

            newConfigEntries.put(realModule, newInfo);
            orderingIdx++;
        }
        currentConfig.addAll(newConfigEntries.values());

        // update version
        version = configTransactionController.getVersion();

        // switch readable Service Reference Registry
        synchronized(readableSRRegistryLock) {
            readableSRRegistry.close();
            readableSRRegistry = ServiceReferenceRegistryImpl.createSRReadableRegistry(
                    configTransactionController.getWritableRegistry(), this, baseJMXRegistrator);
        }

        return new CommitStatus(newInstances, reusedInstances,
                recreatedInstances);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ObjectName> getOpenConfigs() {
        Map<String, Entry<ConfigTransactionControllerInternal, ConfigTransactionLookupRegistry>> transactions = transactionsHolder
                .getCurrentTransactions();
        List<ObjectName> result = new ArrayList<>(transactions.size());
        for (Entry<ConfigTransactionControllerInternal, ConfigTransactionLookupRegistry> configTransactionControllerEntry : transactions
                .values()) {
            result.add(configTransactionControllerEntry.getKey().getControllerObjectName());
        }
        return result;
    }

    /**
     * Abort open transactions and unregister read only modules. Since this
     * class is not responsible for registering itself under
     * {@link org.opendaylight.controller.config.api.ConfigRegistry#OBJECT_NAME}, it will not unregister itself
     * here.
     */
    @Override
    public void close() {
        if(!closed.compareAndSet(false, true)) {
            return;
        }

        // abort transactions
        Map<String, Entry<ConfigTransactionControllerInternal, ConfigTransactionLookupRegistry>> transactions = transactionsHolder
                .getCurrentTransactions();
        for (Entry<ConfigTransactionControllerInternal, ConfigTransactionLookupRegistry> configTransactionControllerEntry : transactions
                .values()) {

            ConfigTransactionControllerInternal configTransactionController = configTransactionControllerEntry.getKey();
            try {
                configTransactionControllerEntry.getValue().close();
                configTransactionController.abortConfig();
            } catch (RuntimeException e) {
                LOG.debug("Ignoring exception while aborting {}", configTransactionController, e);
            }
        }

        // destroy all live objects one after another in order of the dependency hierarchy, from top to bottom
        List<DestroyedModule> destroyedModules = currentConfig.getModulesToBeDestroyed();

        LOG.info("ConfigRegistry closing - destroying {} modules", destroyedModules.size());

        for (DestroyedModule destroyedModule : destroyedModules) {
            destroyedModule.close();
        }
        // unregister MBeans that failed to unregister properly
        baseJMXRegistrator.close();
        // remove jmx servers
        MBeanServerFactory.releaseMBeanServer(registryMBeanServer);
        MBeanServerFactory.releaseMBeanServer(transactionsMBeanServer);

        LOG.info("ConfigRegistry closed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getVersion() {
        return version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAvailableModuleNames() {
        return new HierarchicalConfigMBeanFactoriesHolder(
                resolver.getAllFactories()).getModuleNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHealthy() {
        return isHealthy;
    }

    // filtering methods

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
                instanceName);
        return baseJMXRegistrator.queryNames(namePattern, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ObjectName> lookupRuntimeBeans() {
        return lookupRuntimeBeans("*", "*");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ObjectName> lookupRuntimeBeans(String moduleName,
                                              String instanceName) {
        String finalModuleName = moduleName == null ? "*" : moduleName;
        String finalInstanceName = instanceName == null ? "*" : instanceName;
        ObjectName namePattern = ObjectNameUtil.createRuntimeBeanPattern(
                finalModuleName, finalInstanceName);
        return baseJMXRegistrator.queryNames(namePattern, null);
    }

    @Override
    public void checkConfigBeanExists(ObjectName objectName) throws InstanceNotFoundException {
        ObjectNameUtil.checkDomain(objectName);
        ObjectNameUtil.checkType(objectName, ObjectNameUtil.TYPE_MODULE);
        String transactionName = ObjectNameUtil.getTransactionName(objectName);
        if (transactionName != null) {
            throw new IllegalArgumentException("Transaction attribute not supported in registry, wrong ObjectName: " + objectName);
        }
        // make sure exactly one match is found:
        LookupBeansUtil.lookupConfigBean(this, ObjectNameUtil.getFactoryName(objectName), ObjectNameUtil.getInstanceName(objectName));
    }

    // service reference functionality:
    @Override
    public ObjectName lookupConfigBeanByServiceInterfaceName(String serviceInterfaceQName, String refName) {
        synchronized(readableSRRegistryLock) {
            return readableSRRegistry.lookupConfigBeanByServiceInterfaceName(serviceInterfaceQName, refName);
        }
    }

    @Override
    public Map<String, Map<String, ObjectName>> getServiceMapping() {
        synchronized(readableSRRegistryLock) {
            return readableSRRegistry.getServiceMapping();
        }
    }

    @Override
    public Map<String, ObjectName> lookupServiceReferencesByServiceInterfaceName(String serviceInterfaceQName) {
        synchronized(readableSRRegistryLock) {
            return readableSRRegistry.lookupServiceReferencesByServiceInterfaceName(serviceInterfaceQName);
        }
    }

    @Override
    public Set<String> lookupServiceInterfaceNames(ObjectName objectName) throws InstanceNotFoundException {
        synchronized(readableSRRegistryLock) {
            return readableSRRegistry.lookupServiceInterfaceNames(objectName);
        }
    }

    @Override
    public String getServiceInterfaceName(String namespace, String localName) {
        synchronized(readableSRRegistryLock) {
            return readableSRRegistry.getServiceInterfaceName(namespace, localName);
        }
    }

    @Override
    public void checkServiceReferenceExists(ObjectName objectName) throws InstanceNotFoundException {
        synchronized(readableSRRegistryLock) {
            readableSRRegistry.checkServiceReferenceExists(objectName);
        }
    }

    @Override
    public ObjectName getServiceReference(String serviceInterfaceQName, String refName) throws InstanceNotFoundException {
        synchronized(readableSRRegistryLock) {
            return readableSRRegistry.getServiceReference(serviceInterfaceQName, refName);
        }
    }

    @Override
    public Set<String> getAvailableModuleFactoryQNames() {
        return ModuleQNameUtil.getQNames(resolver.getAllFactories());
    }

    @Override
    public String toString() {
        return "ConfigRegistryImpl{" +
                "versionCounter=" + versionCounter +
                ", version=" + version +
                '}';
    }
}

/**
 * Holds currently running modules
 */
class ConfigHolder {
    private final ConcurrentMap<ModuleIdentifier, ModuleInternalInfo> currentConfig = new ConcurrentHashMap<>();

    /**
     * Add all modules to the internal map. Also add service instance to OSGi
     * Service Registry.
     */
    public void addAll(Collection<ModuleInternalInfo> configInfos) {
        if (!currentConfig.isEmpty()) {
            throw new IllegalStateException(
                    "Error - some config entries were not removed: "
                            + currentConfig);
        }
        for (ModuleInternalInfo configInfo : configInfos) {
            add(configInfo);
        }
    }

    private void add(ModuleInternalInfo configInfo) {
        ModuleInternalInfo oldValue = currentConfig.putIfAbsent(configInfo.getIdentifier(), configInfo);
        if (oldValue != null) {
            throw new IllegalStateException(
                    "Cannot overwrite module with same name:"
                            + configInfo.getIdentifier() + ":" + configInfo);
        }
    }

    /**
     * Remove entry from current config.
     */
    public void remove(ModuleIdentifier name) {
        ModuleInternalInfo removed = currentConfig.remove(name);
        if (removed == null) {
            throw new IllegalStateException(
                    "Cannot remove from ConfigHolder - name not found:" + name);
        }
    }

    public Collection<ModuleInternalInfo> getEntries() {
        return currentConfig.values();
    }

    public List<DestroyedModule> getModulesToBeDestroyed() {
        List<DestroyedModule> result = new ArrayList<>();
        for (ModuleInternalInfo moduleInternalInfo : getEntries()) {
            result.add(moduleInternalInfo.toDestroyedModule());
        }
        Collections.sort(result);
        return result;
    }


}

/**
 * Holds Map<transactionName, transactionController> and purges it each time its
 * content is requested.
 */
class TransactionsHolder {
    /**
     * This map keeps transaction names and
     * {@link ConfigTransactionControllerInternal} instances, because platform
     * MBeanServer transforms mbeans into another representation. Map is cleaned
     * every time current transactions are requested.
     */
    private final ConcurrentMap<String /* transactionName */,
            Entry<ConfigTransactionControllerInternal, ConfigTransactionLookupRegistry>> transactions = new ConcurrentHashMap<>();

    public void add(String transactionName,
                    ConfigTransactionControllerInternal transactionController, ConfigTransactionLookupRegistry txLookupRegistry) {
        Object oldValue = transactions.putIfAbsent(transactionName,
                Maps.immutableEntry(transactionController, txLookupRegistry));
        if (oldValue != null) {
            throw new IllegalStateException(
                    "Error: two transactions with same name");
        }
    }

    /**
     * Purges closed transactions from transactions map. Calling this method more than once within the
     * method can modify the resulting map that was obtained in previous calls.
     *
     * @return current view on transactions map.
     */
    public Map<String, Entry<ConfigTransactionControllerInternal, ConfigTransactionLookupRegistry>> getCurrentTransactions() {
        // first, remove closed transaction
        for (Iterator<Entry<String, Entry<ConfigTransactionControllerInternal, ConfigTransactionLookupRegistry>>> it = transactions
                .entrySet().iterator(); it.hasNext(); ) {
            Entry<String, Entry<ConfigTransactionControllerInternal, ConfigTransactionLookupRegistry>> entry = it
                    .next();
            if (entry.getValue().getKey().isClosed()) {
                it.remove();
            }
        }
        return Collections.unmodifiableMap(transactions);
    }
}
