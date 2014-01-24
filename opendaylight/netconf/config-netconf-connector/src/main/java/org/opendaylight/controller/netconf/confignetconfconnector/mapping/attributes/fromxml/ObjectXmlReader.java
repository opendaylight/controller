/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListDependenciesAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.AttributeIfcSwitchStatement;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfig;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ObjectXmlReader extends AttributeIfcSwitchStatement<AttributeReadingStrategy> {

    private String key;
    private Map<String, Map<Date, EditConfig.IdentityMapping>> identityMap;

    public Map<String, AttributeReadingStrategy> prepareReading(Map<String, AttributeIfc> yangToAttrConfig, Map<String, Map<Date,EditConfig.IdentityMapping>> identityMap) {
        Map<String, AttributeReadingStrategy> strategies = Maps.newHashMap();
        this.identityMap = identityMap;

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
    protected AttributeReadingStrategy caseJavaBinaryAttribute(OpenType<?> openType) {
        return new SimpleBinaryAttributeReadingStrategy(lastAttribute.getNullableDefault());
    }

    @Override
    protected AttributeReadingStrategy caseJavaUnionAttribute(OpenType<?> openType) {
        String mappingKey = JavaAttribute.DESCRIPTION_OF_VALUE_ATTRIBUTE_FOR_UNION;
        return new SimpleUnionAttributeReadingStrategy(lastAttribute.getNullableDefault(), mappingKey);
    }

    @Override
    public AttributeReadingStrategy caseJavaSimpleAttribute(SimpleType<?> openType) {
        return new SimpleAttributeReadingStrategy(lastAttribute.getNullableDefault());
    }

    @Override
    public AttributeReadingStrategy caseJavaArrayAttribute(ArrayType<?> openType) {
        SimpleAttributeReadingStrategy innerStrategy = new SimpleAttributeReadingStrategy(lastAttribute.getNullableDefault());
        return new ArrayAttributeReadingStrategy(lastAttribute.getNullableDefault(), innerStrategy);
    }

    @Override
    public AttributeReadingStrategy caseJavaCompositeAttribute(CompositeType openType) {
        Preconditions.checkState(openType.keySet().size() == 1, "Unexpected number of elements for open type %s, should be 1", openType);
        String mappingKey = openType.keySet().iterator().next();
        return new SimpleCompositeAttributeReadingStrategy(lastAttribute.getNullableDefault(), mappingKey);
    }

    @Override
    protected AttributeReadingStrategy caseJavaIdentityRefAttribute(OpenType<?> openType) {
        Preconditions.checkState(openType instanceof CompositeType);
        Set<String> keys = ((CompositeType) openType).keySet();
        Preconditions.checkState(keys.size() == 1, "Unexpected number of elements for open type %s, should be 1", openType);
        String mappingKey = keys.iterator().next();
        return new SimpleIdentityRefAttributeReadingStrategy(lastAttribute.getNullableDefault(), mappingKey, identityMap);
    }

    @Override
    protected AttributeReadingStrategy caseDependencyAttribute(SimpleType<?> openType) {
        return new ObjectNameAttributeReadingStrategy(lastAttribute.getNullableDefault());
    }

    @Override
    protected AttributeReadingStrategy caseTOAttribute(CompositeType openType) {
        Preconditions.checkState(lastAttribute instanceof TOAttribute);
        Map<String, AttributeIfc> inner = ((TOAttribute)lastAttribute).getYangPropertiesToTypesMap();

        Map<String, AttributeReadingStrategy> innerStrategies = Maps.newHashMap();

        for (Entry<String, AttributeIfc> innerAttrEntry : inner.entrySet()) {
            AttributeReadingStrategy innerStrat = prepareReadingStrategy(innerAttrEntry.getKey(),
                    innerAttrEntry.getValue());
            innerStrategies.put(innerAttrEntry.getKey(), innerStrat);
        }

        return new CompositeAttributeReadingStrategy(lastAttribute.getNullableDefault(), innerStrategies);
    }

    @Override
    protected AttributeReadingStrategy caseListAttribute(ArrayType<?> openType) {
        Preconditions.checkState(lastAttribute instanceof ListAttribute);
        AttributeReadingStrategy innerStrategy = prepareReadingStrategy(key, ((ListAttribute) lastAttribute).getInnerAttribute());
        return new ArrayAttributeReadingStrategy(lastAttribute.getNullableDefault(), innerStrategy);
    }

    @Override
    protected AttributeReadingStrategy caseListDependeciesAttribute(ArrayType<?> openType) {
        Preconditions.checkState(lastAttribute instanceof ListDependenciesAttribute);
        AttributeReadingStrategy innerStrategy = caseDependencyAttribute(SimpleType.OBJECTNAME);
        return new ArrayAttributeReadingStrategy(lastAttribute.getNullableDefault(), innerStrategy);
    }

}
