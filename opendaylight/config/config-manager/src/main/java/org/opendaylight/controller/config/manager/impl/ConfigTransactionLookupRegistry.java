/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import java.io.Closeable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.LookupRegistry;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.TransactionModuleJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.util.LookupBeansUtil;
import org.opendaylight.controller.config.manager.impl.util.ModuleQNameUtil;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;

/**
 * Responsible for creating TransactionJMXRegistrator, registering transaction and all its beans,
 * lookup of beans, closing of TransactionJMXRegistrator.
 */
class ConfigTransactionLookupRegistry  implements LookupRegistry, Closeable {
    private final TransactionJMXRegistrator transactionJMXRegistrator;
    private final TransactionIdentifier transactionIdentifier;
    private final TransactionModuleJMXRegistrator txModuleJMXRegistrator;
    private final Map<String, Map.Entry<ModuleFactory, BundleContext>> allCurrentFactories;

    ConfigTransactionLookupRegistry(TransactionIdentifier transactionIdentifier,
                                    TransactionJMXRegistratorFactory factory, Map<String, Entry<ModuleFactory, BundleContext>> allCurrentFactories) {
        this.transactionIdentifier = transactionIdentifier;
        this.transactionJMXRegistrator = factory.create();
        this.txModuleJMXRegistrator = transactionJMXRegistrator.createTransactionModuleJMXRegistrator();
        this.allCurrentFactories = allCurrentFactories;
    }

    private void checkTransactionName(ObjectName objectName) {
        String foundTransactionName = ObjectNameUtil
                .getTransactionName(objectName);
        if (transactionIdentifier.getName().equals(foundTransactionName) == false) {
            throw new IllegalArgumentException("Wrong transaction name "
                    + objectName);
        }
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
    public void checkConfigBeanExists(ObjectName objectName) throws InstanceNotFoundException {
        ObjectNameUtil.checkDomain(objectName);
        ObjectNameUtil.checkType(objectName, ObjectNameUtil.TYPE_MODULE);
        checkTransactionName(objectName);
        // make sure exactly one match is found:
        LookupBeansUtil.lookupConfigBean(this, ObjectNameUtil.getFactoryName(objectName), ObjectNameUtil.getInstanceName(objectName));
    }

    TransactionIdentifier getTransactionIdentifier() {
        return transactionIdentifier;
    }

    TransactionModuleJMXRegistrator getTxModuleJMXRegistrator() {
        return txModuleJMXRegistrator;
    }

    public void close() {
        transactionJMXRegistrator.close();
    }

    public void registerMBean(ConfigTransactionControllerInternal transactionController, ObjectName controllerObjectName) throws InstanceAlreadyExistsException {
        transactionJMXRegistrator.registerMBean(transactionController, controllerObjectName);
    }

    @Override
    public Set<String> getAvailableModuleFactoryQNames() {
        return ModuleQNameUtil.getQNames(allCurrentFactories);
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
        return transactionJMXRegistrator.queryNames(namePattern, null);
    }

    @Override
    public String toString() {
        return "ConfigTransactionLookupRegistry{" +
                "transactionIdentifier=" + transactionIdentifier +
                '}';
    }
}

interface TransactionJMXRegistratorFactory {
    TransactionJMXRegistrator create();
}
