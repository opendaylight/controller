/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ConfigRegistryMXBean;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;

public class ConfigRegistryJMXClient implements ConfigRegistryClient {
    private final ConfigRegistryMXBean configRegistryProxy;
    private final ObjectName configRegistryON;
    private final MBeanServer configMBeanServer;

    public ConfigRegistryJMXClient(MBeanServer configMBeanServer) {
        this.configMBeanServer = configMBeanServer;
        configRegistryON = OBJECT_NAME;
        Set<ObjectInstance> searchResult = configMBeanServer.queryMBeans(
                configRegistryON, null);
        if (!(searchResult.size() == 1)) {
            throw new IllegalStateException("Config registry not found");
        }
        configRegistryProxy = JMX.newMXBeanProxy(configMBeanServer, configRegistryON, ConfigRegistryMXBean.class,
                false);
    }

    @Override
    public ConfigTransactionJMXClient createTransaction() {
        ObjectName configTransactionControllerON = beginConfig();
        return getConfigTransactionClient(configTransactionControllerON);
    }

    @Override
    public ConfigTransactionJMXClient getConfigTransactionClient(
            String transactionName) {
        ObjectName objectName = ObjectNameUtil
                .createTransactionControllerON(transactionName);
        return getConfigTransactionClient(objectName);
    }

    @Override
    public ConfigTransactionJMXClient getConfigTransactionClient(
            ObjectName objectName) {
        return new ConfigTransactionJMXClient(configRegistryProxy, objectName,
                configMBeanServer);
    }

    public <T> T newMBeanProxy(ObjectName on, Class<T> clazz) {
        return JMX.newMBeanProxy(configMBeanServer, on, clazz);
    }

    public <T> T newMXBeanProxy(ObjectName on, Class<T> clazz) {
        return JMX.newMXBeanProxy(configMBeanServer, on, clazz);
    }

    @Override
    public ObjectName beginConfig() {
        return configRegistryProxy.beginConfig();
    }

    @Override
    public CommitStatus commitConfig(ObjectName transactionControllerON)
            throws ConflictingVersionException, ValidationException {
        return configRegistryProxy.commitConfig(transactionControllerON);
    }

    @Override
    public List<ObjectName> getOpenConfigs() {
        return configRegistryProxy.getOpenConfigs();
    }

    @Override
    public long getVersion() {
        try {
            return (Long) configMBeanServer.getAttribute(configRegistryON,
                    "Version");
        } catch (JMException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> getAvailableModuleNames() {
        return configRegistryProxy.getAvailableModuleNames();
    }

    @Override
    public boolean isHealthy() {
        return configRegistryProxy.isHealthy();
    }

    @Override
    public Set<ObjectName> lookupConfigBeans() {
        return configRegistryProxy.lookupConfigBeans();
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName) {
        return configRegistryProxy.lookupConfigBeans(moduleName);
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName,
            String instanceName) {
        return configRegistryProxy.lookupConfigBeans(moduleName, instanceName);
    }

    @Override
    public ObjectName lookupConfigBean(String moduleName, String instanceName)
            throws InstanceNotFoundException {
        return configRegistryProxy.lookupConfigBean(moduleName, instanceName);
    }

    @Override
    public Set<ObjectName> lookupRuntimeBeans() {
        return configRegistryProxy.lookupRuntimeBeans();
    }

    @Override
    public Set<ObjectName> lookupRuntimeBeans(String ifcName,
            String instanceName) {
        return configRegistryProxy.lookupRuntimeBeans(ifcName, instanceName);
    }

    @Override
    public Object invokeMethod(ObjectName on, String name, Object[] params,
            String[] signature) {
        try {
            return configMBeanServer.invoke(on, name, params, signature);
        } catch (InstanceNotFoundException | ReflectionException
                | MBeanException e) {
            throw new RuntimeException("Unable to invoke operation " + name
                    + " on " + on + " with attributes "
                    + Arrays.toString(params) + " and signature "
                    + Arrays.toString(signature), e);
        }
    }

    @Override
    public Object getAttributeCurrentValue(ObjectName on, String attributeName) {
        try {
            return configMBeanServer.getAttribute(on, attributeName);
        } catch (AttributeNotFoundException | InstanceNotFoundException
                | MBeanException | ReflectionException e) {
            throw new RuntimeException("Unable to get attribute "
                    + attributeName + " for " + on, e);
        }
    }

}
