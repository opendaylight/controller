/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api.jmx;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;

import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.jmx.constants.ConfigRegistryConstants;

/**
 * Provides ObjectName creation. Each created ObjectName consists of domain that
 * is defined as {@link #ON_DOMAIN} and at least one key-value pair. The only
 * mandatory property is {@link #TYPE_KEY}. All transaction related mbeans have
 * {@link #TRANSACTION_NAME_KEY} property set.
 *
 */
@ThreadSafe
public class ObjectNameUtil {

    public static final String ON_DOMAIN = ConfigRegistryConstants.ON_DOMAIN;
    public static final String MODULE_FACTORY_NAME_KEY = "moduleFactoryName";
    public static final String INSTANCE_NAME_KEY = "instanceName";
    public static final String TYPE_KEY = ConfigRegistryConstants.TYPE_KEY;
    public static final String TYPE_CONFIG_REGISTRY = ConfigRegistryConstants.TYPE_CONFIG_REGISTRY;
    public static final String TYPE_CONFIG_TRANSACTION = "ConfigTransaction";
    public static final String TYPE_MODULE = "Module";
    public static final String TYPE_RUNTIME_BEAN = "RuntimeBean";

    public static final String TRANSACTION_NAME_KEY = "TransactionName";

    public static ObjectName createON(String on) {
        try {
            return new ObjectName(on);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectName createONWithDomainAndType(String type) {
        return ConfigRegistryConstants.createONWithDomainAndType(type);
    }

    public static ObjectName createON(String name, String key, String value) {
        return ConfigRegistryConstants.createON(name, key, value);
    }

    public static ObjectName createON(String name, Map<String, String> attribs) {
        Hashtable<String, String> table = new Hashtable<>(attribs);
        try {
            return new ObjectName(name, table);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static ObjectName createTransactionControllerON(
            String transactionName) {
        Map<String, String> onParams = new HashMap<>();
        onParams.put(TRANSACTION_NAME_KEY, transactionName);
        onParams.put(TYPE_KEY, TYPE_CONFIG_TRANSACTION);
        return createON(ON_DOMAIN, onParams);
    }

    public static ObjectName createTransactionModuleON(String transactionName,
            ModuleIdentifier moduleIdentifier) {
        return createTransactionModuleON(transactionName,
                moduleIdentifier.getFactoryName(),
                moduleIdentifier.getInstanceName());
    }

    public static ObjectName createTransactionModuleON(String transactionName,
            String moduleName, String instanceName) {
        Map<String, String> onParams = createModuleON(moduleName, instanceName);
        onParams.put(TRANSACTION_NAME_KEY, transactionName);
        return createON(ON_DOMAIN, onParams);
    }

    public static ObjectName createTransactionModuleON(String transactionName,
            ObjectName on) {
        return createTransactionModuleON(transactionName, getFactoryName(on),
                getInstanceName(on));
    }

    public static ObjectName createReadOnlyModuleON(
            ModuleIdentifier moduleIdentifier) {
        return createReadOnlyModuleON(moduleIdentifier.getFactoryName(),
                moduleIdentifier.getInstanceName());
    }

    public static ObjectName createReadOnlyModuleON(String moduleName,
            String instanceName) {
        Map<String, String> onParams = createModuleON(moduleName, instanceName);
        return createON(ON_DOMAIN, onParams);
    }

    private static Map<String, String> createModuleON(String moduleName,
            String instanceName) {
        Map<String, String> onParams = new HashMap<>();
        onParams.put(TYPE_KEY, TYPE_MODULE);
        onParams.put(MODULE_FACTORY_NAME_KEY, moduleName);
        onParams.put(INSTANCE_NAME_KEY, instanceName);
        return onParams;
    }

    public static String getFactoryName(ObjectName objectName) {
        return objectName.getKeyProperty(MODULE_FACTORY_NAME_KEY);
    }

    public static String getInstanceName(ObjectName objectName) {
        return objectName.getKeyProperty(INSTANCE_NAME_KEY);
    }

    public static String getTransactionName(ObjectName objectName) {
        return objectName.getKeyProperty(TRANSACTION_NAME_KEY);
    }

    public static ObjectName withoutTransactionName(ObjectName on) {
        if (getTransactionName(on) == null) {
            throw new IllegalArgumentException(
                    "Expected ObjectName with transaction:" + on);
        }
        if (ON_DOMAIN.equals(on.getDomain()) == false) {
            throw new IllegalArgumentException("Expected different domain: "
                    + on);
        }
        String moduleName = getFactoryName(on);
        String instanceName = getInstanceName(on);
        return createReadOnlyModuleON(moduleName, instanceName);
    }

    private static void assertDoesNotContain(
            Map<String, String> additionalProperties, String key) {
        if (additionalProperties.containsKey(key)) {
            throw new IllegalArgumentException(
                    "Map 'additionalProperties' cannot overwrite attribute "
                            + key);
        }
    }

    public static ObjectName createRuntimeBeanName(String moduleName,
            String instanceName, Map<String, String> additionalProperties) {
        // check that there is no overwriting of default attributes
        assertDoesNotContain(additionalProperties, MODULE_FACTORY_NAME_KEY);
        assertDoesNotContain(additionalProperties, INSTANCE_NAME_KEY);
        assertDoesNotContain(additionalProperties, TYPE_KEY);
        assertDoesNotContain(additionalProperties, TRANSACTION_NAME_KEY);
        Map<String, String> map = new HashMap<>(additionalProperties);
        map.put(MODULE_FACTORY_NAME_KEY, moduleName);
        map.put(INSTANCE_NAME_KEY, instanceName);
        map.put(TYPE_KEY, TYPE_RUNTIME_BEAN);
        return createON(ON_DOMAIN, map);
    }

    private static Set<String> blacklist = new HashSet<>(Arrays.asList(
            MODULE_FACTORY_NAME_KEY, INSTANCE_NAME_KEY, TYPE_KEY));

    public static Map<String, String> getAdditionalPropertiesOfRuntimeBeanName(
            ObjectName on) {
        checkType(on, TYPE_RUNTIME_BEAN);
        Map<String, String> allProperties = getAdditionalProperties(on);
        Map<String, String> result = new HashMap<>();
        for (Entry<String, String> entry : allProperties.entrySet()) {
            if (blacklist.contains(entry.getKey()) == false) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public static Map<String, String> getAdditionalProperties(ObjectName on) {
        Hashtable<String, String> keyPropertyList = on.getKeyPropertyList();
        Map<String, String> result = new HashMap<>();
        for (Entry<String, String> entry : keyPropertyList.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static void checkDomain(ObjectName objectName) {
        if (!ON_DOMAIN.equals(objectName.getDomain())) {
            throw new IllegalArgumentException("Wrong domain " + objectName);
        }

    }

    public static void checkType(ObjectName objectName, String type) {
        if (!type.equals(objectName.getKeyProperty(TYPE_KEY))) {
            throw new IllegalArgumentException("Wrong type, expected '" + type
                    + "', got " + objectName);
        }
    }

    public static ObjectName createModulePattern(String moduleName,
            String instanceName) {
        if (moduleName == null)
            moduleName = "*";
        if (instanceName == null)
            instanceName = "*";
        // do not return object names containing transaction name
        ObjectName namePattern = ObjectNameUtil
                .createON(ObjectNameUtil.ON_DOMAIN + ":"
                        + ObjectNameUtil.TYPE_KEY + "="
                        + ObjectNameUtil.TYPE_MODULE + ","
                        + ObjectNameUtil.MODULE_FACTORY_NAME_KEY + "="
                        + moduleName + "," + ""
                        + ObjectNameUtil.INSTANCE_NAME_KEY + "=" + instanceName);
        return namePattern;
    }

    public static ObjectName createModulePattern(String ifcName,
            String instanceName, String transactionName) {
        return ObjectNameUtil.createON(ObjectNameUtil.ON_DOMAIN
                + ":type=Module," + ObjectNameUtil.MODULE_FACTORY_NAME_KEY
                + "=" + ifcName + "," + ObjectNameUtil.INSTANCE_NAME_KEY + "="
                + instanceName + "," + ObjectNameUtil.TRANSACTION_NAME_KEY
                + "=" + transactionName);
    }

    public static ObjectName createRuntimeBeanPattern(String moduleName,
            String instanceName) {
        return ObjectNameUtil.createON(ObjectNameUtil.ON_DOMAIN + ":"
                + ObjectNameUtil.TYPE_KEY + "="
                + ObjectNameUtil.TYPE_RUNTIME_BEAN + ","
                + ObjectNameUtil.MODULE_FACTORY_NAME_KEY + "=" + moduleName
                + "," + ObjectNameUtil.INSTANCE_NAME_KEY + "=" + instanceName
                + ",*");

    }

    public static ModuleIdentifier fromON(ObjectName objectName,
            String expectedType) {
        checkType(objectName, expectedType);
        String factoryName = getFactoryName(objectName);
        if (factoryName == null)
            throw new IllegalArgumentException(
                    "ObjectName does not contain module name");
        String instanceName = getInstanceName(objectName);
        if (instanceName == null)
            throw new IllegalArgumentException(
                    "ObjectName does not contain instance name");
        return new ModuleIdentifier(factoryName, instanceName);
    }

}
