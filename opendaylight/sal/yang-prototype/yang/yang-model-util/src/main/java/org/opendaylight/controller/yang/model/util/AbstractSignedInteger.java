/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.IntegerTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;

/**
 * The Abstract Integer class defines implementation of IntegerTypeDefinition
 * interface which represents SIGNED Integer values defined in Yang language. <br>
 * The integer built-in types in Yang are int8, int16, int32, int64. They
 * represent signed integers of different sizes:
 * 
 * <ul>
 * <li>int8 - represents integer values between -128 and 127, inclusively.</li>
 * <li>int16 - represents integer values between -32768 and 32767, inclusively.</li>
 * <li>int32 - represents integer values between -2147483648 and 2147483647,
 * inclusively.</li>
 * <li>int64 - represents integer values between -9223372036854775808 and
 * 9223372036854775807, inclusively.</li>
 * </ul>
 * 
 */
public abstract class AbstractSignedInteger implements IntegerTypeDefinition {

    private final QName name;
    private final SchemaPath path;
    private final String description;
    private final String reference = "https://tools.ietf.org/html/rfc6020#section-9.2";

    private final String units;
    private final List<RangeConstraint> rangeStatements;

    /**
     * @param name
     * @param description
     * @param minRange
     * @param maxRange
     * @param units
     */
    public AbstractSignedInteger(final QName name, final String description,
            final Number minRange, final Number maxRange, final String units) {
        this.name = name;
        this.description = description;
        this.path = BaseTypes.schemaPath(name);
        this.units = units;
        this.rangeStatements = new ArrayList<RangeConstraint>();
        final String rangeDescription = "Integer values between " + minRange
                + " and " + maxRange + ", inclusively.";
        this.rangeStatements.add(BaseConstraints.rangeConstraint(minRange,
                maxRange, rangeDescription, "https://tools.ietf.org/html/rfc6020#section-9.2.4"));
    }

    /**
     * @param name
     * @param description
     * @param rangeStatements
     * @param units
     */
    public AbstractSignedInteger(final QName name, final String description,
            final List<RangeConstraint> rangeStatements, final String units) {
        this.name = name;
        this.description = description;
        this.path = BaseTypes.schemaPath(name);
        this.units = units;
        this.rangeStatements = rangeStatements;
    }

    @Override
    public String getUnits() {
        return units;
    }

    @Override
    public QName getQName() {
        return name;
    }

    @Override
    public SchemaPath getPath() {
        return path;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public Status getStatus() {
        return Status.CURRENT;
    }

    @Override
    public List<RangeConstraint> getRangeStatements() {
        return rangeStatements;
    }

    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return Collections.emptyList();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result
                + ((rangeStatements == null) ? 0 : rangeStatements.hashCode());
        result = prime * result
                + ((reference == null) ? 0 : reference.hashCode());
        result = prime * result + ((units == null) ? 0 : units.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractSignedInteger other = (AbstractSignedInteger) obj;
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        if (rangeStatements == null) {
            if (other.rangeStatements != null) {
                return false;
            }
        } else if (!rangeStatements.equals(other.rangeStatements)) {
            return false;
        }
        if (reference == null) {
            if (other.reference != null) {
                return false;
            }
        } else if (!reference.equals(other.reference)) {
            return false;
        }
        if (units == null) {
            if (other.units != null) {
                return false;
            }
        } else if (!units.equals(other.units)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AbstractInteger [name=");
        builder.append(name);
        builder.append(", path=");
        builder.append(path);
        builder.append(", description=");
        builder.append(description);
        builder.append(", reference=");
        builder.append(reference);
        builder.append(", units=");
        builder.append(units);
        builder.append(", rangeStatements=");
        builder.append(rangeStatements);
        builder.append("]");
        return builder.toString();
    }
}
