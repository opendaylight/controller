/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.config;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.management.ObjectName;
import javax.management.openmbean.OpenType;
import org.opendaylight.controller.config.facade.xml.mapping.IdentityMapping;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml.AttributeReadingStrategy;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml.ObjectXmlReader;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.mapping.AttributeMappingStrategy;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.mapping.ObjectMapper;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.resolving.AttributeResolvingStrategy;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.resolving.ObjectResolver;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.toxml.AttributeWritingStrategy;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.toxml.ObjectXmlWriter;
import org.opendaylight.controller.config.facade.xml.osgi.EnumResolver;
import org.opendaylight.controller.config.facade.xml.strategy.EditStrategyType;
import org.opendaylight.controller.config.util.BeanReader;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlMappingConstants;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class InstanceConfig {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceConfig.class);

    private final Map<String, AttributeIfc> yangToAttrConfig;
    private final String nullableDummyContainerName;
    private final Map<String, AttributeIfc> jmxToAttrConfig;
    private final BeanReader configRegistryClient;

    public InstanceConfig(BeanReader configRegistryClient, Map<String, AttributeIfc> yangNamesToAttributes,
                          String nullableDummyContainerName) {

        this.yangToAttrConfig = yangNamesToAttributes;
        this.nullableDummyContainerName = nullableDummyContainerName;
        this.jmxToAttrConfig = reverseMap(yangNamesToAttributes);
        this.configRegistryClient = configRegistryClient;
    }

    private Map<String, Object> getMappedConfiguration(ObjectName on, final EnumResolver enumResolver) {

        // TODO make field, mappingStrategies can be instantiated only once
        Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> mappingStrategies = new ObjectMapper()
                .prepareMapping(jmxToAttrConfig, enumResolver);

        Map<String, Object> toXml = Maps.newHashMap();

        for (Entry<String, AttributeIfc> configDefEntry : jmxToAttrConfig.entrySet()) {
            // Skip children runtime beans as they are mapped by InstanceRuntime
            if (configDefEntry.getValue() instanceof RuntimeBeanEntry){
                continue;
            }
            Object value = configRegistryClient.getAttributeCurrentValue(on, configDefEntry.getKey());
            try {
                AttributeMappingStrategy<?, ? extends OpenType<?>> attributeMappingStrategy = mappingStrategies
                        .get(configDefEntry.getKey());
                Optional<?> a = attributeMappingStrategy.mapAttribute(value);
                if (!a.isPresent()){
                    continue;
                }
                toXml.put(configDefEntry.getValue().getAttributeYangName(), a.get());
            } catch (Exception e) {
                throw new IllegalStateException("Unable to map value " + value + " to attribute "
                        + configDefEntry.getKey(), e);
            }
        }
        return toXml;
    }

    public Element toXml(ObjectName on, String namespace, Document document, Element rootElement, final EnumResolver enumResolver) {
        Map<String, AttributeWritingStrategy> strats = new ObjectXmlWriter().prepareWriting(yangToAttrConfig, document);
        Map<String, Object> mappedConfig = getMappedConfiguration(on, enumResolver);
        Element parentElement;
        if (nullableDummyContainerName != null) {
            Element dummyElement = XmlUtil.createElement(document, nullableDummyContainerName, Optional.of(namespace));
            rootElement.appendChild(dummyElement);
            parentElement = dummyElement;
        } else {
            parentElement = rootElement;
        }
        for (Entry<String, ?> mappingEntry : mappedConfig.entrySet()) {
            try {
                strats.get(mappingEntry.getKey()).writeElement(parentElement, namespace, mappingEntry.getValue());
            } catch (Exception e) {
                throw new IllegalStateException("Unable to write value " + mappingEntry.getValue() + " for attribute "
                        + mappingEntry.getValue(), e);
            }
        }
        return rootElement;
    }

    private void resolveConfiguration(InstanceConfigElementResolved mappedConfig, ServiceRegistryWrapper depTracker, final EnumResolver enumResolver) {

        // TODO make field, resolvingStrategies can be instantiated only once
        Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> resolvingStrategies = new ObjectResolver(
                depTracker).prepareResolving(yangToAttrConfig, enumResolver);

        for (Entry<String, AttributeConfigElement> configDefEntry : mappedConfig.getConfiguration().entrySet()) {
            AttributeConfigElement value = configDefEntry.getValue();
            String attributeName = configDefEntry.getKey();
            try {
                AttributeResolvingStrategy<?, ? extends OpenType<?>> attributeResolvingStrategy = resolvingStrategies
                        .get(attributeName);
                LOG.trace("Trying to set value {} of attribute {} with {}", value, attributeName, attributeResolvingStrategy);

                value.resolveValue(attributeResolvingStrategy, attributeName);
                value.setJmxName(
                        yangToAttrConfig.get(attributeName).getUpperCaseCammelCase());
            } catch (Exception e) {
                throw new IllegalStateException("Unable to resolve value " + value
                        + " to attribute " + attributeName, e);
            }
        }
    }

    public InstanceConfigElementResolved fromXml(XmlElement moduleElement, ServiceRegistryWrapper services, String moduleNamespace,
                                                 EditStrategyType defaultStrategy,
                                                 Map<String, Map<Date, IdentityMapping>> identityMap, final EnumResolver enumResolver) throws DocumentedException {
        Map<String, AttributeConfigElement> retVal = Maps.newHashMap();

        Map<String, AttributeReadingStrategy> strats = new ObjectXmlReader().prepareReading(yangToAttrConfig, identityMap);
        List<XmlElement> recognisedChildren = Lists.newArrayList();

        XmlElement typeElement = moduleElement.getOnlyChildElementWithSameNamespace(XmlMappingConstants.TYPE_KEY);
        XmlElement nameElement = moduleElement.getOnlyChildElementWithSameNamespace(XmlMappingConstants.NAME_KEY);
        List<XmlElement> typeAndNameElements = Lists.newArrayList(typeElement, nameElement);

        // if dummy container was defined in yang, set moduleElement to its content
        if (nullableDummyContainerName != null) {
            int size = moduleElement.getChildElements().size();
            int expectedChildNodes = 1 + typeAndNameElements.size();
            if (size > expectedChildNodes) {
                throw new DocumentedException("Error reading module " + typeElement.getTextContent() + " : " +
                        nameElement.getTextContent() + " - Expected " + expectedChildNodes +" child nodes, " +
                        "one of them with name " + nullableDummyContainerName +
                        ", got " + size + " elements.");
            }
            if (size == expectedChildNodes) {
                try {
                    moduleElement = moduleElement.getOnlyChildElement(nullableDummyContainerName, moduleNamespace);
                } catch (DocumentedException e) {
                    throw new DocumentedException("Error reading module " + typeElement.getTextContent() + " : " +
                            nameElement.getTextContent() + " - Expected child node with name " + nullableDummyContainerName +
                            "." + e.getMessage());
                }
            } // else 2 elements, no need to descend
        }

        for (Entry<String, AttributeReadingStrategy> readStratEntry : strats.entrySet()) {
            List<XmlElement> configNodes = getConfigNodes(moduleElement, moduleNamespace, readStratEntry.getKey(),
                    recognisedChildren, typeAndNameElements);
            AttributeConfigElement readElement = readStratEntry.getValue().readElement(configNodes);
            retVal.put(readStratEntry.getKey(), readElement);
        }

        recognisedChildren.addAll(typeAndNameElements);
        try {
            moduleElement.checkUnrecognisedElements(recognisedChildren);
        } catch (DocumentedException e) {
            throw new DocumentedException("Error reading module " + typeElement.getTextContent() + " : " +
                    nameElement.getTextContent() + " - " +
                    e.getMessage(), e.getErrorType(), e.getErrorTag(),e.getErrorSeverity(),e.getErrorInfo());
        }
        // TODO: add check for conflicts between global and local edit strategy
        String perInstanceEditStrategy = moduleElement.getAttribute(XmlMappingConstants.OPERATION_ATTR_KEY,
                XmlMappingConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        InstanceConfigElementResolved instanceConfigElementResolved = perInstanceEditStrategy.equals("") ? new InstanceConfigElementResolved(
                retVal, defaultStrategy) : new InstanceConfigElementResolved(perInstanceEditStrategy, retVal, defaultStrategy);

        resolveConfiguration(instanceConfigElementResolved, services, enumResolver);
        return instanceConfigElementResolved;
    }

    private List<XmlElement> getConfigNodes(XmlElement moduleElement, String moduleNamespace, String name,
            List<XmlElement> recognisedChildren, List<XmlElement> typeAndName) throws DocumentedException {
        List<XmlElement> foundConfigNodes = moduleElement.getChildElementsWithinNamespace(name, moduleNamespace);
        if (foundConfigNodes.isEmpty()) {
            LOG.debug("No config nodes {}:{} found in {}", moduleNamespace, name, moduleElement);
            LOG.debug("Trying lookup of config nodes without specified namespace");
            foundConfigNodes = moduleElement.getChildElementsWithinNamespace(name,
                    XmlMappingConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
            // In case module type or name element is not present in config it
            // would be matched with config type or name
            // We need to remove config type and name from available module
            // config elements
            foundConfigNodes.removeAll(typeAndName);
            LOG.debug("Found {} config nodes {} without specified namespace in {}", foundConfigNodes.size(), name,
                    moduleElement);
        } else {
            List<XmlElement> foundWithoutNamespaceNodes = moduleElement.getChildElementsWithinNamespace(name,
                    XmlMappingConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
            foundWithoutNamespaceNodes.removeAll(typeAndName);
            if (!foundWithoutNamespaceNodes.isEmpty()){
                throw new DocumentedException(String.format("Element %s present multiple times with different namespaces: %s, %s", name, foundConfigNodes,
                        foundWithoutNamespaceNodes),
                        DocumentedException.ErrorType.application,
                        DocumentedException.ErrorTag.invalid_value,
                        DocumentedException.ErrorSeverity.error);
            }
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
