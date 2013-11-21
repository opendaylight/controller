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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeReadingStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.ObjectXmlReader;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping.AttributeMappingStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping.ObjectMapper;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.resolving.AttributeResolvingStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.resolving.ObjectResolver;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.toxml.AttributeWritingStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.toxml.ObjectXmlWriter;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditStrategyType;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.management.ObjectName;
import javax.management.openmbean.OpenType;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class InstanceConfig {
    private static final Logger logger = LoggerFactory.getLogger(InstanceConfig.class);

    private final Map<String, AttributeIfc> yangToAttrConfig;
    private final Map<String, AttributeIfc> jmxToAttrConfig;
    private final ConfigRegistryClient configRegistryClient;

    public InstanceConfig(ConfigRegistryClient configRegistryClient, Map<String, AttributeIfc> yangNamesToAttributes) {
        this.yangToAttrConfig = yangNamesToAttributes;
        this.jmxToAttrConfig = reverseMap(yangNamesToAttributes);
        this.configRegistryClient = configRegistryClient;
    }

    private Map<String, Object> getMappedConfiguration(ObjectName on, Services depTracker) {

        // TODO make field, mappingStrategies can be instantiated only once
        Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> mappingStrategies = new ObjectMapper(depTracker)
                .prepareMapping(jmxToAttrConfig);

        Map<String, Object> toXml = Maps.newHashMap();

        for (Entry<String, AttributeIfc> configDefEntry : jmxToAttrConfig.entrySet()) {

            // Skip children runtime beans as they are mapped by InstanceRuntime
            if (configDefEntry.getValue() instanceof RuntimeBeanEntry)
                continue;

            Object value = configRegistryClient.getAttributeCurrentValue(on, configDefEntry.getKey());
            try {
                AttributeMappingStrategy<?, ? extends OpenType<?>> attributeMappingStrategy = mappingStrategies
                        .get(configDefEntry.getKey());
                Optional<?> a = attributeMappingStrategy.mapAttribute(value);
                if (a.isPresent() == false)
                    continue;

                toXml.put(configDefEntry.getValue().getAttributeYangName(), a.get());
            } catch (Exception e) {
                throw new IllegalStateException("Unable to map value " + value + " to attribute "
                        + configDefEntry.getKey(), e);
            }
        }

        return toXml;
    }

    public Element toXml(ObjectName on, Services depTracker, String namespace, Document document, Element rootElement) {

        Element cfgElement = rootElement;

        Map<String, AttributeWritingStrategy> strats = new ObjectXmlWriter().prepareWriting(yangToAttrConfig, document);

        Map<String, Object> mappedConfig = getMappedConfiguration(on, depTracker);

        for (Entry<String, ?> mappingEntry : mappedConfig.entrySet()) {
            try {
                strats.get(mappingEntry.getKey()).writeElement(cfgElement, namespace, mappingEntry.getValue());
            } catch (Exception e) {
                throw new IllegalStateException("Unable to write value " + mappingEntry.getValue() + " for attribute "
                        + mappingEntry.getValue(), e);
            }
        }

        return cfgElement;
    }

    private void resolveConfiguration(InstanceConfigElementResolved mappedConfig, Services depTracker) {

        // TODO make field, resolvingStrategies can be instantiated only once
        Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> resolvingStrategies = new ObjectResolver(
                depTracker).prepareResolving(yangToAttrConfig);

        for (Entry<String, AttributeConfigElement> configDefEntry : mappedConfig.getConfiguration().entrySet()) {
            AttributeConfigElement value = configDefEntry.getValue();
            String attributeName = configDefEntry.getKey();
            try {
                AttributeResolvingStrategy<?, ? extends OpenType<?>> attributeResolvingStrategy = resolvingStrategies
                        .get(attributeName);
                logger.trace("Trying to set value {} of attribute {} with {}", value, attributeName, attributeResolvingStrategy);

                value.resolveValue(attributeResolvingStrategy, attributeName);
                value.setJmxName(
                        yangToAttrConfig.get(attributeName).getUpperCaseCammelCase());
            } catch (Exception e) {
                throw new IllegalStateException("Unable to resolve value " + value
                        + " to attribute " + attributeName, e);
            }
        }
    }

    public InstanceConfigElementResolved fromXml(XmlElement moduleElement, Services services, String moduleNamespace,
                                                 EditStrategyType defaultStrategy) {
        Map<String, AttributeConfigElement> retVal = Maps.newHashMap();

        Map<String, AttributeReadingStrategy> strats = new ObjectXmlReader().prepareReading(yangToAttrConfig);
        List<XmlElement> recognisedChildren = Lists.newArrayList();

        XmlElement type = moduleElement.getOnlyChildElementWithSameNamespace(XmlNetconfConstants.TYPE_KEY);
        XmlElement name = moduleElement.getOnlyChildElementWithSameNamespace(XmlNetconfConstants.NAME_KEY);
        List<XmlElement> typeAndName = Lists.newArrayList(type, name);

        for (Entry<String, AttributeReadingStrategy> readStratEntry : strats.entrySet()) {
            List<XmlElement> configNodes = getConfigNodes(moduleElement, moduleNamespace, readStratEntry.getKey(),
                    recognisedChildren, typeAndName);
            AttributeConfigElement readElement = readStratEntry.getValue().readElement(configNodes);
            retVal.put(readStratEntry.getKey(), readElement);
        }

        recognisedChildren.addAll(typeAndName);
        moduleElement.checkUnrecognisedElements(recognisedChildren);

        // TODO: add check for conflicts between global and local edit strategy
        String perInstanceEditStrategy = moduleElement.getAttribute(XmlNetconfConstants.OPERATION_ATTR_KEY,
                XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        InstanceConfigElementResolved instanceConfigElementResolved = perInstanceEditStrategy.equals("") ? new InstanceConfigElementResolved(
                retVal, defaultStrategy) : new InstanceConfigElementResolved(perInstanceEditStrategy, retVal, defaultStrategy);

        resolveConfiguration(instanceConfigElementResolved, services);
        return instanceConfigElementResolved;
    }

    private List<XmlElement> getConfigNodes(XmlElement moduleElement, String moduleNamespace, String name,
            List<XmlElement> recognisedChildren, List<XmlElement> typeAndName) {
        List<XmlElement> foundConfigNodes = moduleElement.getChildElementsWithinNamespace(name, moduleNamespace);
        if (foundConfigNodes.isEmpty()) {
            logger.debug("No config nodes {}:{} found in {}", moduleNamespace, name, moduleElement);
            logger.debug("Trying lookup of config nodes without specified namespace");
            foundConfigNodes = moduleElement.getChildElementsWithinNamespace(name,
                    XmlNetconfConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
            // In case module type or name element is not present in config it
            // would be matched with config type or name
            // We need to remove config type and name from available module
            // config elements
            foundConfigNodes.removeAll(typeAndName);
            logger.debug("Found {} config nodes {} without specified namespace in {}", foundConfigNodes.size(), name,
                    moduleElement);
        } else {
            List<XmlElement> foundWithoutNamespaceNodes = moduleElement.getChildElementsWithinNamespace(name,
                    XmlNetconfConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
            foundWithoutNamespaceNodes.removeAll(typeAndName);
            Preconditions.checkState(foundWithoutNamespaceNodes.isEmpty(),
                    "Element %s present multiple times with different namespaces: %s, %s", name, foundConfigNodes,
                    foundWithoutNamespaceNodes);
        }

        recognisedChildren.addAll(foundConfigNodes);
        return foundConfigNodes;
    }

    private static Map<String, AttributeIfc> reverseMap(Map<String, AttributeIfc> yangNameToAttr) {
        Map<String, AttributeIfc> reversednameToAtr = Maps.newHashMap();

        for (Entry<String, AttributeIfc> entry : yangNameToAttr.entrySet()) {
            reversednameToAtr.put(entry.getValue().getUpperCaseCammelCase(), entry.getValue());
        }

        return reversednameToAtr;
    }

}
