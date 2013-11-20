/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.RuntimeBeanRegistratorAwareModule;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.dynamicmbean.DynamicReadableWrapper;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HierarchicalConfigMBeanFactoriesHolder;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.ModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.impl.jmx.BaseJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.ModuleJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.RootRuntimeBeanRegistratorImpl;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.osgi.BeanToOsgiServiceManager;
import org.opendaylight.controller.config.manager.impl.osgi.BeanToOsgiServiceManager.OsgiRegistration;
import org.opendaylight.controller.config.manager.impl.util.LookupBeansUtil;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
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

/**
 * Singleton that is responsible for creating and committing Config
 * Transactions. It is registered in Platform MBean Server.
 */
@ThreadSafe
public class ConfigRegistryImpl implements AutoCloseable, ConfigRegistryImplMXBean {
    private static final Logger logger = LoggerFactory.getLogger(ConfigRegistryImpl.class);

    private final ModuleFactoriesResolver resolver;
    private final MBeanServer configMBeanServer;

    @GuardedBy("this")
    private final BundleContext bundleContext;

    @GuardedBy("this")
    private long version = 0;
    @GuardedBy("this")
    private long versionCounter = 0;

    /**
     * Contains current configuration in form of {moduleName:{instanceName,read
     * only module}} for copying state to new transaction. Each running module
     * is present just once, no matter how many interfaces it exposes.
     */
    @GuardedBy("this")
    private final ConfigHolder currentConfig = new ConfigHolder();

    /**
     * Will return true unless there was a transaction that succeeded during
     * validation but failed in second phase of commit. In this case the server
     * is unstable and its state is undefined.
     */
    @GuardedBy("this")
    private boolean isHealthy = true;

    /**
     * Holds Map<transactionName, transactionController> and purges it each time
     * its content is requested.
     */
    @GuardedBy("this")
    private final TransactionsHolder transactionsHolder = new TransactionsHolder();

    private final BaseJMXRegistrator baseJMXRegistrator;

    private final BeanToOsgiServiceManager beanToOsgiServiceManager;

    // internal jmx server for read only beans
    private final MBeanServer registryMBeanServer;
    // internal jmx server shared by all transactions
    private final MBeanServer transactionsMBeanServer;

    @GuardedBy("this")
    private List<ModuleFactory> lastListOfFactories = Collections.emptyList();

    // constructor
    public ConfigRegistryImpl(ModuleFactoriesResolver resolver,
            BundleContext bundleContext, MBeanServer configMBeanServer) {
        this(resolver, bundleContext, configMBeanServer,
                new BaseJMXRegistrator(configMBeanServer));
    }

    // constructor
    public ConfigRegistryImpl(ModuleFactoriesResolver resolver,
            BundleContext bundleContext, MBeanServer configMBeanServer,
            BaseJMXRegistrator baseJMXRegistrator) {
        this.resolver = resolver;
        this.beanToOsgiServiceManager = new BeanToOsgiServiceManager(
                bundleContext);
        this.bundleContext = bundleContext;
        this.configMBeanServer = configMBeanServer;
        this.baseJMXRegistrator = baseJMXRegistrator;
        this.registryMBeanServer = MBeanServerFactory
                .createMBeanServer("ConfigRegistry" + configMBeanServer.getDefaultDomain());
        this.transactionsMBeanServer = MBeanServerFactory
                .createMBeanServer("ConfigTransactions" + configMBeanServer.getDefaultDomain());
    }

    /**
     * Create new {@link ConfigTransactionControllerImpl }
     */
    @Override
    public synchronized ObjectName beginConfig() {
        return beginConfigInternal().getControllerObjectName();
    }

