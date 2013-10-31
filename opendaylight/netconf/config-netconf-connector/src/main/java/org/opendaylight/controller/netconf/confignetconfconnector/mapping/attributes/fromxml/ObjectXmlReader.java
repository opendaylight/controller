/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml;

import com.google.common.collect.Maps;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.AttributeIfcSwitchStatement;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.SimpleType;
import java.util.Map;
import java.util.Map.Entry;

public class ObjectXmlReader extends AttributeIfcSwitchStatement<AttributeReadingStrategy> {

    private String key;

    public Map<String, AttributeReadingStrategy> prepareReading(Map<String, AttributeIfc> yangToAttrConfig) {
        Map<String, AttributeReadingStrategy> strategies = Maps.newHashMap();

        for (Entry<String, AttributeIfc> attributeEntry : yangToAttrConfig.entrySet()) {
            AttributeReadingStrategy strat = prepareReadingStrategy(attributeEntry.getKey(), attributeEntry.getValue());
            strategies.put(attributeEntry.getKey(), strat);
        }
        return strategies;
    }

    private AttributeReadingStrategy prepareReadingStrategy(String key, AttributeIfc attributeIfc) {
        this.key = key;
        return switchAttribute(attributeIfc);
    }

    @Override
    protected AttributeReadingStrategy caseJavaAttribute(JavaAttribute attributeIfc) {
        if (attributeIfc.getOpenType() instanceof SimpleType<?>)
            return new SimpleAttributeReadingStrategy(attributeIfc);
        else if (attributeIfc.getOpenType() instanceof ArrayType<?>) {
            SimpleAttributeReadingStrategy innerStrategy = new SimpleAttributeReadingStrategy(
                    ((ArrayType<?>) attributeIfc.getOpenType()).getElementOpenType());
            return new ArrayAttributeReadingStrategy(attributeIfc, innerStrategy);
        }
        throw new IllegalStateException(JavaAttribute.class + " can only provide open type " + SimpleType.class
                + " or " + ArrayType.class);
    }

    @Override
    protected AttributeReadingStrategy caseDependencyAttribute(DependencyAttribute attributeIfc) {
        return new ObjectNameAttributeReadingStrategy(attributeIfc);
    }

    @Override
    protected AttributeReadingStrategy caseTOAttribute(TOAttribute attributeIfc) {
        Map<String, AttributeIfc> inner = attributeIfc.getYangPropertiesToTypesMap();
        Map<String, AttributeReadingStrategy> innerStrategies = Maps.newHashMap();

        for (Entry<String, AttributeIfc> innerAttrEntry : inner.entrySet()) {
            AttributeReadingStrategy innerStrat = prepareReadingStrategy(innerAttrEntry.getKey(),
                    innerAttrEntry.getValue());
            innerStrategies.put(innerAttrEntry.getKey(), innerStrat);
        }

        return new CompositeAttributeReadingStrategy(attributeIfc, innerStrategies);
    }

    @Override
    protected AttributeReadingStrategy caseListAttribute(ListAttribute attributeIfc) {
        AttributeIfc innerAttr = attributeIfc.getInnerAttribute();
        AttributeReadingStrategy innerStrategy = prepareReadingStrategy(key, innerAttr);
        return new ArrayAttributeReadingStrategy(attributeIfc, innerStrategy);
    }

}
