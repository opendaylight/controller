/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.runtimerpc;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.rpc.ModuleRpcs;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;

import javax.management.ObjectName;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents parsed xpath to runtime bean instance
 */
public final class RuntimeRpcElementResolved {
    private final String moduleName;
    private final String instanceName;
    private final String namespace;
    private final String runtimeBeanName;
    private final Map<String, String> additionalAttributes;

    private RuntimeRpcElementResolved(String namespace, String moduleName, String instanceName, String runtimeBeanName,
            Map<String, String> additionalAttributes) {
        this.moduleName = Preconditions.checkNotNull(moduleName, "Module name");
        this.instanceName =  Preconditions.checkNotNull(instanceName, "Instance name");
        this.additionalAttributes = additionalAttributes;
        this.namespace = Preconditions.checkNotNull(namespace, "Namespace");
        this.runtimeBeanName = Preconditions.checkNotNull(runtimeBeanName, "Runtime bean name");
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getRuntimeBeanName() {
        return runtimeBeanName;
    }

    public ObjectName getObjectName(ModuleRpcs rpcMapping) {
        Map<String, String> additionalAttributesJavaNames = Maps
                .newHashMapWithExpectedSize(additionalAttributes.size());
        for (String attributeYangName : additionalAttributes.keySet()) {
            String attributeJavaName = rpcMapping.getRbeJavaName(attributeYangName);
            Preconditions.checkState(attributeJavaName != null,
                    "Cannot find java name for runtime bean wtih yang name %s", attributeYangName);
            additionalAttributesJavaNames.put(attributeJavaName, additionalAttributes.get(attributeYangName));
        }
        return ObjectNameUtil.createRuntimeBeanName(moduleName, instanceName, additionalAttributesJavaNames);
    }

    private static final String xpathPatternBlueprint =
            "/" + XmlNetconfConstants.MODULES_KEY
            + "/" + XmlNetconfConstants.MODULE_KEY
            + "\\["

            + "(?<key1>type|name)"
            + "='(?<value1>[^']+)'"
            + "( and |\\]\\[)"
            + "(?<key2>type|name)"
            + "='(?<value2>[^']+)'"

            + "\\]"
            + "(?<additional>.*)";

    private static final Pattern xpathPattern = Pattern.compile(xpathPatternBlueprint);
    private static final String additionalPatternBlueprint = "(?<additionalKey>.+)\\[(.+)='(?<additionalValue>.+)'\\]";
    private static final Pattern additionalPattern = Pattern.compile(additionalPatternBlueprint);

    public static RuntimeRpcElementResolved fromXpath(String xpath, String elementName, String namespace) {
        Matcher matcher = xpathPattern.matcher(xpath);
        Preconditions.checkState(matcher.matches(),
                "Node %s with value '%s' not in required form on rpc element %s, required format is %s",
                RuntimeRpc.CONTEXT_INSTANCE, xpath, elementName, xpathPatternBlueprint);

        PatternGroupResolver groups = new PatternGroupResolver(matcher.group("key1"), matcher.group("value1"),
                matcher.group("key2"), matcher.group("value2"), matcher.group("additional"));

        String moduleName = groups.getModuleName();
        String instanceName = groups.getInstanceName();

        HashMap<String, String> additionalAttributes = groups.getAdditionalKeys(elementName, moduleName);

        return new RuntimeRpcElementResolved(namespace, moduleName, instanceName, groups.getRuntimeBeanYangName(),
                additionalAttributes);
    }

    private static final class PatternGroupResolver {

        private final String key1, key2, value1, value2;
        private final String additional;
        private String runtimeBeanYangName;

        PatternGroupResolver(String key1, String value1, String key2, String value2, String additional) {
            this.key1 = Preconditions.checkNotNull(key1);
            this.value1 = Preconditions.checkNotNull(value1);

            this.key2 = Preconditions.checkNotNull(key2);
            this.value2 = Preconditions.checkNotNull(value2);

            this.additional = Preconditions.checkNotNull(additional);
        }

        String getModuleName() {
            return key1.equals(XmlNetconfConstants.TYPE_KEY) ? value1 : value2;
        }

        String getInstanceName() {
            return key1.equals(XmlNetconfConstants.NAME_KEY) ? value1 : value2;
        }

        HashMap<String, String> getAdditionalKeys(String elementName, String moduleName) {
            HashMap<String, String> additionalAttributes = Maps.newHashMap();

            runtimeBeanYangName = moduleName;
            for (String additionalKeyValue : additional.split("/")) {
                if (Strings.isNullOrEmpty(additionalKeyValue))
                    continue;
                Matcher matcher = additionalPattern.matcher(additionalKeyValue);
                Preconditions
                        .checkState(
                                matcher.matches(),
                                "Attribute %s not in required form on rpc element %s, required format for additional attributes is  %s",
                                additionalKeyValue, elementName, additionalPatternBlueprint);
                String name = matcher.group("additionalKey");
                runtimeBeanYangName = name;
                additionalAttributes.put(name, matcher.group("additionalValue"));
            }
            return additionalAttributes;
        }

        private String getRuntimeBeanYangName() {
            Preconditions.checkState(runtimeBeanYangName!=null);
            return runtimeBeanYangName;
        }
    }
}
