/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ConfigRegistryMXBean;
import org.opendaylight.controller.config.api.jmx.ConfigTransactionControllerMXBean;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;

import javax.management.Attribute;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.Map;
import java.util.Set;

public class ConfigTransactionJMXClient implements ConfigTransactionClient {
    private final ConfigRegistryMXBean configRegistryMXBeanProxy;
    private final ObjectName configTransactionControllerON;
    private final ConfigTransactionControllerMXBean configTransactionControllerMXBeanProxy;
    private final MBeanServer configMBeanServer;

    public ConfigTransactionJMXClient(
            ConfigRegistryMXBean configRegistryMXBeanProxy,
            ObjectName configTransactionControllerON,
            MBeanServer configMBeanServer) {
        this.configMBeanServer = configMBeanServer;
        this.configRegistryMXBeanProxy = configRegistryMXBeanProxy;
        this.configTransactionControllerON = configTransactionControllerON;
        this.configTransactionControllerMXBeanProxy = JMX.newMXBeanProxy(configMBeanServer,
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
        return configRegistryMXBeanProxy
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
        return configTransactionControllerMXBeanProxy.createModule(moduleName, instanceName);
    }

    @Override
    public void destroyModule(ObjectName objectName)
            throws InstanceNotFoundException {
        configTransactionControllerMXBeanProxy.destroyModule(objectName);
    }

    @Override
    @Deprecated
    /**
     * {@inheritDoc}
     */
    public void destroyConfigBean(String moduleName, String instanceName)
            throws InstanceNotFoundException {
        destroyModule(ObjectNameUtil.createTransactionModuleON(
                getTransactionName(), moduleName, instanceName));
    }

    @Override
    public void destroyModule(String moduleName, String instanceName)
            throws InstanceNotFoundException {
        destroyModule(ObjectNameUtil.createTransactionModuleON(
                getTransactionName(), moduleName, instanceName));
    }

    @Override
    public void abortConfig() {
        configTransactionControllerMXBeanProxy.abortConfig();
    }

    @Override
    public void validateConfig() throws ValidationException {
        configTransactionControllerMXBeanProxy.validateConfig();
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
        return configTransactionControllerMXBeanProxy.getTransactionName();
    }

    @Override
    public Set<String> getAvailableModuleNames() {
        return configTransactionControllerMXBeanProxy.getAvailableModuleNames();
    }

    @Override
    public ObjectName getObjectName() {
        return configTransactionControllerON;
    }

    @Override
    public Set<ObjectName> lookupConfigBeans() {
        return configTransactionControllerMXBeanProxy.lookupConfigBeans();
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName) {
        return configTransactionControllerMXBeanProxy.lookupConfigBeans(moduleName);
    }

    @Override
    public ObjectName lookupConfigBean(String moduleName, String instanceName)
            throws InstanceNotFoundException {
        return configTransactionControllerMXBeanProxy.lookupConfigBean(moduleName, instanceName);
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName,
            String instanceName) {
        return configTransactionControllerMXBeanProxy
                .lookupConfigBeans(moduleName, instanceName);
    }

    @Override
    public void checkConfigBeanExists(ObjectName objectName) throws InstanceNotFoundException {
        configTransactionControllerMXBeanProxy.checkConfigBeanExists(objectName);
    }

    @Override
    public ObjectName saveServiceReference(String serviceInterfaceName, String refName, ObjectName moduleON) throws InstanceNotFoundException {
        return configTransactionControllerMXBeanProxy.saveServiceReference(serviceInterfaceName,refName, moduleON);
    }

    @Override
    public void removeServiceReference(String serviceInterfaceName, String refName) throws InstanceNotFoundException{
        configTransactionControllerMXBeanProxy.removeServiceReference(serviceInterfaceName, refName);
    }

    @Override
    public void removeAllServiceReferences() {
        configTransactionControllerMXBeanProxy.removeAllServiceReferences();
    }

    @Override
    public ObjectName lookupConfigBeanByServiceInterfaceName(String serviceInterfaceQName, String refName) {
        return configTransactionControllerMXBeanProxy.lookupConfigBeanByServiceInterfaceName(serviceInterfaceQName, refName);
    }

    @Override
    public Map<String, Map<String, ObjectName>> getServiceMapping() {
        return configTransactionControllerMXBeanProxy.getServiceMapping();
    }

    @Override
    public Map<String, ObjectName> lookupServiceReferencesByServiceInterfaceName(String serviceInterfaceQName) {
        return configTransactionControllerMXBeanProxy.lookupServiceReferencesByServiceInterfaceName(serviceInterfaceQName);
    }

    @Override
    public Set<String> lookupServiceInterfaceNames(ObjectName objectName) throws InstanceNotFoundException {
        return configTransactionControllerMXBeanProxy.lookupServiceInterfaceNames(objectName);
    }

    @Override
    public String getServiceInterfaceName(String namespace, String localName) {
        return configTransactionControllerMXBeanProxy.getServiceInterfaceName(namespace, localName);
    }

    @Override
    public boolean removeServiceReferences(ObjectName objectName) throws InstanceNotFoundException {
        return configTransactionControllerMXBeanProxy.removeServiceReferences(objectName);
    }

    @Override
    public ObjectName getServiceReference(String serviceInterfaceQName, String refName) throws InstanceNotFoundException {
        return configTransactionControllerMXBeanProxy.getServiceReference(serviceInterfaceQName, refName);
    }

    @Override
    public void checkServiceReferenceExists(ObjectName objectName) throws InstanceNotFoundException {
        configTransactionControllerMXBeanProxy.checkServiceReferenceExists(objectName);
    }

    @Override
    public void validateBean(ObjectName configBeanON)
            throws ValidationException {
        try {
            configMBeanServer.invoke(configBeanON, "validate", null, null);
        } catch (MBeanException e) {
            Exception targetException = e.getTargetException();
            if (targetException instanceof ValidationException){
                throw (ValidationException) targetException;
            } else {
                throw new RuntimeException(e);
            }
        } catch (JMException e) {
            throw new RuntimeException(e);
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

    @Override
    public Set<String> getAvailableModuleFactoryQNames() {
        return configTransactionControllerMXBeanProxy.getAvailableModuleFactoryQNames();
    }
}
