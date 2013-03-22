/*
  * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
  *
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  */
package org.opendaylight.controller.model.util;

import java.util.List;

import org.opendaylight.controller.model.api.type.IntegerTypeDefinition;
import org.opendaylight.controller.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.common.QName;

public class Uint16 extends AbstractInteger {

    private static final QName name = BaseTypes.constructQName("uint16");
    private Integer defaultValue = null;
    private static final String description = "";
    private static final String reference = "";

    public Uint16() {
        super(name, description, reference);
    }

    public Uint16(final Integer defaultValue) {
        super(name, description, reference);
        this.defaultValue = defaultValue;
    }

    public Uint16(final List<RangeConstraint> rangeStatements,
            final Integer defaultValue) {
        super(name, description, reference, rangeStatements);
        this.defaultValue = defaultValue;
    }

    public Uint16(final String units, final Integer defaultValue) {
        super(name, description, reference, units);
        this.defaultValue = defaultValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.TypeDefinition#getBaseType()
     */
    @Override
    public IntegerTypeDefinition getBaseType() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.TypeDefinition#getDefaultValue()
     */
    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((defaultValue == null) ? 0 : defaultValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Uint16 other = (Uint16) obj;
        if (defaultValue == null) {
            if (other.defaultValue != null) {
                return false;
            }
        } else if (!defaultValue.equals(other.defaultValue)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Uint16 [defaultValue=");
        builder.append(defaultValue);
        builder.append(", AbstractInteger=");
        builder.append(super.toString());
        builder.append("]");
        return builder.toString();
    }
}
