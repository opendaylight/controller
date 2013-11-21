/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping;

import com.google.common.collect.Maps;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.AttributeIfcSwitchStatement;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.Services;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.util.Map;
import java.util.Map.Entry;

public class ObjectMapper extends AttributeIfcSwitchStatement<AttributeMappingStrategy<?, ? extends OpenType<?>>> {

    private final Services dependencyTracker;

    public ObjectMapper(Services depTracker) {
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
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseJavaAttribute(JavaAttribute attributeIfc) {

        if (attributeIfc.getOpenType() instanceof SimpleType<?>)
            return new SimpleAttributeMappingStrategy((SimpleType<?>) attributeIfc.getOpenType());
        else if (attributeIfc.getOpenType() instanceof ArrayType<?>) {
            ArrayType<?> arrayType = (ArrayType<?>) attributeIfc.getOpenType();
            AttributeMappingStrategy<?, ? extends OpenType<?>> innerStrategy = new SimpleAttributeMappingStrategy(
                    (SimpleType<?>) arrayType.getElementOpenType());
            return new ArrayAttributeMappingStrategy(arrayType, innerStrategy);
        }
        throw new IllegalStateException(JavaAttribute.class + " can only provide open type " + SimpleType.class
                + " or " + ArrayType.class);
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseDependencyAttribute(
            DependencyAttribute attributeIfc) {
        String serviceName = attributeIfc.getDependency().getSie().getQName().getLocalName();
        String namespace = attributeIfc.getDependency().getSie().getQName().getNamespace().toString();
        return new ObjectNameAttributeMappingStrategy((SimpleType<?>) attributeIfc.getOpenType(), dependencyTracker,
                serviceName, namespace);
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseTOAttribute(TOAttribute attributeIfc) {
        Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> innerStrategies = Maps.newHashMap();

        for (Entry<String, AttributeIfc> innerAttrEntry : attributeIfc.getJmxPropertiesToTypesMap().entrySet()) {
            innerStrategies.put(innerAttrEntry.getKey(), prepareStrategy(innerAttrEntry.getValue()));
        }

        return new CompositeAttributeMappingStrategy((CompositeType) attributeIfc.getOpenType(), innerStrategies,
                createJmxToYangMapping(attributeIfc));
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseListAttribute(ListAttribute attributeIfc) {
        return new ArrayAttributeMappingStrategy(attributeIfc.getOpenType(),
                prepareStrategy(attributeIfc.getInnerAttribute()));
    }

}
