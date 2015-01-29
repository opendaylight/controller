/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.resolving;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.util.NetconfUtil;
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
    public Optional<CompositeDataSupport> parseAttribute(String attrName, Object value) throws NetconfDocumentedException {

        if (value == null) {
            return Optional.absent();
        }

        NetconfUtil.checkType(value, Map.class);
        Map<?, ?> valueMap = (Map<?, ?>) value;
        valueMap = preprocessValueMap(valueMap);

        Map<String, Object> items = Maps.newHashMap();
        Map<String, OpenType<?>> openTypes = Maps.newHashMap();

        for (Object innerAttrName : innerTypes.keySet()) {
            Preconditions.checkState(innerAttrName instanceof String, "Attribute name must be string");
            String innerAttrNameStr = (String) innerAttrName;

            AttributeResolvingStrategy<?, ? extends OpenType<?>> attributeResolvingStrategy = innerTypes
                    .get(innerAttrName);

            Object valueToParse = valueMap.get(innerAttrName);

            Optional<?> parsedInnerValue = attributeResolvingStrategy.parseAttribute(innerAttrNameStr, valueToParse);

            openTypes.put(innerAttrNameStr, attributeResolvingStrategy.getOpenType());

            items.put(yangToJavaAttrMapping.get(innerAttrNameStr),
                    parsedInnerValue.isPresent() ? parsedInnerValue.get() : null);
        }

        CompositeDataSupport parsedValue;
        try {
            parsedValue = new CompositeDataSupport(getOpenType(), items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("An error occured during restoration of composite type " + this
                    + " for attribute " + attrName + " from value " + value, e);
        }

        LOG.debug("Attribute {} : {} parsed to type {} as {}", attrName, value, getOpenType(), parsedValue);

        return Optional.of(parsedValue);
    }

    protected Map<?, ?> preprocessValueMap(Map<?, ?> valueMap) {
        return valueMap;
    }
}
