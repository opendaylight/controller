/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.resolving;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.config.facade.xml.util.Util;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CompositeAttributeResolvingStrategy extends
        AbstractAttributeResolvingStrategy<CompositeDataSupport, CompositeType> {
    private final Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> innerTypes;
    private final Map<String, String> yangToJavaAttrMapping;

    private static final Logger LOG = LoggerFactory.getLogger(CompositeAttributeResolvingStrategy.class);

    CompositeAttributeResolvingStrategy(Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> innerTypes,
            CompositeType openType, Map<String, String> yangToJavaAttrMapping) {
        super(openType);
        this.innerTypes = innerTypes;
        this.yangToJavaAttrMapping = yangToJavaAttrMapping;
    }

    @Override
    public String toString() {
        return "ResolvedCompositeAttribute [" + innerTypes + "]";
    }

    @Override
    public Optional<CompositeDataSupport> parseAttribute(String attrName, Object value) throws DocumentedException {

        if (value == null) {
            return Optional.absent();
        }

        Util.checkType(value, Map.class);
        Map<?, ?> valueMap = (Map<?, ?>) value;
        valueMap = preprocessValueMap(valueMap);

        Map<String, Object> items = Maps.newHashMap();
        Map<String, OpenType<?>> openTypes = Maps.newHashMap();

        final String[] names = new String[getOpenType().keySet().size()];
        final String[] descriptions = new String[getOpenType().keySet().size()];
        OpenType<?>[] itemTypes = new OpenType[names.length];
        int i = 0;

        for (Object innerAttrName : innerTypes.keySet()) {
            Preconditions.checkState(innerAttrName instanceof String, "Attribute name must be string");
            String innerAttrNameStr = (String) innerAttrName;

            AttributeResolvingStrategy<?, ? extends OpenType<?>> attributeResolvingStrategy = innerTypes
                    .get(innerAttrName);

            Object valueToParse = valueMap.get(innerAttrName);

            Optional<?> parsedInnerValue = attributeResolvingStrategy.parseAttribute(innerAttrNameStr, valueToParse);

            if(attributeResolvingStrategy instanceof EnumAttributeResolvingStrategy) {
                // Open type for enum contain the class name necessary for its resolution, however in a DTO
                // the open type need to be just SimpleType.STRING so that JMX is happy
                // After the enum attribute is resolved, change its open type back to STRING
                openTypes.put(innerAttrNameStr, SimpleType.STRING);
            } else {
                openTypes.put(innerAttrNameStr, attributeResolvingStrategy.getOpenType());
            }

            items.put(yangToJavaAttrMapping.get(innerAttrNameStr),
                    parsedInnerValue.isPresent() ? parsedInnerValue.get() : null);

            // fill names + item types in order to reconstruct the open type for current attribute
            names[i] = yangToJavaAttrMapping.get(innerAttrNameStr);
            descriptions[i] = getOpenType().getDescription(names[i]);
            itemTypes[i] = openTypes.get(innerAttrNameStr);
            i++;
        }

        CompositeDataSupport parsedValue;
        try {
            LOG.trace("Attribute {} with open type {}. Reconstructing open type.", attrName, getOpenType());
            setOpenType(new CompositeType(getOpenType().getTypeName(), getOpenType().getDescription(), names, descriptions, itemTypes));
            LOG.debug("Attribute {}. Open type reconstructed to {}", attrName, getOpenType(), getOpenType());
            parsedValue = new CompositeDataSupport(getOpenType(), items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("An error occurred during restoration of composite type " + this
                    + " for attribute " + attrName + " from value " + value, e);
        }

        LOG.debug("Attribute {} : {} parsed to type {} as {}", attrName, value, getOpenType(), parsedValue);

        return Optional.of(parsedValue);
    }


    protected Map<?, ?> preprocessValueMap(Map<?, ?> valueMap) {
        return valueMap;
    }


}
