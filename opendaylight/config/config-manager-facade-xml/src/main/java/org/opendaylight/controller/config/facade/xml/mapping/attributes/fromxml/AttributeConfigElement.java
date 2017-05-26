/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml;

import com.google.common.base.Optional;
import javax.management.openmbean.OpenType;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.resolving.AttributeResolvingStrategy;
import org.opendaylight.controller.config.facade.xml.strategy.EditStrategyType;
import org.opendaylight.controller.config.util.xml.DocumentedException;

/**
 * Parsed xml element containing configuration for one attribute of an instance
 * of some module. Contains default value extracted from yang file.
 */
public class AttributeConfigElement {
    private final Object defaultValue;
    private final Object value;
    private final Optional<EditStrategyType> editStrategy;

    private Optional<?> resolvedValue;
    private Object resolvedDefaultValue;
    private String jmxName;

    public AttributeConfigElement(Object defaultValue, Object value, final EditStrategyType editStrategyType) {
        this.defaultValue = defaultValue;
        this.value = value;
        this.editStrategy = Optional.fromNullable(editStrategyType);
    }

    public void setJmxName(String jmxName) {
        this.jmxName = jmxName;
    }

    public String getJmxName() {
        return jmxName;
    }

    public void resolveValue(AttributeResolvingStrategy<?, ? extends OpenType<?>> attributeResolvingStrategy,
            String attrName) throws DocumentedException {
        resolvedValue = attributeResolvingStrategy.parseAttribute(attrName, value);
        Optional<?> resolvedDefault = attributeResolvingStrategy.parseAttribute(attrName, defaultValue);
        resolvedDefaultValue = resolvedDefault.isPresent() ? resolvedDefault.get() : null;
    }

    public Optional<EditStrategyType> getEditStrategy() {
        return editStrategy;
    }

    public static AttributeConfigElement create(Object nullableDefault, Object value) {
        return new AttributeConfigElement(nullableDefault, value, null);
    }

    public static AttributeConfigElement createNullValue(Object nullableDefault) {
        return new AttributeConfigElement(nullableDefault, null, null);
    }

    public static AttributeConfigElement create(final String nullableDefault, final Object value, final EditStrategyType editStrategyType) {
        return new AttributeConfigElement(nullableDefault, value, editStrategyType);
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
