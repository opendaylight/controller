/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.netconf.util.xml.XmlElement;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CompositeAttributeReadingStrategy extends AbstractAttributeReadingStrategy<TOAttribute> {

    private final Map<String, AttributeReadingStrategy> innerStrategies;

    public CompositeAttributeReadingStrategy(TOAttribute attributeIfc,
            Map<String, AttributeReadingStrategy> innerStrategies) {
        super(attributeIfc);
        this.innerStrategies = innerStrategies;
    }

    @Override
    AttributeConfigElement readElementHook(List<XmlElement> configNodes) {

        Preconditions.checkState(configNodes.size() == 1, "This element should be present only once %s", configNodes);

        XmlElement complexElement = configNodes.get(0);

        Map<String, Object> innerMap = Maps.newHashMap();

        Map<String, AttributeIfc> inner = getAttributeIfc().getYangPropertiesToTypesMap();

        List<XmlElement> recognisedChildren = Lists.newArrayList();
        for (Entry<String, AttributeIfc> innerAttrEntry : inner.entrySet()) {
            List<XmlElement> childItem = complexElement.getChildElementsWithSameNamespace(innerAttrEntry.getKey());
            recognisedChildren.addAll(childItem);

            AttributeConfigElement resolvedInner = innerStrategies.get(innerAttrEntry.getKey()).readElement(childItem);

            innerMap.put(innerAttrEntry.getKey(), resolvedInner.getValue());
        }

        complexElement.checkUnrecognisedElements(recognisedChildren);

        return AttributeConfigElement.create(getAttributeIfc(), innerMap);
    }

}
