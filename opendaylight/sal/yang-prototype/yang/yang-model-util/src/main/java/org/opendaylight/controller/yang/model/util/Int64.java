/*
  * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
  *
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  */
package org.opendaylight.controller.yang.model.util;

import java.net.URI;
import java.util.Date;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.type.IntegerTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;

/**
 * Implementation of Yang int64 built-in type. <br>
 * int64  represents integer values between -9223372036854775808 and 9223372036854775807, inclusively.
 * The Java counterpart of Yang int64 built-in type is
 * {@link Long}.
 *
 */
public class Int64 extends AbstractSignedInteger {

    private static final QName name = BaseTypes.constructQName("int64");
    private Long defaultValue = null;
    private static final String description =
            "int64  represents integer values between -9223372036854775808 and 9223372036854775807, inclusively.";
    private final IntegerTypeDefinition baseType;

    private Int64() {
        super(name, description, Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        this.baseType = this;
    }

    public Int64(final List<String> actualPath, final URI namespace,
            final Date revision) {
        super(actualPath, namespace, revision, name, description, Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        this.baseType = new Int64();
    }

    public Int64(final List<String> actualPath, final URI namespace,
            final Date revision, final Long defaultValue) {
        super(actualPath, namespace, revision, name, description, Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        this.baseType = new Int64();
        this.defaultValue = defaultValue;
    }

    public Int64(final List<String> actualPath, final URI namespace,
            final Date revision, final List<RangeConstraint> rangeStatements,
            final String units, final Long defaultValue) {
        super(actualPath, namespace, revision, name, description, rangeStatements, units);
        this.baseType = new Int64();
        this.defaultValue = defaultValue;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.controller.yang.model.api.TypeDefinition#getBaseType()
     */
    @Override
    public IntegerTypeDefinition getBaseType() {
        return baseType;
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
        Int64 other = (Int64) obj;
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
        builder.append("Int64 [defaultValue=");
        builder.append(defaultValue);
        builder.append(", AbstractInteger=");
        builder.append(super.toString());
        builder.append("]");
        return builder.toString();
    }
}
