/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.util.xml.XmlElement;

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
    AttributeConfigElement readElementHook(List<XmlElement> configNodes) throws NetconfDocumentedException {
        List<Object> innerList = Lists.newArrayList();
        for (XmlElement configNode : configNodes) {
            innerList.add(innerStrategy.readElement(Lists.newArrayList(configNode)).getValue());
        }
        return AttributeConfigElement.create(getNullableDefault(), innerList);
    }

}
