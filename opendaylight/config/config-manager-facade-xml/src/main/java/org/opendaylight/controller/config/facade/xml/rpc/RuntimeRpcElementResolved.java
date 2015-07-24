/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.rpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.util.xml.XmlMappingConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.Modules;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;

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

    @VisibleForTesting
    Map<String, String> getAdditionalAttributes() {
        return additionalAttributes;
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

    /**
     * Pattern for an absolute instance identifier xpath pointing to a runtime bean instance e.g:
     * <pre>
     * /modules/module[name=instanceName][type=moduleType]
     * </pre>
     * or
     * <pre>
     * /a:modules/a:module[a:name=instanceName][a:type=moduleType]
     * </pre>
     */
    private static final String xpathPatternBlueprint =
            "/" + getRegExForPrefixedName(Modules.QNAME.getLocalName())+ "/" + getRegExForPrefixedName(Module.QNAME.getLocalName())

                    + "\\["
                    + "(?<key1>" + getRegExForPrefixedName(XmlMappingConstants.TYPE_KEY) + "|" + getRegExForPrefixedName(XmlMappingConstants.NAME_KEY) + ")"
                    + "=('|\")?(?<value1>[^'\"\\]]+)('|\")?"
                    + "( and |\\]\\[)"
                    + "(?<key2>" + getRegExForPrefixedName(XmlMappingConstants.TYPE_KEY) + "|" + getRegExForPrefixedName(XmlMappingConstants.NAME_KEY) + ")"
                    + "=('|\")?(?<value2>[^'\"\\]]+)('|\")?"
                    + "\\]"

                    + "(?<additional>.*)";

    /**
     * Return reg ex that matches either the name with or without a prefix
     */
    private static String getRegExForPrefixedName(final String name) {
        return "([^:]+:)?" + name;
    }

    private static final Pattern xpathPattern = Pattern.compile(xpathPatternBlueprint);

    /**
     * Pattern for additional path elements inside xpath for instance identifier pointing to an inner runtime bean e.g:
     * <pre>
     * /modules/module[name=instanceName and type=moduleType]/inner[key=b]
     * </pre>
     */
    private static final String additionalPatternBlueprint = getRegExForPrefixedName("(?<additionalKey>.+)") + "\\[(?<prefixedKey>" + getRegExForPrefixedName("(.+)") + ")=('|\")?(?<additionalValue>[^'\"\\]]+)('|\")?\\]";
    private static final Pattern additionalPattern = Pattern.compile(additionalPatternBlueprint);

    public static RuntimeRpcElementResolved fromXpath(String xpath, String elementName, String namespace) {
        Matcher matcher = xpathPattern.matcher(xpath);
        Preconditions.checkState(matcher.matches(),
                "Node %s with value '%s' not in required form on rpc element %s, required format is %s",
                //TODO refactor this string, and/or unify with RPR.CONTEXT_INSTANCE from netconf
                "context-instance", xpath, elementName, xpathPatternBlueprint);

        PatternGroupResolver groups = new PatternGroupResolver(matcher.group("key1"), matcher.group("value1"),
                matcher.group("value2"), matcher.group("additional"));

        String moduleName = groups.getModuleName();
        String instanceName = groups.getInstanceName();

        Map<String, String> additionalAttributes = groups.getAdditionalKeys(elementName, moduleName);

        return new RuntimeRpcElementResolved(namespace, moduleName, instanceName, groups.getRuntimeBeanYangName(),
                additionalAttributes);
    }

    private static final class PatternGroupResolver {

        private final String key1, value1, value2;
        private final String additional;
        private String runtimeBeanYangName;

        PatternGroupResolver(String key1, String value1,  String value2, String additional) {
            this.key1 = Preconditions.checkNotNull(key1);
            this.value1 = Preconditions.checkNotNull(value1);
            this.value2 = Preconditions.checkNotNull(value2);
            this.additional = Preconditions.checkNotNull(additional);
        }

        String getModuleName() {
            return key1.contains(XmlMappingConstants.TYPE_KEY) ? value1 : value2;
        }

        String getInstanceName() {
            return key1.contains(XmlMappingConstants.NAME_KEY) ? value1 : value2;
        }


        Map<String, String> getAdditionalKeys(String elementName, String moduleName) {
            HashMap<String, String> additionalAttributes = Maps.newHashMap();

            runtimeBeanYangName = moduleName;
            for (String additionalKeyValue : additional.split("/")) {
                if (Strings.isNullOrEmpty(additionalKeyValue)){
                    continue;
                }
                Matcher matcher = additionalPattern.matcher(additionalKeyValue);
                Preconditions
                        .checkState(
                                matcher.matches(),
                                "Attribute %s not in required form on rpc element %s, required format for additional attributes is: %s",
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
