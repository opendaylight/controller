/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import java.util.Set;

import javax.management.Attribute;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;

import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ConfigRegistryMXBean;
import org.opendaylight.controller.config.api.jmx.ConfigTransactionControllerMXBean;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;

public class ConfigTransactionJMXClient implements ConfigTransactionClient {
    private final ConfigRegistryMXBean configTransactionManagerProxy;
    private final ObjectName configTransactionControllerON;
    private final ConfigTransactionControllerMXBean configControllerProxy;
    private final MBeanServer configMBeanServer;

    public ConfigTransactionJMXClient(
            ConfigRegistryMXBean configTransactionManagerProxy,
            ObjectName configTransactionControllerON,
            MBeanServer configMBeanServer) {
        this.configMBeanServer = configMBeanServer;
        this.configTransactionManagerProxy = configTransactionManagerProxy;
        this.configTransactionControllerON = configTransactionControllerON;
        this.configControllerProxy = JMX.newMXBeanProxy(configMBeanServer,
                configTransactionControllerON,
                ConfigTransactionControllerMXBean.class);
    }

    public <T> T newMXBeanProxy(ObjectName on, Class<T> clazz) {
        return JMX.newMXBeanProxy(configMBeanServer, on, clazz);
    }

    public <T> T newMBeanProxy(ObjectName on, Class<T> clazz) {
        return JMX.newMBeanProxy(configMBeanServer, on, clazz);
    }

    @Override
    public CommitStatus commit() throws ConflictingVersionException,
            ValidationException {
        return configTransactionManagerProxy
                .commitConfig(configTransactionControllerON);
    }

    @Override
    public void assertVersion(int expectedParentVersion,
            int expectedCurrentVersion) {
        if (expectedParentVersion != getParentVersion()) {
            throw new IllegalStateException();
        }
        if (expectedCurrentVersion != getVersion()) {
            throw new IllegalStateException();
        }
    }

    // proxy around ConfigManagerMXBean
    @Override
    public ObjectName createModule(String moduleName, String instanceName)
            throws InstanceAlreadyExistsException {
        return configControllerProxy.createModule(moduleName, instanceName);
    }

    @Override
    public void destroyModule(ObjectName objectName)
            throws InstanceNotFoundException {
        configControllerProxy.destroyModule(objectName);
    }

    @Override
    public void destroyConfigBean(String moduleName, String instanceName)
            throws InstanceNotFoundException {
        destroyModule(ObjectNameUtil.createTransactionModuleON(
                getTransactionName(), moduleName, instanceName));
    }

    @Override
    public void abortConfig() {
        configControllerProxy.abortConfig();
    }

    @Override
    public void validateConfig() throws ValidationException {
        configControllerProxy.validateConfig();
    }

    @Override
    public long getParentVersion() {
        try {
            return (Long) configMBeanServer.getAttribute(
                    configTransactionControllerON, "ParentVersion");
        } catch (JMException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getVersion() {
        try {
            return (Long) configMBeanServer.getAttribute(
                    configTransactionControllerON, "Version");
        } catch (JMException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTransactionName() {
        return configControllerProxy.getTransactionName();
    }

    @Override
    public Set<String> getAvailableModuleNames() {
        return configControllerProxy.getAvailableModuleNames();
    }

    @Override
    public ObjectName getObjectName() {
        return configTransactionControllerON;
    }

    @Override
    public Set<ObjectName> lookupConfigBeans() {
        return configControllerProxy.lookupConfigBeans();
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName) {
        return configControllerProxy.lookupConfigBeans(moduleName);
    }

    @Override
    public ObjectName lookupConfigBean(String moduleName, String instanceName)
            throws InstanceNotFoundException {
        return configControllerProxy.lookupConfigBean(moduleName, instanceName);
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName,
            String instanceName) {
        return configControllerProxy
                .lookupConfigBeans(moduleName, instanceName);
    }

    @Override
    public void validateBean(ObjectName configBeanON)
            throws ValidationException {
        try {
            configMBeanServer.invoke(configBeanON, "validate", null, null);
        } catch (JMException e) {
            throw new RuntimeException(e);
        } catch (RuntimeMBeanException e) {
            throw e.getTargetException();
        }
    }

    @Override
    public void setAttribute(ObjectName on, String attrName, Attribute attribute) {
        if (ObjectNameUtil.getTransactionName(on) == null)
            throw new IllegalArgumentException("Not in transaction instance "
                    + on + ", no transaction name present");

        try {
            configMBeanServer.setAttribute(on, attribute);
        } catch (JMException e) {
            throw new IllegalStateException("Unable to set attribute "
                    + attrName + " for " + on, e);
        }
    }
}
