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
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.jmx.constants.ConfigRegistryConstants;

/**
 * Provides ObjectName creation. Each created ObjectName consists of domain that
 * is defined as {@link #ON_DOMAIN} and at least one key-value pair. The only
 * mandatory property is {@link #TYPE_KEY}. All transaction related mbeans have
 * {@link #TRANSACTION_NAME_KEY} property set.
 */
@ThreadSafe
public class ObjectNameUtil {
    private ObjectNameUtil() {
    }

    public static final String ON_DOMAIN = ConfigRegistryConstants.ON_DOMAIN;
    public static final String MODULE_FACTORY_NAME_KEY = "moduleFactoryName";
    public static final String SERVICE_QNAME_KEY = "serviceQName";
    public static final String INSTANCE_NAME_KEY = "instanceName";
    public static final String TYPE_KEY = ConfigRegistryConstants.TYPE_KEY;
    public static final String TYPE_CONFIG_TRANSACTION = "ConfigTransaction";
    public static final String TYPE_MODULE = "Module";
    public static final String TYPE_SERVICE_REFERENCE = "ServiceReference";
    public static final String TYPE_RUNTIME_BEAN = "RuntimeBean";
    public static final String TRANSACTION_NAME_KEY = "TransactionName";
    public static final String REF_NAME_KEY = "RefName";
    private static final String REPLACED_QUOTATION_MARK = "\\?";
    public static final String ON_WILDCARD = "*";

