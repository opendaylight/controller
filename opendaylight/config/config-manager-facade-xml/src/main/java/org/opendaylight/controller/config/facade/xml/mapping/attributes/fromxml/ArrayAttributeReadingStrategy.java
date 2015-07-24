/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.controller.config.facade.xml.strategy.EditStrategyType;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;

public class ArrayAttributeReadingStrategy extends AbstractAttributeReadingStrategy {

    private final AttributeReadingStrategy innerStrategy;

    /**
     * @param attributeIfc
     * @param innerStrategy
     */
    public ArrayAttributeReadingStrategy(String nullableDefault, AttributeReadingStrategy innerStrategy) {
        super(nullableDefault);
        this.innerStrategy = innerStrategy;
    }

    @Override
    AttributeConfigElement readElementHook(List<XmlElement> configNodes) throws DocumentedException {
        List<Object> innerList = Lists.newArrayList();
        EditStrategyType innerEditStrategy= null;
        for (XmlElement configNode : configNodes) {
            final AttributeConfigElement attributeConfigElement = innerStrategy.readElement(Lists.newArrayList(configNode));
            if(attributeConfigElement.getEditStrategy().isPresent()) {
                // TODO this sets the last operation for the entire array
                innerEditStrategy = attributeConfigElement.getEditStrategy().get();
            }
            innerList.add(attributeConfigElement.getValue());
        }
        return innerEditStrategy == null ? AttributeConfigElement.create(getNullableDefault(), innerList) :
                AttributeConfigElement.create(getNullableDefault(), innerList, innerEditStrategy);
    }

}
