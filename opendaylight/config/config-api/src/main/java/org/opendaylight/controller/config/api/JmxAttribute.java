/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

/**
 * Wrapper around strings to make {@link JmxAttributeValidationException} type
 * safe.
 */
public class JmxAttribute {
    private final String attributeName;

    public JmxAttribute(String attributeName) {
        if (attributeName == null)
            throw new NullPointerException("Parameter 'attributeName' is null");
        this.attributeName = attributeName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        JmxAttribute that = (JmxAttribute) o;

        if (attributeName != null ? !attributeName.equals(that.attributeName)
                : that.attributeName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return attributeName != null ? attributeName.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "JmxAttribute{'" + attributeName + "'}";
    }
}
