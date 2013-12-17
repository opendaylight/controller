/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListDependenciesAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.AttributeIfcSwitchStatement;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ServiceRegistryWrapper;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.util.Map;
import java.util.Map.Entry;

public class ObjectMapper extends AttributeIfcSwitchStatement<AttributeMappingStrategy<?, ? extends OpenType<?>>> {

    private final ServiceRegistryWrapper dependencyTracker;

    public ObjectMapper(ServiceRegistryWrapper depTracker) {
        this.dependencyTracker = depTracker;
    }

    public Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> prepareMapping(
            Map<String, AttributeIfc> configDefinition) {
        Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> strategies = Maps.newHashMap();

        for (Entry<String, AttributeIfc> attrEntry : configDefinition.entrySet()) {
            strategies.put(attrEntry.getKey(), prepareStrategy(attrEntry.getValue()));
        }

        return strategies;
    }

    public AttributeMappingStrategy<?, ? extends OpenType<?>> prepareStrategy(AttributeIfc attributeIfc) {

        if(attributeIfc instanceof DependencyAttribute) {
            serviceNameOfDepAttr = ((DependencyAttribute)attributeIfc).getDependency().getSie().getQName().getLocalName();
            namespaceOfDepAttr = ((DependencyAttribute)attributeIfc).getDependency().getSie().getQName().getNamespace().toString();
        }

        return switchAttribute(attributeIfc);
    }

    private Map<String, String> createJmxToYangMapping(TOAttribute attributeIfc) {
        Map<String, String> retVal = Maps.newHashMap();
        for (Entry<String, AttributeIfc> entry : attributeIfc.getJmxPropertiesToTypesMap().entrySet()) {
            retVal.put(entry.getKey(), (entry.getValue()).getAttributeYangName());
        }
        return retVal;
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseJavaSimpleAttribute(SimpleType<?> openType) {
        return new SimpleAttributeMappingStrategy(openType);
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseJavaArrayAttribute(ArrayType<?> openType) {

        AttributeMappingStrategy<?, ? extends OpenType<?>> innerStrategy = new SimpleAttributeMappingStrategy(
                (SimpleType<?>) openType.getElementOpenType());
        return new ArrayAttributeMappingStrategy(openType, innerStrategy);
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseJavaCompositeAttribute(CompositeType openType) {
        Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> innerStrategies = Maps.newHashMap();

        Map<String, String> attributeMapping = Maps.newHashMap();

        for (String innerAttributeKey : openType.keySet()) {

            innerStrategies.put(innerAttributeKey, caseJavaAttribute(openType.getType(innerAttributeKey)));
            attributeMapping.put(innerAttributeKey, innerAttributeKey);
        }

        return new CompositeAttributeMappingStrategy(openType, innerStrategies, attributeMapping);
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseJavaUnionAttribute(OpenType<?> openType) {
        Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> innerStrategies = Maps.newHashMap();

        Map<String, String> attributeMapping = Maps.newHashMap();

        CompositeType compositeType = (CompositeType) openType;
        for (String innerAttributeKey : compositeType.keySet()) {

            innerStrategies.put(innerAttributeKey, caseJavaAttribute(compositeType.getType(innerAttributeKey)));
            attributeMapping.put(innerAttributeKey, innerAttributeKey);
        }

        return new UnionCompositeAttributeMappingStrategy(compositeType, innerStrategies, attributeMapping);
    }

    private String serviceNameOfDepAttr;
    private String namespaceOfDepAttr;

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseDependencyAttribute(
            SimpleType<?> openType) {
        return new ObjectNameAttributeMappingStrategy(openType, dependencyTracker,
                serviceNameOfDepAttr, namespaceOfDepAttr);
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseTOAttribute(CompositeType openType) {
        Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> innerStrategies = Maps.newHashMap();

        Preconditions.checkState(lastAttribute instanceof TOAttribute);
        TOAttribute lastTO = (TOAttribute) lastAttribute;

        for (Entry<String, AttributeIfc> innerAttrEntry : ((TOAttribute)lastAttribute).getJmxPropertiesToTypesMap().entrySet()) {
            innerStrategies.put(innerAttrEntry.getKey(), prepareStrategy(innerAttrEntry.getValue()));
        }

        return new CompositeAttributeMappingStrategy(openType, innerStrategies,
                createJmxToYangMapping(lastTO));
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseListAttribute(ArrayType<?> openType) {
        Preconditions.checkState(lastAttribute instanceof ListAttribute);
        return new ArrayAttributeMappingStrategy(openType,
                prepareStrategy(((ListAttribute) lastAttribute).getInnerAttribute()));
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseListDependeciesAttribute(ArrayType<?> openType) {
        Preconditions.checkState(lastAttribute instanceof ListDependenciesAttribute);
        return new ArrayAttributeMappingStrategy(openType, caseDependencyAttribute(SimpleType.OBJECTNAME));
    }

}
