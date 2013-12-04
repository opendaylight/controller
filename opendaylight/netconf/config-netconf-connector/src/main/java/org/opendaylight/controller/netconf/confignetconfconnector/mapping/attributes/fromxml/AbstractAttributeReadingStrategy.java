/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml;

import org.opendaylight.controller.netconf.util.xml.XmlElement;

import java.util.List;

public abstract class AbstractAttributeReadingStrategy implements AttributeReadingStrategy {

    private final String nullableDefault;

    public AbstractAttributeReadingStrategy(String nullableDefault) {
        this.nullableDefault = nullableDefault;
    }

    public String getNullableDefault() {
        return nullableDefault;
    }

    @Override
    public AttributeConfigElement readElement(List<XmlElement> configNodes) {
        if (configNodes.size() == 0)
            return AttributeConfigElement.createNullValue(postprocessNullableDefault(nullableDefault));

        return readElementHook(configNodes);
    }

    abstract AttributeConfigElement readElementHook(List<XmlElement> configNodes);

    protected Object postprocessNullableDefault(String nullableDefault) {
        return nullableDefault;
    }
}
