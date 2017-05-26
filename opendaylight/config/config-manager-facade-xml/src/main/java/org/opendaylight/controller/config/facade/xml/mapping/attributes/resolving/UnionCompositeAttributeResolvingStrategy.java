/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.resolving;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;

final class UnionCompositeAttributeResolvingStrategy extends CompositeAttributeResolvingStrategy {

    UnionCompositeAttributeResolvingStrategy(Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> innerTypes,
                                        CompositeType openType, Map<String, String> yangToJavaAttrMapping) {
        super(innerTypes, openType, yangToJavaAttrMapping);
    }

    protected Map<String, Object> preprocessValueMap(Map<?, ?> valueMap) {
        CompositeType openType = getOpenType();

        Preconditions.checkArgument(
                valueMap.size() == 1 && valueMap.containsKey(JavaAttribute.DESCRIPTION_OF_VALUE_ATTRIBUTE_FOR_UNION),
                "Unexpected structure of incoming map, expecting one element under %s, but was %s",
                JavaAttribute.DESCRIPTION_OF_VALUE_ATTRIBUTE_FOR_UNION, valueMap);

        Map<String, Object> newMap = Maps.newHashMap();

        for (String key : openType.keySet()) {
            if (openType.getDescription(key).equals(JavaAttribute.DESCRIPTION_OF_VALUE_ATTRIBUTE_FOR_UNION)){
                newMap.put(key, valueMap.get(JavaAttribute.DESCRIPTION_OF_VALUE_ATTRIBUTE_FOR_UNION));
            } else {
                newMap.put(key, null);
            }
        }
        return newMap;
    }
}
