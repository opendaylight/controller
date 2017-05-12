/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import java.util.Map;
import java.util.Set;
import javax.management.Attribute;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ConfigRegistryMXBean;
import org.opendaylight.controller.config.api.jmx.ConfigTransactionControllerMXBean;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;

public class ConfigTransactionJMXClient implements ConfigTransactionClient {
    private final ConfigRegistryMXBean configRegistryMXBeanProxy;
    private final ObjectName configTransactionControllerON;
    private final ConfigTransactionControllerMXBean configTransactionControllerMXBeanProxy;
    private final MBeanServer configMBeanServer;

    public ConfigTransactionJMXClient(
            final ConfigRegistryMXBean configRegistryMXBeanProxy,
            final ObjectName configTransactionControllerON,
            final MBeanServer configMBeanServer) {
        this.configMBeanServer = configMBeanServer;
        this.configRegistryMXBeanProxy = configRegistryMXBeanProxy;
        this.configTransactionControllerON = configTransactionControllerON;
        this.configTransactionControllerMXBeanProxy = JMX.newMXBeanProxy(configMBeanServer,
                configTransactionControllerON,
                ConfigTransactionControllerMXBean.class);
    }

    public <T> T newMXBeanProxy(final ObjectName on, final Class<T> clazz) {
        ObjectName onName = on;
        // if on is without transaction, add it. Reason is that when using getters on MXBeans the transaction name is stripped
        onName = ObjectNameUtil.withTransactionName(onName, getTransactionName());
        // if this is service reference and user requests for implementation, look it up
        onName = ConfigRegistryJMXClient.translateServiceRefIfPossible(onName, clazz, configMBeanServer);
        onName = ObjectNameUtil.withTransactionName(onName, getTransactionName());
        return JMX.newMXBeanProxy(configMBeanServer, onName, clazz);
    }

    /**
     * Usage of this method indicates error as config JMX uses solely MXBeans.
     * Use {@link #newMXBeanProxy(javax.management.ObjectName, Class)}
     * or {@link JMX#newMBeanProxy(javax.management.MBeanServerConnection, javax.management.ObjectName, Class)}
     * This method will be removed soon.
     */
    @Deprecated
    public <T> T newMBeanProxy(final ObjectName on, final Class<T> clazz) {
        return JMX.newMBeanProxy(configMBeanServer, on, clazz);
    }

    @Override
    public CommitStatus commit() throws ConflictingVersionException,
            ValidationException {
        return configRegistryMXBeanProxy
                .commitConfig(configTransactionControllerON);
    }

    @Override
    public void assertVersion(final int expectedParentVersion,
            final int expectedCurrentVersion) {
        if (expectedParentVersion != getParentVersion()) {
            throw new IllegalStateException();
        }
        if (expectedCurrentVersion != getVersion()) {
            throw new IllegalStateException();
        }
    }

    // proxy around ConfigManagerMXBean
    @Override
    public ObjectName createModule(final String moduleName, final String instanceName)
            throws InstanceAlreadyExistsException {
        return configTransactionControllerMXBeanProxy.createModule(moduleName, instanceName);
    }

    @Override
    public void reCreateModule(final ObjectName objectName) throws InstanceNotFoundException {
        configTransactionControllerMXBeanProxy.reCreateModule(objectName);
    }

    @Override
    public void destroyModule(final ObjectName objectName)
            throws InstanceNotFoundException {
        configTransactionControllerMXBeanProxy.destroyModule(objectName);
    }

    @Override
    @Deprecated
    /**
     * {@inheritDoc}
     */
    public void destroyConfigBean(final String moduleName, final String instanceName)
            throws InstanceNotFoundException {
        destroyModule(ObjectNameUtil.createTransactionModuleON(
                getTransactionName(), moduleName, instanceName));
    }

