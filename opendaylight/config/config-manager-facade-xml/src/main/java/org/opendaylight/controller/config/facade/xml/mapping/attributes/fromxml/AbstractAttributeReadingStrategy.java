/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml;

import java.util.List;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;

public abstract class AbstractAttributeReadingStrategy implements AttributeReadingStrategy {

    private final String nullableDefault;

    public AbstractAttributeReadingStrategy(final String nullableDefault) {
        this.nullableDefault = nullableDefault;
    }

    public String getNullableDefault() {
        return nullableDefault;
    }

    @Override
    public AttributeConfigElement readElement(final List<XmlElement> configNodes) throws DocumentedException {
        if (configNodes.isEmpty()) {
            return AttributeConfigElement.createNullValue(postprocessNullableDefault(nullableDefault));
        }
        return readElementHook(configNodes);
    }

    abstract AttributeConfigElement readElementHook(List<XmlElement> configNodes) throws DocumentedException;

    @SuppressWarnings("checkstyle:hiddenField")
    protected Object postprocessNullableDefault(final String nullableDefault) {
        return nullableDefault;
    }
}
