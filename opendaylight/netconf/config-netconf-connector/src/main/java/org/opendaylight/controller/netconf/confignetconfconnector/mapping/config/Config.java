/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.config;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditStrategyType;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.management.ObjectName;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

public class Config {
    private final Logger logger = LoggerFactory.getLogger(Config.class);

    private final Map<String/* Namespace from yang file */,
            Map<String /* Name of module entry from yang file */, ModuleConfig>> moduleConfigs;
    private final Map<String, ModuleConfig> moduleNamesToConfigs;

    private final Map<String, Map<Date, EditConfig.IdentityMapping>> identityMap;

    public Config(Map<String, Map<String, ModuleConfig>> moduleConfigs) {
        this(moduleConfigs, Collections.<String, Map<Date, EditConfig.IdentityMapping>>emptyMap());
    }

    public Config(Map<String, Map<String, ModuleConfig>> moduleConfigs, Map<String, Map<Date,EditConfig.IdentityMapping>> identityMap) {
        this.moduleConfigs = moduleConfigs;
        Map<String, ModuleConfig> moduleNamesToConfigs = new HashMap<>();
        for (Entry<String, Map<String, ModuleConfig>> entry : moduleConfigs.entrySet()) {
            moduleNamesToConfigs.putAll(entry.getValue());
        }
        this.moduleNamesToConfigs = Collections.unmodifiableMap(moduleNamesToConfigs);
        this.identityMap = identityMap;
    }

    public static Map<String, Map<String, Collection<ObjectName>>> getMappedInstances(Set<ObjectName> instancesToMap,
                                                                                Map<String, Map<String, ModuleConfig>> configs) {
        Multimap<String, ObjectName> moduleToInstances = mapInstancesToModules(instancesToMap);

        Map<String, Map<String, Collection<ObjectName>>> retVal = Maps.newLinkedHashMap();

        for (String namespace : configs.keySet()) {

            Map<String, Collection<ObjectName>> innerRetVal = Maps.newHashMap();

            for (Entry<String, ModuleConfig> mbeEntry : configs.get(namespace).entrySet()) {

                String moduleName = mbeEntry.getKey();
                Collection<ObjectName> instances = moduleToInstances.get(moduleName);

                // TODO, this code does not support same module names from different namespaces
                // Namespace should be present in ObjectName

                if (instances == null)
                    continue;

                innerRetVal.put(moduleName, instances);

            }

            retVal.put(namespace, innerRetVal);
        }
        return retVal;
    }

    private static Multimap<String, ObjectName> mapInstancesToModules(Set<ObjectName> instancesToMap) {
        Multimap<String, ObjectName> retVal = HashMultimap.create();

        for (ObjectName objectName : instancesToMap) {
            String factoryName = ObjectNameUtil.getFactoryName(objectName);
            retVal.put(factoryName, objectName);
        }
        return retVal;
    }

    // public Element toXml(Set<ObjectName> instancesToMap, String namespace,
    // Document document) {
    // return toXml(instancesToMap, Optional.of(namespace), document);
    // }