    private synchronized ConfigTransactionControllerInternal beginConfigInternal() {
        versionCounter++;
        String transactionName = "ConfigTransaction-" + version + "-" + versionCounter;
        TransactionJMXRegistrator transactionRegistrator = baseJMXRegistrator
                .createTransactionJMXRegistrator(transactionName);
        List<ModuleFactory> allCurrentFactories = Collections.unmodifiableList(resolver.getAllFactories());
        ConfigTransactionControllerInternal transactionController = new ConfigTransactionControllerImpl(
                transactionName, transactionRegistrator, version,
                versionCounter, allCurrentFactories, transactionsMBeanServer, configMBeanServer, bundleContext);
        try {
            transactionRegistrator.registerMBean(transactionController, transactionController.getControllerObjectName());
        } catch (InstanceAlreadyExistsException e) {
            throw new IllegalStateException(e);
        }

        transactionController.copyExistingModulesAndProcessFactoryDiff(currentConfig.getEntries(), lastListOfFactories);

        transactionsHolder.add(transactionName, transactionController);
        return transactionController;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized CommitStatus commitConfig(ObjectName transactionControllerON)
            throws ConflictingVersionException, ValidationException {
        final String transactionName = ObjectNameUtil
                .getTransactionName(transactionControllerON);
        logger.info("About to commit {}. Current parentVersion: {}, versionCounter {}", transactionName, version, versionCounter);

        // find ConfigTransactionController
        Map<String, ConfigTransactionControllerInternal> transactions = transactionsHolder.getCurrentTransactions();
        ConfigTransactionControllerInternal configTransactionController = transactions.get(transactionName);
        if (configTransactionController == null) {
            throw new IllegalArgumentException(String.format(
                    "Transaction with name '%s' not found", transactionName));
        }
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
            return secondPhaseCommit(
                    configTransactionController, commitInfo);
        } catch (Throwable t) { // some libs throw Errors: e.g.
                                // javax.xml.ws.spi.FactoryFinder$ConfigurationError
            isHealthy = false;
            logger.error("Configuration Transaction failed on 2PC, server is unhealthy", t);
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new RuntimeException(t);
            }
        }
    }

    private CommitStatus secondPhaseCommit(ConfigTransactionControllerInternal configTransactionController,
                                           CommitInfo commitInfo) {

        // close instances which were destroyed by the user, including
        // (hopefully) runtime beans
        for (DestroyedModule toBeDestroyed : commitInfo
                .getDestroyedFromPreviousTransactions()) {
            toBeDestroyed.close(); // closes instance (which should close
                                   // runtime jmx registrator),
            // also closes osgi registration and ModuleJMXRegistrator
            // registration
            currentConfig.remove(toBeDestroyed.getIdentifier());
        }

        // set RuntimeBeanRegistrators on beans implementing
        // RuntimeBeanRegistratorAwareModule, each module
        // should have exactly one runtime jmx registrator.
        Map<ModuleIdentifier, RootRuntimeBeanRegistratorImpl> runtimeRegistrators = new HashMap<>();
        for (ModuleInternalTransactionalInfo entry : commitInfo.getCommitted()
                .values()) {
            RootRuntimeBeanRegistratorImpl runtimeBeanRegistrator;
            if (entry.hasOldModule() == false) {
                runtimeBeanRegistrator = baseJMXRegistrator
                        .createRuntimeBeanRegistrator(entry.getName());
            } else {
                // reuse old JMX registrator
                runtimeBeanRegistrator = entry.getOldInternalInfo()
                        .getRuntimeBeanRegistrator();
            }
            // set runtime jmx registrator if required
            Module module = entry.getModule();
            if (module instanceof RuntimeBeanRegistratorAwareModule) {
                ((RuntimeBeanRegistratorAwareModule) module)
                        .setRuntimeBeanRegistrator(runtimeBeanRegistrator);
            }
            // save it to info so it is accessible afterwards
            runtimeRegistrators.put(entry.getName(), runtimeBeanRegistrator);
        }

        // can register runtime beans
        List<ModuleIdentifier> orderedModuleIdentifiers = configTransactionController
                .secondPhaseCommit();

        // copy configuration to read only mode
        List<ObjectName> newInstances = new LinkedList<>();
        List<ObjectName> reusedInstances = new LinkedList<>();
        List<ObjectName> recreatedInstances = new LinkedList<>();

        Map<Module, ModuleInternalInfo> newConfigEntries = new HashMap<>();

        int orderingIdx = 0;
        for (ModuleIdentifier moduleIdentifier : orderedModuleIdentifiers) {
            ModuleInternalTransactionalInfo entry = commitInfo.getCommitted()
                    .get(moduleIdentifier);
            if (entry == null)
                throw new NullPointerException("Module not found "
                        + moduleIdentifier);
            Module module = entry.getModule();
            ObjectName primaryReadOnlyON = ObjectNameUtil
                    .createReadOnlyModuleON(moduleIdentifier);

            // determine if current instance was recreated or reused or is new

            // rules for closing resources:
            // osgi registration - will be (re)created every time, so it needs
            // to be closed here
            // module jmx registration - will be (re)created every time, needs
            // to be closed here
            // runtime jmx registration - should be taken care of by module
            // itself
            // instance - is closed only if it was destroyed
            ModuleJMXRegistrator newModuleJMXRegistrator = baseJMXRegistrator
                    .createModuleJMXRegistrator();

            OsgiRegistration osgiRegistration = null;
            if (entry.hasOldModule()) {
                ModuleInternalInfo oldInternalInfo = entry.getOldInternalInfo();
                DynamicReadableWrapper oldReadableConfigBean = oldInternalInfo
                        .getReadableModule();
                currentConfig.remove(entry.getName());

                // test if old instance == new instance
                if (oldReadableConfigBean.getInstance().equals(module.getInstance())) {
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
            } else {
                // new instance:
                // wrap in readable dynamic mbean
                newInstances.add(primaryReadOnlyON);
            }

            DynamicReadableWrapper newReadableConfigBean = new DynamicReadableWrapper(
                    module, module.getInstance(), moduleIdentifier,
                    registryMBeanServer, configMBeanServer);

            // register to JMX
            try {
                newModuleJMXRegistrator.registerMBean(newReadableConfigBean,
                        primaryReadOnlyON);
            } catch (InstanceAlreadyExistsException e) {
                throw new IllegalStateException(e);
            }

            // register to OSGi
            if (osgiRegistration == null) {
                osgiRegistration = beanToOsgiServiceManager.registerToOsgi(module.getClass(),
                        newReadableConfigBean.getInstance(), entry.getName());
            }

            RootRuntimeBeanRegistratorImpl runtimeBeanRegistrator = runtimeRegistrators
                    .get(entry.getName());
            ModuleInternalInfo newInfo = new ModuleInternalInfo(
                    entry.getName(), newReadableConfigBean, osgiRegistration,
                    runtimeBeanRegistrator, newModuleJMXRegistrator,
                    orderingIdx);

            newConfigEntries.put(module, newInfo);
            orderingIdx++;
        }
        currentConfig.addAll(newConfigEntries.values());

        // update version
        version = configTransactionController.getVersion();
        return new CommitStatus(newInstances, reusedInstances,
                recreatedInstances);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<ObjectName> getOpenConfigs() {
        Map<String, ConfigTransactionControllerInternal> transactions = transactionsHolder
                .getCurrentTransactions();
        List<ObjectName> result = new ArrayList<>(transactions.size());
        for (ConfigTransactionControllerInternal configTransactionController : transactions
                .values()) {
            result.add(configTransactionController.getControllerObjectName());
        }
        return result;
    }

    /**
     * Abort open transactions and unregister read only modules. Since this
     * class is not responsible for registering itself under
     * {@link ConfigRegistryMXBean#OBJECT_NAME}, it will not unregister itself
     * here.
     */
    @Override
    public synchronized void close() {
        // abort transactions
        Map<String, ConfigTransactionControllerInternal> transactions = transactionsHolder
                .getCurrentTransactions();
        for (ConfigTransactionControllerInternal configTransactionController : transactions
                .values()) {
            try {
                configTransactionController.abortConfig();
            } catch (RuntimeException e) {
                logger.warn("Ignoring exception while aborting {}",
                        configTransactionController, e);
            }
        }

        // destroy all live objects one after another in order of the dependency
        // hierarchy
        List<DestroyedModule> destroyedModules = currentConfig
                .getModulesToBeDestroyed();
        for (DestroyedModule destroyedModule : destroyedModules) {
            destroyedModule.close();
        }
        // unregister MBeans that failed to unregister properly
        baseJMXRegistrator.close();
        // remove jmx servers
        MBeanServerFactory.releaseMBeanServer(registryMBeanServer);
        MBeanServerFactory.releaseMBeanServer(transactionsMBeanServer);

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
        if (moduleName == null)
            moduleName = "*";
        if (instanceName == null)
            instanceName = "*";
        ObjectName namePattern = ObjectNameUtil.createRuntimeBeanPattern(
                moduleName, instanceName);
        return baseJMXRegistrator.queryNames(namePattern, null);
    }

}

/**
 * Holds currently running modules
 */
@NotThreadSafe
class ConfigHolder {
    private final Map<ModuleIdentifier, ModuleInternalInfo> currentConfig = new HashMap<>();

    /**
     * Add all modules to the internal map. Also add service instance to OSGi
     * Service Registry.
     */
    public void addAll(Collection<ModuleInternalInfo> configInfos) {
        if (currentConfig.size() > 0) {
            throw new IllegalStateException(
                    "Error - some config entries were not removed: "
                            + currentConfig);
        }
        for (ModuleInternalInfo configInfo : configInfos) {
            add(configInfo);
        }
    }

    private void add(ModuleInternalInfo configInfo) {
        ModuleInternalInfo oldValue = currentConfig.put(configInfo.getName(),
                configInfo);
        if (oldValue != null) {
            throw new IllegalStateException(
                    "Cannot overwrite module with same name:"
                            + configInfo.getName() + ":" + configInfo);
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
@NotThreadSafe
class TransactionsHolder {
    /**
     * This map keeps transaction names and
     * {@link ConfigTransactionControllerInternal} instances, because platform
     * MBeanServer transforms mbeans into another representation. Map is cleaned
     * every time current transactions are requested.
     *
     */
    @GuardedBy("ConfigRegistryImpl.this")
    private final Map<String /* transactionName */, ConfigTransactionControllerInternal> transactions = new HashMap<>();

    /**
     * Can only be called from within synchronized method.
     */
    public void add(String transactionName,
            ConfigTransactionControllerInternal transactionController) {
        Object oldValue = transactions.put(transactionName,
                transactionController);
        if (oldValue != null) {
            throw new IllegalStateException(
                    "Error: two transactions with same name");
        }
    }

    /**
     * Purges closed transactions from transactions map. Can only be called from
     * within synchronized method. Calling this method more than once within the
     * method can modify the resulting map that was obtained in previous calls.
     *
     * @return current view on transactions map.
     */
    public Map<String, ConfigTransactionControllerInternal> getCurrentTransactions() {
        // first, remove closed transaction
        for (Iterator<Entry<String, ConfigTransactionControllerInternal>> it = transactions
                .entrySet().iterator(); it.hasNext();) {
            Entry<String, ConfigTransactionControllerInternal> entry = it
                    .next();
            if (entry.getValue().isClosed()) {
                it.remove();
            }
        }
        return Collections.unmodifiableMap(transactions);
    }
}
