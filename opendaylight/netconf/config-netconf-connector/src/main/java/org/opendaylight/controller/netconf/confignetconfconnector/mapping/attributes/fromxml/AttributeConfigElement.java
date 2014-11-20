/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml;

import com.google.common.base.Optional;
import javax.management.openmbean.OpenType;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.resolving.AttributeResolvingStrategy;

/**
 * Parsed xml element containing configuration for one attribute of an instance
 * of some module. Contains default value extracted from yang file.
 */
public class AttributeConfigElement {
    private final Object defaultValue;
    private final Object value;

    private Optional<?> resolvedValue;
    private Object resolvedDefaultValue;
    private String jmxName;

    public AttributeConfigElement(Object defaultValue, Object value) {
        this.defaultValue = defaultValue;
        this.value = value;
    }

    public void setJmxName(String jmxName) {
        this.jmxName = jmxName;
    }

    public String getJmxName() {
        return jmxName;
    }

    public void resolveValue(AttributeResolvingStrategy<?, ? extends OpenType<?>> attributeResolvingStrategy,
            String attrName) throws NetconfDocumentedException {
        resolvedValue = attributeResolvingStrategy.parseAttribute(attrName, value);
        Optional<?> resolvedDefault = attributeResolvingStrategy.parseAttribute(attrName, defaultValue);
        resolvedDefaultValue = resolvedDefault.isPresent() ? resolvedDefault.get() : null;
    }

    public static AttributeConfigElement create(Object nullableDefault, Object value) {
        return new AttributeConfigElement(nullableDefault, value);
    }

    public static AttributeConfigElement createNullValue(Object nullableDefault) {
        return new AttributeConfigElement(nullableDefault, null);
    }

    public Object getValue() {
        return value;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Optional<?> getResolvedValue() {
        return resolvedValue;
    }

    public Object getResolvedDefaultValue() {
        return resolvedDefaultValue;
    }

    @Override
    public String toString() {
        return "AttributeConfigElement [defaultValue=" + defaultValue + ", value=" + value + "]";
    }

}