    public Element toXml(Set<ObjectName> instancesToMap, Optional<String> maybeNamespace, Document document,
            Element dataElement, ServiceRegistryWrapper serviceTracker) {

        Map<String, Map<String, Collection<ObjectName>>> moduleToInstances = getMappedInstances(instancesToMap,
                moduleConfigs);

        Element root = dataElement;
        if (maybeNamespace.isPresent()) {
            XmlUtil.addNamespaceAttr(root, maybeNamespace.get());
        }

        Element modulesElement = document.createElement(XmlNetconfConstants.MODULES_KEY);
        XmlUtil.addNamespaceAttr(modulesElement,
                XmlNetconfConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
        root.appendChild(modulesElement);
        for (String moduleNamespace : moduleToInstances.keySet()) {
            for (Entry<String, Collection<ObjectName>> moduleMappingEntry : moduleToInstances.get(moduleNamespace)
                    .entrySet()) {

                ModuleConfig mapping = moduleConfigs.get(moduleNamespace).get(moduleMappingEntry.getKey());

                if (moduleMappingEntry.getValue().isEmpty()) {
                    continue;
                }

                for (ObjectName objectName : moduleMappingEntry.getValue()) {
                    modulesElement.appendChild(mapping.toXml(objectName, serviceTracker, document, moduleNamespace));
                }

            }
        }

        root.appendChild(Services.toXml(serviceTracker, document));

        return root;
    }

    // TODO refactor, replace string representing namespace with namespace class
    // TODO refactor, replace Map->Multimap with e.g. ConfigElementResolved
    // class

    public Map<String, Multimap<String, ModuleElementResolved>> fromXmlModulesResolved(XmlElement xml, EditStrategyType defaultEditStrategyType, ServiceRegistryWrapper serviceTracker) {
        Optional<XmlElement> modulesElement = getModulesElement(xml);
        List<XmlElement> moduleElements = getModulesElementList(modulesElement);

        Map<String, Multimap<String, ModuleElementResolved>> retVal = Maps.newHashMap();

        for (XmlElement moduleElement : moduleElements) {
            ResolvingStrategy<ModuleElementResolved> resolvingStrategy = new ResolvingStrategy<ModuleElementResolved>() {
                @Override
                public ModuleElementResolved resolveElement(ModuleConfig moduleMapping, XmlElement moduleElement, ServiceRegistryWrapper serviceTracker, String instanceName, String moduleNamespace, EditStrategyType defaultStrategy) {
                    return moduleMapping.fromXml(moduleElement, serviceTracker,
                            instanceName, moduleNamespace, defaultStrategy, identityMap);
                }
            };

            resolveModule(retVal, serviceTracker, moduleElement, defaultEditStrategyType, resolvingStrategy);
        }
        return retVal;
    }

    /**
     * return a map containing namespace -> moduleName -> instanceName map. Attribute parsing is omitted.
     */
    public Map<String, Multimap<String, ModuleElementDefinition>> fromXmlModulesMap(XmlElement xml,
            EditStrategyType defaultEditStrategyType, ServiceRegistryWrapper serviceTracker) {
        Optional<XmlElement> modulesElement = getModulesElement(xml);
        List<XmlElement> moduleElements = getModulesElementList(modulesElement);

        Map<String, Multimap<String, ModuleElementDefinition>> retVal = Maps.newHashMap();

        for (XmlElement moduleElement : moduleElements) {
            ResolvingStrategy<ModuleElementDefinition> resolvingStrategy = new ResolvingStrategy<ModuleElementDefinition>() {
                @Override
                public ModuleElementDefinition resolveElement(ModuleConfig moduleMapping, XmlElement moduleElement,
                        ServiceRegistryWrapper serviceTracker, String instanceName, String moduleNamespace,
                        EditStrategyType defaultStrategy) {
                    // TODO: add check for conflicts between global and local
                    // edit strategy
                    String perInstanceEditStrategy = moduleElement.getAttribute(XmlNetconfConstants.OPERATION_ATTR_KEY,
                            XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
                    return new ModuleElementDefinition(instanceName, perInstanceEditStrategy, defaultStrategy);
                }
            };

            resolveModule(retVal, serviceTracker, moduleElement, defaultEditStrategyType, resolvingStrategy);
        }
        return retVal;
    }

    private static Optional<XmlElement> getModulesElement(XmlElement xml) {
        return xml.getOnlyChildElementOptionally(XmlNetconfConstants.MODULES_KEY,
                    XmlNetconfConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
    }

    private List<XmlElement> getModulesElementList(Optional<XmlElement> modulesElement) {
        List<XmlElement> moduleElements;

        if (modulesElement.isPresent()) {
            moduleElements = modulesElement.get().getChildElementsWithSameNamespace(XmlNetconfConstants.MODULE_KEY);
            modulesElement.get().checkUnrecognisedElements(moduleElements);
        } else {
            moduleElements = Lists.newArrayList();
        }
        return moduleElements;
    }

    private <T> void resolveModule(Map<String, Multimap<String, T>> retVal, ServiceRegistryWrapper serviceTracker,
            XmlElement moduleElement, EditStrategyType defaultStrategy, ResolvingStrategy<T> resolvingStrategy) {
        XmlElement typeElement = moduleElement.getOnlyChildElementWithSameNamespace(XmlNetconfConstants.TYPE_KEY);
        Entry<String, String> prefixToNamespace = typeElement.findNamespaceOfTextContent();
        String moduleNamespace = prefixToNamespace.getValue();
        XmlElement nameElement = moduleElement.getOnlyChildElementWithSameNamespace(XmlNetconfConstants.NAME_KEY);
        String instanceName = nameElement.getTextContent();
        String factoryNameWithPrefix = typeElement.getTextContent();
        String prefixOrEmptyString = prefixToNamespace.getKey();
        String factoryName = getFactoryName(factoryNameWithPrefix, prefixOrEmptyString);

        ModuleConfig moduleMapping = getModuleMapping(moduleNamespace, instanceName, factoryName);

        Multimap<String, T> innerMap = retVal.get(moduleNamespace);
        if (innerMap == null) {
            innerMap = HashMultimap.create();
            retVal.put(moduleNamespace, innerMap);
        }

        T resolvedElement = resolvingStrategy.resolveElement(moduleMapping, moduleElement, serviceTracker,
                instanceName, moduleNamespace, defaultStrategy);

        innerMap.put(factoryName, resolvedElement);
    }

    public Services fromXmlServices(XmlElement xml) {
        Optional<XmlElement> servicesElement = getServicesElement(xml);

        Services services;
        if (servicesElement.isPresent()) {
            services = Services.fromXml(servicesElement.get());
        } else {
            services = new Services();
        }

        return services;
    }

    private static Optional<XmlElement> getServicesElement(XmlElement xml) {
        return xml.getOnlyChildElementOptionally(XmlNetconfConstants.SERVICES_KEY,
                    XmlNetconfConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
    }

    public static void checkUnrecognisedChildren(XmlElement parent) {
        Optional<XmlElement> servicesOpt = getServicesElement(parent);
        Optional<XmlElement> modulesOpt = getModulesElement(parent);

        List<XmlElement> recognised = Lists.newArrayList();
        if(servicesOpt.isPresent())
            recognised.add(servicesOpt.get());
        if(modulesOpt.isPresent())
            recognised.add(modulesOpt.get());

        parent.checkUnrecognisedElements(recognised);
    }

    private String getFactoryName(String factoryNameWithPrefix, String prefixOrEmptyString) {
        checkState(
                factoryNameWithPrefix.startsWith(prefixOrEmptyString),
                format("Internal error: text " + "content '%s' of type node does not start with prefix '%s'",
                        factoryNameWithPrefix, prefixOrEmptyString));

        int factoryNameAfterPrefixIndex;
        if (prefixOrEmptyString.isEmpty()) {
            factoryNameAfterPrefixIndex = 0;
        } else {
            factoryNameAfterPrefixIndex = prefixOrEmptyString.length() + 1;
        }
        return factoryNameWithPrefix.substring(factoryNameAfterPrefixIndex);
    }

    private ModuleConfig getModuleMapping(String moduleNamespace, String instanceName, String factoryName) {
        Map<String, ModuleConfig> mappingsFromNamespace = moduleConfigs.get(moduleNamespace);

        Preconditions.checkNotNull(mappingsFromNamespace,
                "Namespace %s, defined in: module %s of type %s not found, available namespaces: %s", moduleNamespace,
                instanceName, factoryName, moduleConfigs.keySet());

        ModuleConfig moduleMapping = mappingsFromNamespace.get(factoryName);
        checkState(moduleMapping != null, "Cannot find mapping for module type " + factoryName);
        return moduleMapping;
    }

    private interface ResolvingStrategy<T> {
        public T resolveElement(ModuleConfig moduleMapping, XmlElement moduleElement, ServiceRegistryWrapper serviceTracker,
                String instanceName, String moduleNamespace, EditStrategyType defaultStrategy);
    }
}
