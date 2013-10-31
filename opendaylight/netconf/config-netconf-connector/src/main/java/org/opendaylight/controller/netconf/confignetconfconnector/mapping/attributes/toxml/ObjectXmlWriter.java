/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.toxml;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.AttributeIfcSwitchStatement;
import org.w3c.dom.Document;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.SimpleType;
import java.util.Map;
import java.util.Map.Entry;

public class ObjectXmlWriter extends AttributeIfcSwitchStatement<AttributeWritingStrategy> {

    private Document document;
    private String key;

    public Map<String, AttributeWritingStrategy> prepareWriting(Map<String, AttributeIfc> yangToAttrConfig,
            Document document) {

        Map<String, AttributeWritingStrategy> preparedWriting = Maps.newHashMap();

        for (Entry<String, AttributeIfc> mappedAttributeEntry : yangToAttrConfig.entrySet()) {
            String key = mappedAttributeEntry.getKey();
            AttributeIfc value = mappedAttributeEntry.getValue();
            AttributeWritingStrategy strat = prepareWritingStrategy(key, value, document);
            preparedWriting.put(key, strat);
        }

        return preparedWriting;
    }

    public AttributeWritingStrategy prepareWritingStrategy(String key, AttributeIfc expectedAttr, Document document) {
        Preconditions.checkNotNull(expectedAttr, "Mbean attributes mismatch, unable to find expected attribute for %s",
                key);
        this.document = document;
        this.key = key;
        return switchAttribute(expectedAttr);
    }

    @Override
    protected AttributeWritingStrategy caseJavaAttribute(JavaAttribute attributeIfc) {

        if (attributeIfc.getOpenType() instanceof SimpleType<?>)
            return new SimpleAttributeWritingStrategy(document, key);
        else if (attributeIfc.getOpenType() instanceof ArrayType<?>) {
            AttributeWritingStrategy innerStrategy = new SimpleAttributeWritingStrategy(document, key);
            return new ArrayAttributeWritingStrategy(innerStrategy);
        }
        throw new IllegalStateException(JavaAttribute.class + " can only provide open type " + SimpleType.class
                + " or " + ArrayType.class);
    }

    @Override
    protected AttributeWritingStrategy caseDependencyAttribute(DependencyAttribute attributeIfc) {
        return new ObjectNameAttributeWritingStrategy(document, key);
    }

    @Override
    protected AttributeWritingStrategy caseTOAttribute(TOAttribute attributeIfc) {
        Map<String, AttributeWritingStrategy> innerStrats = Maps.newHashMap();
        String currentKey = key;
        for (Entry<String, AttributeIfc> innerAttrEntry : attributeIfc.getYangPropertiesToTypesMap().entrySet()) {

            AttributeWritingStrategy innerStrategy = prepareWritingStrategy(innerAttrEntry.getKey(),
                    innerAttrEntry.getValue(), document);
            innerStrats.put(innerAttrEntry.getKey(), innerStrategy);
        }

        return new CompositeAttributeWritingStrategy(document, currentKey, innerStrats);
    }

    @Override
    protected AttributeWritingStrategy caseListAttribute(ListAttribute attributeIfc) {
        AttributeIfc inner = attributeIfc.getInnerAttribute();
        AttributeWritingStrategy innerStrategy = prepareWritingStrategy(key, inner, document);
        return new ArrayAttributeWritingStrategy(innerStrategy);
    }

}
