/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.config.facade.xml.strategy.EditStrategyType;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlMappingConstants;

public class CompositeAttributeReadingStrategy extends AbstractAttributeReadingStrategy {

    private final Map<String, AttributeReadingStrategy> innerStrategies;

    public CompositeAttributeReadingStrategy(String nullableDefault,
            Map<String, AttributeReadingStrategy> innerStrategies) {
        super(nullableDefault);
        this.innerStrategies = innerStrategies;
    }

    @Override
    AttributeConfigElement readElementHook(List<XmlElement> configNodes) throws DocumentedException {

        Preconditions.checkState(configNodes.size() == 1, "This element should be present only once %s", configNodes);

        XmlElement complexElement = configNodes.get(0);

        Map<String, Object> innerMap = Maps.newHashMap();

        List<XmlElement> recognisedChildren = Lists.newArrayList();
        for (Entry<String, AttributeReadingStrategy> innerAttrEntry : innerStrategies.entrySet()) {
            List<XmlElement> childItem = complexElement.getChildElementsWithSameNamespace(
                    innerAttrEntry.getKey());
            recognisedChildren.addAll(childItem);

            AttributeConfigElement resolvedInner = innerAttrEntry.getValue().readElement(childItem);

            Object value = resolvedInner.getValue();
            if(value == null) {
                value = resolvedInner.getDefaultValue();
            }

            innerMap.put(innerAttrEntry.getKey(), value);
        }

        complexElement.checkUnrecognisedElements(recognisedChildren);

        String perInstanceEditStrategy = complexElement.getAttribute(XmlMappingConstants.OPERATION_ATTR_KEY,
                XmlMappingConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        return Strings.isNullOrEmpty(perInstanceEditStrategy) ? AttributeConfigElement.create(getNullableDefault(), innerMap) :
                AttributeConfigElement.create(getNullableDefault(), innerMap, EditStrategyType.valueOf(perInstanceEditStrategy));
    }

}
