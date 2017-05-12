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
import java.util.Map;
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
import org.opendaylight.controller.config.api.jmx.ServiceReferenceMXBean;
import org.opendaylight.controller.config.api.jmx.constants.ConfigRegistryConstants;

public class ConfigRegistryJMXClient implements ConfigRegistryClient {
    private final ConfigRegistryMXBean configRegistryMXBeanProxy;
    private final ObjectName configRegistryON;
    private final MBeanServer configMBeanServer;

    public ConfigRegistryJMXClient(final MBeanServer configMBeanServer) {
        this(configMBeanServer, OBJECT_NAME);
    }

    private ConfigRegistryJMXClient(final MBeanServer configMBeanServer, final ObjectName configRegistryON) {
        this.configMBeanServer = configMBeanServer;
        this.configRegistryON = configRegistryON;
        Set<ObjectInstance> searchResult = configMBeanServer.queryMBeans(configRegistryON, null);
        if (searchResult.size() != 1) {
            throw new IllegalStateException("Config registry not found");
        }
        configRegistryMXBeanProxy = JMX.newMXBeanProxy(configMBeanServer, configRegistryON, ConfigRegistryMXBean.class,
                false);
    }

    public static ConfigRegistryJMXClient createWithoutNotifications(final MBeanServer configMBeanServer) {
        return new ConfigRegistryJMXClient(configMBeanServer, ConfigRegistryConstants.OBJECT_NAME_NO_NOTIFICATIONS);
    }

    @Override
    public ConfigTransactionJMXClient createTransaction() {
        ObjectName configTransactionControllerON = beginConfig();
        return getConfigTransactionClient(configTransactionControllerON);
    }

    @Override
    public ConfigTransactionJMXClient getConfigTransactionClient(
            final String transactionName) {
        ObjectName objectName = ObjectNameUtil
                .createTransactionControllerON(transactionName);
        return getConfigTransactionClient(objectName);
    }

    @Override
    public ConfigTransactionJMXClient getConfigTransactionClient(
            final ObjectName objectName) {
        return new ConfigTransactionJMXClient(configRegistryMXBeanProxy, objectName,
                configMBeanServer);
    }

    /**
     * Usage of this method indicates error as config JMX uses solely MXBeans.
     * Use {@link #newMXBeanProxy(javax.management.ObjectName, Class)}
     * or {@link JMX#newMBeanProxy(javax.management.MBeanServerConnection, javax.management.ObjectName, Class)}
     * This method will be removed soon.
     */
    @Deprecated
    public <T> T newMBeanProxy(final ObjectName on, final Class<T> clazz) {
        ObjectName onObj = translateServiceRefIfPossible(on, clazz, configMBeanServer);
        return JMX.newMBeanProxy(configMBeanServer, onObj, clazz);
    }

    static  ObjectName translateServiceRefIfPossible(final ObjectName on, final Class<?> clazz, final MBeanServer configMBeanServer) {
        ObjectName onObj = on;
        if (ObjectNameUtil.isServiceReference(onObj) && !clazz.equals(ServiceReferenceMXBean.class)) {
            ServiceReferenceMXBean proxy = JMX.newMXBeanProxy(configMBeanServer, onObj, ServiceReferenceMXBean.class);
            onObj = proxy.getCurrentImplementation();
        }
        return onObj;
    }


    public <T> T newMXBeanProxy(final ObjectName on, final Class<T> clazz) {
        return JMX.newMXBeanProxy(configMBeanServer, on, clazz);
    }

    @Override
    public ObjectName beginConfig() {
        return configRegistryMXBeanProxy.beginConfig();
    }

    @Override
    public CommitStatus commitConfig(final ObjectName transactionControllerON)
            throws ConflictingVersionException, ValidationException {
        return configRegistryMXBeanProxy.commitConfig(transactionControllerON);
    }

