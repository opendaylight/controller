/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.toxml;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.AttributeIfcSwitchStatement;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListDependenciesAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.w3c.dom.Document;

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
    protected AttributeWritingStrategy caseJavaBinaryAttribute(OpenType<?> openType) {
        return new SimpleBinaryAttributeWritingStrategy(document, key);
    }

    @Override
    protected AttributeWritingStrategy caseJavaEnumAttribute(final OpenType<?> openType) {
        return new SimpleAttributeWritingStrategy(document, key);
    }

    @Override
    protected AttributeWritingStrategy caseJavaSimpleAttribute(SimpleType<?> openType) {
        return new SimpleAttributeWritingStrategy(document, key);
    }

    @Override
    protected AttributeWritingStrategy caseJavaArrayAttribute(ArrayType<?> openType) {
        AttributeWritingStrategy innerStrategy = new SimpleAttributeWritingStrategy(document, key);
        return new ArrayAttributeWritingStrategy(innerStrategy);
    }

    @Override
    protected AttributeWritingStrategy caseJavaIdentityRefAttribute(OpenType<?> openType) {
        return new SimpleIdentityRefAttributeWritingStrategy(document, key);
    }

    @Override
    protected AttributeWritingStrategy caseJavaCompositeAttribute(CompositeType openType) {
        return new SimpleCompositeAttributeWritingStrategy(document, key);
    }

    @Override
    protected AttributeWritingStrategy caseJavaUnionAttribute(OpenType<?> openType) {
        return new SimpleUnionAttributeWritingStrategy(document, key);
    }

    @Override
    protected AttributeWritingStrategy caseDependencyAttribute(SimpleType<?> openType) {
        return new ObjectNameAttributeWritingStrategy(document, key);
    }

    @Override
    protected AttributeWritingStrategy caseTOAttribute(CompositeType openType) {
        Preconditions.checkState(getLastAttribute() instanceof TOAttribute);

        Map<String, AttributeWritingStrategy> innerStrats = Maps.newHashMap();
        String currentKey = key;
        for (Entry<String, AttributeIfc> innerAttrEntry : ((TOAttribute) getLastAttribute()).getYangPropertiesToTypesMap().entrySet()) {

            AttributeWritingStrategy innerStrategy = prepareWritingStrategy(innerAttrEntry.getKey(),
                    innerAttrEntry.getValue(), document);
            innerStrats.put(innerAttrEntry.getKey(), innerStrategy);
        }

        return new CompositeAttributeWritingStrategy(document, currentKey, innerStrats);
    }

    @Override
    protected AttributeWritingStrategy caseListAttribute(ArrayType<?> openType) {
        Preconditions.checkState(getLastAttribute() instanceof ListAttribute);
        AttributeIfc innerAttribute = ((ListAttribute) getLastAttribute()).getInnerAttribute();

        AttributeWritingStrategy innerStrategy = prepareWritingStrategy(key, innerAttribute, document);
        return new ArrayAttributeWritingStrategy(innerStrategy);
    }

    @Override
    protected AttributeWritingStrategy caseListDependeciesAttribute(ArrayType<?> openType) {
        Preconditions.checkState(getLastAttribute() instanceof ListDependenciesAttribute);
        AttributeWritingStrategy innerStrategy = caseDependencyAttribute(SimpleType.OBJECTNAME);
        return new ArrayAttributeWritingStrategy(innerStrategy);
    }

}