    @Override
    public void destroyModule(final String moduleName, final String instanceName)
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
        } catch (final JMException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getVersion() {
        try {
            return (Long) configMBeanServer.getAttribute(
                    configTransactionControllerON, "Version");
        } catch (final JMException e) {
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
    public Set<ObjectName> lookupConfigBeans(final String moduleName) {
        return configTransactionControllerMXBeanProxy.lookupConfigBeans(moduleName);
    }

    @Override
    public ObjectName lookupConfigBean(final String moduleName, final String instanceName)
            throws InstanceNotFoundException {
        return configTransactionControllerMXBeanProxy.lookupConfigBean(moduleName, instanceName);
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(final String moduleName,
            final String instanceName) {
        return configTransactionControllerMXBeanProxy
                .lookupConfigBeans(moduleName, instanceName);
    }

    @Override
    public void checkConfigBeanExists(final ObjectName objectName) throws InstanceNotFoundException {
        configTransactionControllerMXBeanProxy.checkConfigBeanExists(objectName);
    }

    @Override
    public ObjectName saveServiceReference(final String serviceInterfaceName, final String refName, final ObjectName moduleON) throws InstanceNotFoundException {
        return configTransactionControllerMXBeanProxy.saveServiceReference(serviceInterfaceName,refName, moduleON);
    }

    @Override
    public void removeServiceReference(final String serviceInterfaceName, final String refName) throws InstanceNotFoundException{
        configTransactionControllerMXBeanProxy.removeServiceReference(serviceInterfaceName, refName);
    }

    @Override
    public void removeAllServiceReferences() {
        configTransactionControllerMXBeanProxy.removeAllServiceReferences();
    }

    @Override
    public ObjectName lookupConfigBeanByServiceInterfaceName(final String serviceInterfaceQName, final String refName) {
        return configTransactionControllerMXBeanProxy.lookupConfigBeanByServiceInterfaceName(serviceInterfaceQName, refName);
    }

    @Override
    public Map<String, Map<String, ObjectName>> getServiceMapping() {
        return configTransactionControllerMXBeanProxy.getServiceMapping();
    }

    @Override
    public Map<String, ObjectName> lookupServiceReferencesByServiceInterfaceName(final String serviceInterfaceQName) {
        return configTransactionControllerMXBeanProxy.lookupServiceReferencesByServiceInterfaceName(serviceInterfaceQName);
    }

    @Override
    public Set<String> lookupServiceInterfaceNames(final ObjectName objectName) throws InstanceNotFoundException {
        return configTransactionControllerMXBeanProxy.lookupServiceInterfaceNames(objectName);
    }

    @Override
    public String getServiceInterfaceName(final String namespace, final String localName) {
        return configTransactionControllerMXBeanProxy.getServiceInterfaceName(namespace, localName);
    }

    @Override
    public boolean removeServiceReferences(final ObjectName objectName) throws InstanceNotFoundException {
        return configTransactionControllerMXBeanProxy.removeServiceReferences(objectName);
    }

    @Override
    public ObjectName getServiceReference(final String serviceInterfaceQName, final String refName) throws InstanceNotFoundException {
        return configTransactionControllerMXBeanProxy.getServiceReference(serviceInterfaceQName, refName);
    }

    @Override
    public void checkServiceReferenceExists(final ObjectName objectName) throws InstanceNotFoundException {
        configTransactionControllerMXBeanProxy.checkServiceReferenceExists(objectName);
    }

    @Override
    public Attribute getAttribute(final ObjectName on, final String attrName) {
        if (ObjectNameUtil.getTransactionName(on) == null) {
            throw new IllegalArgumentException("Not in transaction instance "
                    + on + ", no transaction name present");
        }

        try {
            return new Attribute(attrName, configMBeanServer.getAttribute(on,attrName));
        } catch (final JMException e) {
            throw new IllegalStateException("Unable to get attribute "
                    + attrName + " for " + on, e);
        }
    }

    @Override
    public Object getAttributeCurrentValue(final ObjectName on, final String attrName) {
        return getAttribute(on, attrName).getValue();
    }

    @Override
    public void validateBean(final ObjectName configBeanON)
            throws ValidationException {
        try {
            configMBeanServer.invoke(configBeanON, "validate", null, null);
        } catch (final MBeanException e) {
            Exception targetException = e.getTargetException();
            if (targetException instanceof ValidationException){
                throw (ValidationException) targetException;
            } else {
                throw new RuntimeException(e);
            }
        } catch (final JMException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setAttribute(final ObjectName on, final String attrName, final Attribute attribute) {
        if (ObjectNameUtil.getTransactionName(on) == null) {
            throw new IllegalArgumentException("Not in transaction instance "
                    + on + ", no transaction name present");
        }

        try {
            configMBeanServer.setAttribute(on, attribute);
        } catch (final JMException e) {
            throw new IllegalStateException("Unable to set attribute "
                    + attrName + " for " + on, e);
        }
    }

    @Override
    public Set<String> getAvailableModuleFactoryQNames() {
        return configTransactionControllerMXBeanProxy.getAvailableModuleFactoryQNames();
    }

    @Override
    public Set<ObjectName> lookupRuntimeBeans() {
        return configTransactionControllerMXBeanProxy.lookupRuntimeBeans();
    }

    @Override
    public Set<ObjectName> lookupRuntimeBeans(final String moduleName, final String instanceName) {
        return configTransactionControllerMXBeanProxy.lookupRuntimeBeans(moduleName, instanceName);
    }
}
