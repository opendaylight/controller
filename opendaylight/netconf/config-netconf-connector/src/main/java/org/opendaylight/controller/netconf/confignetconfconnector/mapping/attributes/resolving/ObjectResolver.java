/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.resolving;

import java.util.Map;
import java.util.Map.Entry;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.AttributeIfcSwitchStatement;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.Services;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class ObjectResolver extends AttributeIfcSwitchStatement<AttributeResolvingStrategy<?, ? extends OpenType<?>>> {

    private final Services serviceTracker;
    private OpenType<?> openType;

    public ObjectResolver(Services serviceTracker) {
        this.serviceTracker = serviceTracker;
    }

    public Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> prepareResolving(
            Map<String, AttributeIfc> configDefinition) {
        Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> strategies = Maps.newHashMap();

        for (Entry<String, AttributeIfc> attrEntry : configDefinition.entrySet()) {
            strategies.put(attrEntry.getKey(),
                    prepareStrategy(attrEntry.getValue(), attrEntry.getValue().getOpenType()));
        }

        return strategies;
    }

    private AttributeResolvingStrategy<?, ? extends OpenType<?>> prepareStrategy(AttributeIfc attributeIfc,
            OpenType<?> openType) {

        this.openType = openType;
        return switchAttribute(attributeIfc);
    }

    private Map<String, String> createYangToJmxMapping(TOAttribute attributeIfc) {
        Map<String, String> retVal = Maps.newHashMap();
        for (Entry<String, AttributeIfc> entry : attributeIfc.getYangPropertiesToTypesMap().entrySet()) {
            retVal.put(entry.getKey(), (entry.getValue()).getLowerCaseCammelCase());
        }
        return retVal;
    }

    @Override
    protected AttributeResolvingStrategy<?, ? extends OpenType<?>> caseJavaAttribute(JavaAttribute attributeIfc) {
        if (attributeIfc.getOpenType() instanceof SimpleType<?>)
            return new SimpleAttributeResolvingStrategy((SimpleType<?>) openType);
        else if (attributeIfc.getOpenType() instanceof ArrayType<?>) {
            ArrayType<?> arrayType = (ArrayType<?>) openType;
            SimpleType<?> innerType = (SimpleType<?>) arrayType.getElementOpenType();
            AttributeResolvingStrategy<?, ? extends OpenType<?>> strat = new SimpleAttributeResolvingStrategy(innerType);
            return new ArrayAttributeResolvingStrategy(strat, arrayType);
        }
        throw new IllegalStateException(JavaAttribute.class + " can only provide open type " + SimpleType.class
                + " or " + ArrayType.class);
    }

    @Override
    protected AttributeResolvingStrategy<?, ? extends OpenType<?>> caseDependencyAttribute(
            DependencyAttribute attributeIfc) {
        return new ObjectNameAttributeResolvingStrategy(serviceTracker);
    }

    @Override
    protected AttributeResolvingStrategy<?, ? extends OpenType<?>> caseTOAttribute(TOAttribute attributeIfc) {
        CompositeType compositeType = (CompositeType) openType;
        Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> innerMap = Maps.newHashMap();
        for (String innerName : compositeType.keySet()) {
            Preconditions.checkState(attributeIfc instanceof TOAttribute, "Unexpected state, " + attributeIfc
                    + " should be instance of " + TOAttribute.class.getName());
            AttributeIfc innerAttributeIfc = attributeIfc.getJmxPropertiesToTypesMap().get(innerName);
            innerMap.put(innerAttributeIfc.getAttributeYangName(),
                    prepareStrategy(innerAttributeIfc, compositeType.getType(innerName)));
        }
        return new CompositeAttributeResolvingStrategy(innerMap, compositeType, createYangToJmxMapping(attributeIfc));
    }

    @Override
    protected AttributeResolvingStrategy<?, ? extends OpenType<?>> caseListAttribute(ListAttribute attributeIfc) {
        ArrayType<?> arrayType = (ArrayType<?>) openType;
        OpenType<?> innerType = arrayType.getElementOpenType();
        AttributeIfc inner = attributeIfc.getInnerAttribute();
        return new ArrayAttributeResolvingStrategy(prepareStrategy(inner, innerType), arrayType);
    }

}
