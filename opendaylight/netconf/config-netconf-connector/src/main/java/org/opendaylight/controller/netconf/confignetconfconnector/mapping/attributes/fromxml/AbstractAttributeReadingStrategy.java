/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml;

import java.util.List;

import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.netconf.util.xml.XmlElement;

public abstract class AbstractAttributeReadingStrategy<A extends AttributeIfc> implements AttributeReadingStrategy {

    private final A attributeIfc;

    public AbstractAttributeReadingStrategy(A attributeIfc) {
        this.attributeIfc = attributeIfc;
    }

    public A getAttributeIfc() {
        return attributeIfc;
    }

    @Override
    public AttributeConfigElement readElement(List<XmlElement> configNodes) {
        if (configNodes.size() == 0)
            return AttributeConfigElement.createNullValue(attributeIfc);

        return readElementHook(configNodes);
    }

    abstract AttributeConfigElement readElementHook(List<XmlElement> configNodes);

}
