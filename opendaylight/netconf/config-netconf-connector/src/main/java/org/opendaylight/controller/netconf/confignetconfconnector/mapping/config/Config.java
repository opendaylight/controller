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

    public Config(Map<String, Map<String, ModuleConfig>> moduleConfigs) {
        this.moduleConfigs = moduleConfigs;
        Map<String, ModuleConfig> moduleNamesToConfigs = new HashMap<>();
        for (Entry<String, Map<String, ModuleConfig>> entry : moduleConfigs.entrySet()) {
            moduleNamesToConfigs.putAll(entry.getValue());
        }
        this.moduleNamesToConfigs = Collections.unmodifiableMap(moduleNamesToConfigs);
    }

    private Map<String, Map<String, Collection<ObjectName>>> getMappedInstances(Set<ObjectName> instancesToMap,
            Services serviceTracker) {
        Multimap<String, ObjectName> moduleToInstances = mapInstancesToModules(instancesToMap);

        Map<String, Map<String, Collection<ObjectName>>> retVal = Maps.newLinkedHashMap();

        for (String namespace : moduleConfigs.keySet()) {

            Map<String, Collection<ObjectName>> innerRetVal = Maps.newHashMap();

            for (Entry<String, ModuleConfig> mbeEntry : moduleConfigs.get(namespace).entrySet()) {

                String moduleName = mbeEntry.getKey();
                Collection<ObjectName> instances = moduleToInstances.get(moduleName);

                if (instances == null)
                    continue;

                innerRetVal.put(moduleName, instances);

                // All found instances add to service tracker in advance
                // This way all instances will be serialized as all available
                // services when get-config is triggered
                // (even if they are not used as services by other instances)
                // = more user friendly
                addServices(serviceTracker, instances, mbeEntry.getValue().getProvidedServices());

            }

            retVal.put(namespace, innerRetVal);
        }
        return retVal;
    }

    private void addServices(Services serviceTracker, Collection<ObjectName> instances,
            Multimap<String, String> providedServices) {
        for (ObjectName instanceOn : instances) {
            for (Entry<String, String> serviceName : providedServices.entries()) {
                serviceTracker.addServiceEntry(serviceName.getKey(), serviceName.getValue(), instanceOn);
            }
        }
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
            Element dataElement) {
        Services serviceTracker = new Services();

        Map<String, Map<String, Collection<ObjectName>>> moduleToInstances = getMappedInstances(instancesToMap,
                serviceTracker);

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
                    addEmptyModulesCommented(document, modulesElement, moduleNamespace, moduleMappingEntry);
                } else {
                    for (ObjectName objectName : moduleMappingEntry.getValue()) {
                        modulesElement
                                .appendChild(mapping.toXml(objectName, serviceTracker, document, moduleNamespace));
                    }
                }

            }
        }

        root.appendChild(serviceTracker.toXml(serviceTracker.getMappedServices(), document));

        return root;
    }

    // TODO remove commented modules from output
    private void addEmptyModulesCommented(Document document, Element root, String moduleNamespace,
            Entry<String, Collection<ObjectName>> moduleMappingEntry) {
        Element emptyModule = document.createElement(XmlNetconfConstants.MODULE_KEY);

        Element typeElement = XmlUtil.createTextElement(document, XmlNetconfConstants.TYPE_KEY,
                moduleMappingEntry.getKey());
        emptyModule.appendChild(typeElement);

        root.appendChild(document.createComment(XmlUtil.toString(emptyModule, false)));
    }

    // TODO refactor, replace string representing namespace with namespace class
    // TODO refactor, replace Map->Multimap with e.g. ConfigElementResolved
    // class
    public Map<String, Multimap<String, ModuleElementResolved>> fromXml(XmlElement xml, Set<ObjectName> instancesForFillingServiceRefMapping,
                                                                        EditStrategyType defaultEditStrategyType) {
        Map<String, Multimap<String, ModuleElementResolved>> retVal = Maps.newHashMap();

        List<XmlElement> recognisedChildren = Lists.newArrayList();

        Services serviceTracker = fromXmlServices(xml, recognisedChildren, instancesForFillingServiceRefMapping);
        List<XmlElement> moduleElements = fromXmlModules(xml, recognisedChildren);

        xml.checkUnrecognisedElements(recognisedChildren);

        for (XmlElement moduleElement : moduleElements) {
            resolveModule(retVal, serviceTracker, moduleElement, defaultEditStrategyType);
        }

        return retVal;
    }

    private List<XmlElement> fromXmlModules(XmlElement xml, List<XmlElement> recognisedChildren) {
        Optional<XmlElement> modulesElement = xml.getOnlyChildElementOptionally(XmlNetconfConstants.MODULES_KEY,
                XmlNetconfConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
        List<XmlElement> moduleElements;
        if (modulesElement.isPresent()) {
            moduleElements = modulesElement.get().getChildElementsWithSameNamespace(XmlNetconfConstants.MODULE_KEY);
            recognisedChildren.add(modulesElement.get());
            modulesElement.get().checkUnrecognisedElements(moduleElements);
        } else {
            moduleElements = Lists.newArrayList();
        }
        return moduleElements;
    }

    private void resolveModule(Map<String, Multimap<String, ModuleElementResolved>> retVal, Services serviceTracker,
            XmlElement moduleElement, EditStrategyType defaultStrategy) {
        XmlElement typeElement = moduleElement.getOnlyChildElementWithSameNamespace(XmlNetconfConstants.TYPE_KEY);
        Entry<String, String> prefixToNamespace = typeElement.findNamespaceOfTextContent();
        String moduleNamespace = prefixToNamespace.getValue();
        XmlElement nameElement = moduleElement.getOnlyChildElementWithSameNamespace(XmlNetconfConstants.NAME_KEY);
        String instanceName = nameElement.getTextContent();
        String factoryNameWithPrefix = typeElement.getTextContent();
        String prefixOrEmptyString = prefixToNamespace.getKey();
        String factoryName = getFactoryName(factoryNameWithPrefix, prefixOrEmptyString);

        ModuleConfig moduleMapping = getModuleMapping(moduleNamespace, instanceName, factoryName);

        Multimap<String, ModuleElementResolved> innerMap = retVal.get(moduleNamespace);
        if (innerMap == null) {
            innerMap = HashMultimap.create();
            retVal.put(moduleNamespace, innerMap);
        }

        ModuleElementResolved moduleElementResolved = moduleMapping.fromXml(moduleElement, serviceTracker,
                instanceName, moduleNamespace, defaultStrategy);

        innerMap.put(factoryName, moduleElementResolved);
    }

    private Services fromXmlServices(XmlElement xml, List<XmlElement> recognisedChildren, Set<ObjectName> instancesForFillingServiceRefMapping) {
        Optional<XmlElement> servicesElement = xml.getOnlyChildElementOptionally(XmlNetconfConstants.SERVICES_KEY,
                XmlNetconfConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);

        Map<String, Map<String, Map<String, String>>> mappedServices;
        if (servicesElement.isPresent()) {
            mappedServices = Services.fromXml(servicesElement.get());
            recognisedChildren.add(servicesElement.get());
        } else {
            mappedServices = new HashMap<>();
        }
        Services services = Services.resolveServices(mappedServices);
        // merge with what candidate db contains by default - ref_

        for(ObjectName existingON: instancesForFillingServiceRefMapping) {
            logger.trace("Filling services from {}", existingON);
            // get all its services
            String factoryName = ObjectNameUtil.getFactoryName(existingON);
            ModuleConfig moduleConfig = moduleNamesToConfigs.get(factoryName);

            checkState(moduleConfig != null, "Cannot find ModuleConfig with name " + factoryName + " in " + moduleNamesToConfigs);
            // Set<String> services = ;
            for (Entry<String, String> serviceName : moduleConfig.getProvidedServices().entries()) {

                services.addServiceEntry(serviceName.getKey(), serviceName.getValue(), existingON);
            }
        }

        return services;
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

}