    public static ObjectName createON(String on) {
        try {
            return new ObjectName(on);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static ObjectName createONWithDomainAndType(String type) {
        return ConfigRegistryConstants.createONWithDomainAndType(type);
    }

    public static ObjectName createON(String name, String key, String value) {
        return ConfigRegistryConstants.createON(name, key, value);
    }

    public static ObjectName createON(String domain, Map<String, String> attribs) {
        Hashtable<String, String> table = new Hashtable<>(attribs);
        try {
            return new ObjectName(domain, table);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
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
        Map<String, String> onParams = createModuleMap(moduleName, instanceName);
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

    public static ObjectName createReadOnlyServiceON(String serviceQName, String refName) {
        Map<String, String> onParams = createServiceMap(serviceQName, refName);
        return createON(ON_DOMAIN, onParams);
    }

    public static ObjectName createTransactionServiceON(String transactionName, String serviceQName, String refName) {
        Map<String, String> onParams = createServiceON(transactionName, serviceQName, refName);
        return createON(ON_DOMAIN, onParams);
    }

    public static String getServiceQName(ObjectName objectName) {
        checkType(objectName, TYPE_SERVICE_REFERENCE);
        String quoted = objectName.getKeyProperty(SERVICE_QNAME_KEY);
        return unquoteAndUnescape(objectName, quoted);
    }

    // ObjectName supports quotation and ignores tokens like =, but fails to ignore ? sign.
    // It must be replaced with another character that hopefully does not collide
    // with actual value.
    private static String unquoteAndUnescape(ObjectName objectName, String quoted) {
        if (quoted == null) {
            throw new IllegalArgumentException("Cannot find " + SERVICE_QNAME_KEY + " in " + objectName);
        }
        if (!quoted.startsWith("\"") || !quoted.endsWith("\"")) {
            throw new IllegalArgumentException("Quotes not found in " + objectName);
        }
        String substring = quoted.substring(1);
        substring = substring.substring(0, substring.length() - 1);
        substring = substring.replace(REPLACED_QUOTATION_MARK, "?");
        return substring;
    }

    private static String quoteAndEscapeValue(String serviceQName) {
        return "\"" + serviceQName.replace("?", REPLACED_QUOTATION_MARK) + "\"";
    }

    public static String getReferenceName(ObjectName objectName) {
        checkType(objectName, TYPE_SERVICE_REFERENCE);
        return objectName.getKeyProperty(REF_NAME_KEY);
    }

    private static Map<String, String> createServiceON(String transactionName, String serviceQName,
                                                       String refName) {
        Map<String, String> result = new HashMap<>(createServiceMap(serviceQName, refName));
        result.put(TRANSACTION_NAME_KEY, transactionName);
        return result;
    }

    private static Map<String, String> createServiceMap(String serviceQName,
                                                        String refName) {
        Map<String, String> onParams = new HashMap<>();
        onParams.put(TYPE_KEY, TYPE_SERVICE_REFERENCE);
        onParams.put(SERVICE_QNAME_KEY, quoteAndEscapeValue(serviceQName));
        onParams.put(REF_NAME_KEY, refName);
        return onParams;
    }


    public static ObjectName createReadOnlyModuleON(String moduleName,
                                                    String instanceName) {
        Map<String, String> onParams = createModuleMap(moduleName, instanceName);
        return createON(ON_DOMAIN, onParams);
    }

    private static Map<String, String> createModuleMap(String moduleName,
                                                       String instanceName) {
        Map<String, String> onParams = new HashMap<>();
        onParams.put(TYPE_KEY, TYPE_MODULE);
        onParams.put(MODULE_FACTORY_NAME_KEY, moduleName);
        onParams.put(INSTANCE_NAME_KEY, instanceName);
        return onParams;
    }

    public static String getFactoryName(ObjectName objectName) {
        checkTypeOneOf(objectName, TYPE_MODULE, TYPE_RUNTIME_BEAN);
        return objectName.getKeyProperty(MODULE_FACTORY_NAME_KEY);
    }

    public static String getInstanceName(ObjectName objectName) {
        checkTypeOneOf(objectName, TYPE_MODULE, TYPE_RUNTIME_BEAN);
        return objectName.getKeyProperty(INSTANCE_NAME_KEY);
    }

    public static String getTransactionName(ObjectName objectName) {
        return objectName.getKeyProperty(TRANSACTION_NAME_KEY);
    }

    /**
     * Sanitize on: keep only mandatory attributes of module + metadata.
     */
    public static ObjectName withoutTransactionName(ObjectName inputON) {
        checkTypeOneOf(inputON, TYPE_MODULE, TYPE_SERVICE_REFERENCE);
        if (getTransactionName(inputON) == null) {
            throw new IllegalArgumentException(
                    "Expected ObjectName with transaction:" + inputON);
        }
        if (!ON_DOMAIN.equals(inputON.getDomain())) {
            throw new IllegalArgumentException("Expected different domain: "
                    + inputON);
        }
        Map<String, String> outputProperties;
        if (inputON.getKeyProperty(TYPE_KEY).equals(TYPE_MODULE)) {
            String moduleName = getFactoryName(inputON);
            String instanceName = getInstanceName(inputON);
            outputProperties = new HashMap<>(createModuleMap(moduleName, instanceName));
        } else {
            String serviceQName = getServiceQName(inputON);
            String refName = getReferenceName(inputON);
            outputProperties = new HashMap<>(createServiceMap(serviceQName, refName));
        }
        Map<String, String> allProperties = getAdditionalProperties(inputON);
        for (Entry<String, String> entry : allProperties.entrySet()) {
            if (entry.getKey().startsWith("X-")) {
                outputProperties.put(entry.getKey(), entry.getValue());
            }
        }
        return createON(ON_DOMAIN, outputProperties);
    }

    public static ObjectName withTransactionName(ObjectName inputON, String transactionName) {
        Map<String, String> additionalProperties = getAdditionalProperties(inputON);
        additionalProperties.put(TRANSACTION_NAME_KEY, transactionName);
        return createON(inputON.getDomain(), additionalProperties);

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
            if (!blacklist.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public static Map<String, String> getAdditionalProperties(ObjectName on) {
        Map<String, String> keyPropertyList = on.getKeyPropertyList();
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

    public static void checkTypeOneOf(ObjectName objectName, String... types) {
        for (String type : types) {
            if (type.equals(objectName.getKeyProperty(TYPE_KEY))) {
                return;
            }
        }
        throw new IllegalArgumentException("Wrong type, expected one of " + Arrays.asList(types)
                + ", got " + objectName);
    }

    public static ObjectName createModulePattern(String moduleName,
                                                 String instanceName) {
        String finalModuleName = moduleName == null ? ON_WILDCARD : moduleName;
        String finalInstanceName = instanceName == null ? ON_WILDCARD : instanceName;

        // do not return object names containing transaction name
        ObjectName namePattern = ObjectNameUtil
                .createON(ObjectNameUtil.ON_DOMAIN + ":"
                        + ObjectNameUtil.TYPE_KEY + "="
                        + ObjectNameUtil.TYPE_MODULE + ","
                        + ObjectNameUtil.MODULE_FACTORY_NAME_KEY + "="
                        + finalModuleName + "," + ""
                        + ObjectNameUtil.INSTANCE_NAME_KEY + "=" + finalInstanceName);
        return namePattern;
    }

    public static ObjectName createModulePattern(String ifcName,
                                                 String instanceName, String transactionName) {
        String finalIfcName = ifcName == null ? ON_WILDCARD : ifcName;
        String finalInstanceName = instanceName == null ? ON_WILDCARD : instanceName;
        String finalTransactionName = transactionName == null ? ON_WILDCARD : transactionName;

        return ObjectNameUtil.createON(ObjectNameUtil.ON_DOMAIN
                + ":type=Module," + ObjectNameUtil.MODULE_FACTORY_NAME_KEY
                + "=" + finalIfcName + "," + ObjectNameUtil.INSTANCE_NAME_KEY + "="
                + finalInstanceName + "," + ObjectNameUtil.TRANSACTION_NAME_KEY
                + "=" + finalTransactionName);
    }

    public static ObjectName createRuntimeBeanPattern(String moduleName,
                                                      String instanceName) {
        String finalModuleName = moduleName == null ? ON_WILDCARD : moduleName;
        String finalInstanceName = instanceName == null ? ON_WILDCARD : instanceName;

        return ObjectNameUtil.createON(ObjectNameUtil.ON_DOMAIN + ":"
                + ObjectNameUtil.TYPE_KEY + "="
                + ObjectNameUtil.TYPE_RUNTIME_BEAN + ","
                + ObjectNameUtil.MODULE_FACTORY_NAME_KEY + "=" + finalModuleName
                + "," + ObjectNameUtil.INSTANCE_NAME_KEY + "=" + finalInstanceName
                + ",*");

    }

    public static ModuleIdentifier fromON(ObjectName objectName,
                                          String expectedType) {
        checkType(objectName, expectedType);
        String factoryName = getFactoryName(objectName);
        if (factoryName == null) {
            throw new IllegalArgumentException(
                    "ObjectName does not contain module name");
        }
        String instanceName = getInstanceName(objectName);
        if (instanceName == null) {
            throw new IllegalArgumentException(
                    "ObjectName does not contain instance name");
        }
        return new ModuleIdentifier(factoryName, instanceName);
    }

    public static boolean isServiceReference(ObjectName objectName) {
        return TYPE_SERVICE_REFERENCE.equals(objectName.getKeyProperty(TYPE_KEY));
    }
}
