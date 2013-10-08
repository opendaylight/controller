/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml;

import com.google.common.collect.Lists;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.netconf.util.xml.XmlElement;

import java.util.List;

public class ArrayAttributeReadingStrategy extends AbstractAttributeReadingStrategy<AttributeIfc> {

    private final AttributeReadingStrategy innerStrategy;

    /**
     * @param attributeIfc
     * @param innerStrategy
     */
    public ArrayAttributeReadingStrategy(AttributeIfc attributeIfc, AttributeReadingStrategy innerStrategy) {
        super(attributeIfc);
        this.innerStrategy = innerStrategy;
    }

    @Override
    AttributeConfigElement readElementHook(List<XmlElement> configNodes) {
        List<Object> innerList = Lists.newArrayList();
        for (int i = 0; i < configNodes.size(); i++) {
            innerList.add(innerStrategy.readElement(Lists.newArrayList(configNodes.get(i))).getValue());
        }
        return AttributeConfigElement.create(getAttributeIfc(), innerList);
    }

}
