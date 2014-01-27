/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ServiceReferenceWritableRegistry;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Wraps second phase commit with additional functionality - a watchdog thread that will warn
 * after a period that there might be a deadlock.
 */
public class DeadlockInCommitDetector extends ConfigTransactionControllerInternalWrapper {

    public DeadlockInCommitDetector(ConfigTransactionControllerInternal inner) {
        super(inner);
    }

    @Override
    public List<ModuleIdentifier> secondPhaseCommit() {
        Thread watchdogThread = new Thread(new Watchdog(), "ConfigWatchdog-" + inner.toString());
        watchdogThread.start();
        try {
            return inner.secondPhaseCommit();
        } finally {
            watchdogThread.interrupt();
        }
    }

}

class Watchdog implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Watchdog.class);

    private final Thread watchedThread = Thread.currentThread();
    private final long startNanos = System.nanoTime();

    private final int maxWaitSecconds = 30;

    @Override
    public void run() {
        long maxWaitNanos = TimeUnit.SECONDS.toNanos(maxWaitSecconds);
        try {
            while(true) { // repeat warning every x seconds
                long deadlineNanos =  System.nanoTime() + maxWaitNanos;
                while (System.nanoTime() < deadlineNanos) {
                    Thread.sleep(1000);
                }
                // possible deadlock
                logger.warn("Second phase commit did not finish after {} ms\nPlease inspect thread dump: {}",
                        getDurationFromStartInMillis(), Arrays.asList(watchedThread.getStackTrace()));
            }
        } catch (InterruptedException e) {
            logger.trace("Commit finished after {} ms", getDurationFromStartInMillis());
        }
    }

    private long getDurationFromStartInMillis() {
        return (System.nanoTime() - startNanos) / 10 ^ 6;
    }
}


class ConfigTransactionControllerInternalWrapper implements ConfigTransactionControllerInternal {

    protected final ConfigTransactionControllerInternal inner;

    public ConfigTransactionControllerInternalWrapper(ConfigTransactionControllerInternal inner) {
        this.inner = inner;
    }


    @Override
    public void copyExistingModulesAndProcessFactoryDiff(Collection<ModuleInternalInfo> entries, List<ModuleFactory> lastListOfFactories) {
        inner.copyExistingModulesAndProcessFactoryDiff(entries, lastListOfFactories);
    }

    @Override
    public CommitInfo validateBeforeCommitAndLockTransaction() throws ValidationException {
        return inner.validateBeforeCommitAndLockTransaction();
    }

    @Override
    public ObjectName getControllerObjectName() {
        return inner.getControllerObjectName();
    }

    @Override
    public List<ModuleIdentifier> secondPhaseCommit() {
        return inner.secondPhaseCommit();
    }

    @Override
    public boolean isClosed() {
        return inner.isClosed();
    }

    @Override
    public List<ModuleFactory> getCurrentlyRegisteredFactories() {
        return inner.getCurrentlyRegisteredFactories();
    }

    @Override
    public BundleContext getModuleFactoryBundleContext(String factoryName) {
        return inner.getModuleFactoryBundleContext(factoryName);
    }

    @Override
    public ServiceReferenceWritableRegistry getWritableRegistry() {
        return inner.getWritableRegistry();
    }

    @Override
    public long getParentVersion() {
        return inner.getParentVersion();
    }

    @Override
    public long getVersion() {
        return inner.getVersion();
    }

    @Override
    public ObjectName createModule(String moduleName, String instanceName) throws InstanceAlreadyExistsException {
        return inner.createModule(moduleName, instanceName);
    }

    @Override
    public void destroyModule(ObjectName objectName) throws InstanceNotFoundException {
        inner.destroyModule(objectName);
    }

    @Override
    public void abortConfig() {
        inner.abortConfig();
    }

    @Override
    public void validateConfig() throws ValidationException {
        inner.validateConfig();
    }

    @Override
    public String getTransactionName() {
        return inner.getTransactionName();
    }

    @Override
    public Set<String> getAvailableModuleNames() {
        return inner.getAvailableModuleNames();
    }

    @Override
    public Set<ObjectName> lookupConfigBeans() {
        return inner.lookupConfigBeans();
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName) {
        return inner.lookupConfigBeans(moduleName);
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName, String instanceName) {
        return inner.lookupConfigBeans(moduleName, instanceName);
    }

    @Override
    public ObjectName lookupConfigBean(String moduleName, String instanceName) throws InstanceNotFoundException {
        return inner.lookupConfigBean(moduleName, instanceName);
    }

    @Override
    public void checkConfigBeanExists(ObjectName objectName) throws InstanceNotFoundException {
        inner.checkConfigBeanExists(objectName);
    }

    @Override
    public Set<String> getAvailableModuleFactoryQNames() {
        return inner.getAvailableModuleFactoryQNames();
    }

    @Override
    public ObjectName saveServiceReference(String serviceInterfaceName, String refName, ObjectName moduleON) throws InstanceNotFoundException {
        return inner.saveServiceReference(serviceInterfaceName, refName, moduleON);
    }

    @Override
    public void removeServiceReference(String serviceInterfaceName, String refName) throws InstanceNotFoundException {
        inner.removeServiceReference(serviceInterfaceName, refName);
    }

    @Override
    public void removeAllServiceReferences() {
        inner.removeAllServiceReferences();
    }

    @Override
    public boolean removeServiceReferences(ObjectName objectName) throws InstanceNotFoundException {
        return inner.removeServiceReferences(objectName);
    }

    @Override
    public ObjectName lookupConfigBeanByServiceInterfaceName(String serviceInterfaceQName, String refName) {
        return inner.lookupConfigBeanByServiceInterfaceName(serviceInterfaceQName, refName);
    }

    @Override
    public Map<String, Map<String, ObjectName>> getServiceMapping() {
        return inner.getServiceMapping();
    }

    @Override
    public Map<String, ObjectName> lookupServiceReferencesByServiceInterfaceName(String serviceInterfaceQName) {
        return inner.lookupServiceReferencesByServiceInterfaceName(serviceInterfaceQName);
    }

    @Override
    public Set<String> lookupServiceInterfaceNames(ObjectName objectName) throws InstanceNotFoundException {
        return inner.lookupServiceInterfaceNames(objectName);
    }

    @Override
    public String getServiceInterfaceName(String namespace, String localName) {
        return inner.getServiceInterfaceName(namespace, localName);
    }

    @Override
    public ObjectName getServiceReference(String serviceInterfaceQName, String refName) throws InstanceNotFoundException {
        return inner.getServiceReference(serviceInterfaceQName, refName);
    }

    @Override
    public void checkServiceReferenceExists(ObjectName objectName) throws InstanceNotFoundException {
        inner.checkServiceReferenceExists(objectName);
    }

    @Override
    public String toString() {
        return getClass() + ":" + inner.toString();
    }

}