    @Override
    public List<ObjectName> getOpenConfigs() {
        return configRegistryMXBeanProxy.getOpenConfigs();
    }

    @Override
    public long getVersion() {
        try {
            return (Long) configMBeanServer.getAttribute(configRegistryON,
                    "Version");
        } catch (final JMException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> getAvailableModuleNames() {
        return configRegistryMXBeanProxy.getAvailableModuleNames();
    }

    @Override
    public boolean isHealthy() {
        return configRegistryMXBeanProxy.isHealthy();
    }

    @Override
    public Set<ObjectName> lookupConfigBeans() {
        return configRegistryMXBeanProxy.lookupConfigBeans();
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(final String moduleName) {
        return configRegistryMXBeanProxy.lookupConfigBeans(moduleName);
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(final String moduleName,
            final String instanceName) {
        return configRegistryMXBeanProxy.lookupConfigBeans(moduleName, instanceName);
    }

    @Override
    public ObjectName lookupConfigBean(final String moduleName, final String instanceName)
            throws InstanceNotFoundException {
        return configRegistryMXBeanProxy.lookupConfigBean(moduleName, instanceName);
    }

    @Override
    public Set<ObjectName> lookupRuntimeBeans() {
        return configRegistryMXBeanProxy.lookupRuntimeBeans();
    }

    @Override
    public Set<ObjectName> lookupRuntimeBeans(final String ifcName,
            final String instanceName) {
        return configRegistryMXBeanProxy.lookupRuntimeBeans(ifcName, instanceName);
    }

    @Override
    public void checkConfigBeanExists(final ObjectName objectName) throws InstanceNotFoundException {
        configRegistryMXBeanProxy.checkConfigBeanExists(objectName);
    }

    @Override
    public ObjectName lookupConfigBeanByServiceInterfaceName(final String serviceInterfaceQName, final String refName) {
        return configRegistryMXBeanProxy.lookupConfigBeanByServiceInterfaceName(serviceInterfaceQName, refName);
    }

    @Override
    public Map<String, Map<String, ObjectName>> getServiceMapping() {
        return configRegistryMXBeanProxy.getServiceMapping();
    }

    @Override
    public Map<String, ObjectName> lookupServiceReferencesByServiceInterfaceName(final String serviceInterfaceQName) {
        return configRegistryMXBeanProxy.lookupServiceReferencesByServiceInterfaceName(serviceInterfaceQName);
    }

    @Override
    public Set<String> lookupServiceInterfaceNames(final ObjectName objectName) throws InstanceNotFoundException {
        return configRegistryMXBeanProxy.lookupServiceInterfaceNames(objectName);
    }

    @Override
    public String getServiceInterfaceName(final String namespace, final String localName) {
        return configRegistryMXBeanProxy.getServiceInterfaceName(namespace, localName);
    }

    @Override
    public Object invokeMethod(final ObjectName on, final String name, final Object[] params,
            final String[] signature) {
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
    public Object getAttributeCurrentValue(final ObjectName on, final String attributeName) {
        try {
            return configMBeanServer.getAttribute(on, attributeName);
        } catch (AttributeNotFoundException | InstanceNotFoundException
                | MBeanException | ReflectionException e) {
            throw new RuntimeException("Unable to get attribute "
                    + attributeName + " for " + on + ". Available beans: " + lookupConfigBeans(), e);
        }
    }

    @Override
    public Set<String> getAvailableModuleFactoryQNames() {
        return configRegistryMXBeanProxy.getAvailableModuleFactoryQNames();
    }

    @Override
    public ObjectName getServiceReference(final String serviceInterfaceQName, final String refName) throws InstanceNotFoundException {
        return configRegistryMXBeanProxy.getServiceReference(serviceInterfaceQName, refName);
    }

    @Override
    public void checkServiceReferenceExists(final ObjectName objectName) throws InstanceNotFoundException {
        configRegistryMXBeanProxy.checkServiceReferenceExists(objectName);
    }
}
