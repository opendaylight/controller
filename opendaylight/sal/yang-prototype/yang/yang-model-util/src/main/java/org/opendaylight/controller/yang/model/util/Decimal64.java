/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.DecimalTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;

/**
 * The <code>default</code> implementation of Decimal Type Definition interface.
 * 
 * 
 * @see DecimalTypeDefinition
 */
public class Decimal64 implements DecimalTypeDefinition {

    private final QName name = BaseTypes.constructQName("decimal64");
    private final SchemaPath path;
    private String units = "";
    private BigDecimal defaultValue = null;

    private final String description = "The decimal64 type represents a subset of the real numbers, which can "
            + "be represented by decimal numerals. The value space of decimal64 is the set of numbers that can "
            + "be obtained by multiplying a 64-bit signed integer by a negative power of ten, i.e., expressible as "
            + "'i x 10^-n' where i is an integer64 and n is an integer between 1 and 18, inclusively.";

    private final String reference = "https://tools.ietf.org/html/rfc6020#section-9.3";

    private final List<RangeConstraint> rangeStatements;
    private final Integer fractionDigits;

    /**
     * Default Decimal64 Type Constructor. <br>
     * <br>
     * The initial range statements are set to Decimal64
     * <code>min=-922337203685477580.8</code> and
     * <code>max=922337203685477580.7</code> <br>
     * The fractions digits MUST be defined as integer between 1 and 18
     * inclusively as defined interface {@link DecimalTypeDefinition} <br>
     * If the fraction digits are not defined inner the definition boundaries
     * the constructor will throw {@link IllegalArgumentException}
     * 
     * @param fractionDigits
     *            integer between 1 and 18 inclusively
     * 
     * @see DecimalTypeDefinition
     * @exception IllegalArgumentException
     */
    public Decimal64(final Integer fractionDigits) {
        super();
        if (!((fractionDigits.intValue() > 1) && (fractionDigits.intValue() <= 18))) {
            throw new IllegalArgumentException(
                    "The fraction digits outside of boundaries. Fraction digits MUST be integer between 1 and 18 inclusively");
        }
        this.fractionDigits = fractionDigits;
        rangeStatements = defaultRangeStatements();
        this.path = BaseTypes.schemaPath(name);
    }

    /**
     * Decimal64 Type Constructor. <br>
     * 
     * If parameter <code>Range Statements</code> is <code>null</code> or
     * defined as <code>empty List</code> the constructor automatically assigns
     * the boundaries as min and max value defined for Decimal64 in <a
     * href="https://tools.ietf.org/html/rfc6020#section-9.3">[RFC-6020] The
     * decimal64 Built-In Type</a> <br>
     * <br>
     * The fractions digits MUST be defined as integer between 1 and 18
     * inclusively as defined interface {@link DecimalTypeDefinition} <br>
     * If the fraction digits are not defined inner the definition boundaries
     * the constructor will throw {@link IllegalArgumentException}
     * 
     * @param rangeStatements
     *            Range Constraint Statements
     * @param fractionDigits
     *            integer between 1 and 18 inclusively
     * @exception IllegalArgumentException
     */
    public Decimal64(final List<RangeConstraint> rangeStatements,
            Integer fractionDigits) {
        super();
        if (!((fractionDigits.intValue() > 1) && (fractionDigits.intValue() <= 18))) {
            throw new IllegalArgumentException(
                    "The fraction digits outside of boundaries. Fraction digits MUST be integer between 1 and 18 inclusively");
        }
        if (rangeStatements == null || rangeStatements.isEmpty()) {
            this.rangeStatements = defaultRangeStatements();
        } else {
            this.rangeStatements = Collections.unmodifiableList(rangeStatements);
        }
        this.fractionDigits = fractionDigits;
        this.path = BaseTypes.schemaPath(name);
    }

    /**
     * Decimal64 Type Constructor. <br>
     * If parameter <code>Range Statements</code> is <code>null</code> or
     * defined as <code>empty List</code> the constructor automatically assigns
     * the boundaries as min and max value defined for Decimal64 in <a
     * href="https://tools.ietf.org/html/rfc6020#section-9.3">[RFC-6020] The
     * decimal64 Built-In Type</a> <br>
     * <br>
     * The fractions digits MUST be defined as integer between 1 and 18
     * inclusively as defined interface {@link DecimalTypeDefinition} <br>
     * If the fraction digits are not defined inner the definition boundaries
     * the constructor will throw {@link IllegalArgumentException}
     * 
     * @param units
     *            units associated with the type
     * @param defaultValue
     *            Default Value for type
     * @param rangeStatements
     *            Range Constraint Statements
     * @param fractionDigits
     *            integer between 1 and 18 inclusively
     * 
     * @exception IllegalArgumentException
     */
    public Decimal64(final String units, final BigDecimal defaultValue,
            final List<RangeConstraint> rangeStatements,
            final Integer fractionDigits) {
        super();
        if (!((fractionDigits.intValue() > 1) && (fractionDigits.intValue() <= 18))) {
            throw new IllegalArgumentException(
                    "The fraction digits outside of boundaries. Fraction digits MUST be integer between 1 and 18 inclusively");
        }

        if (rangeStatements == null || rangeStatements.isEmpty()) {
            this.rangeStatements = defaultRangeStatements();
            
        } else {
            this.rangeStatements = Collections.unmodifiableList(rangeStatements);
        }
        this.units = units;
        this.defaultValue = defaultValue;
        this.fractionDigits = fractionDigits;
        this.path = BaseTypes.schemaPath(name);
    }

    /**
     * Returns unmodifiable List with default definition of Range Statements.
     * 
     * @return unmodifiable List with default definition of Range Statements.
     */
    private List<RangeConstraint> defaultRangeStatements() {
        final List<RangeConstraint> rangeStatements = new ArrayList<RangeConstraint>();
        final BigDecimal min = new BigDecimal("-922337203685477580.8");
        final BigDecimal max = new BigDecimal("922337203685477580.7");
        final String rangeDescription = "Integer values between " + min
                + " and " + max + ", inclusively.";
        rangeStatements.add(BaseConstraints.rangeConstraint(min, max,
                rangeDescription,
                "https://tools.ietf.org/html/rfc6020#section-9.2.4"));
        return Collections.unmodifiableList(rangeStatements);
    }

    @Override
    public DecimalTypeDefinition getBaseType() {
        return this;
    }

    @Override
    public String getUnits() {
        return units;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
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
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return Collections.emptyList();
    }

    @Override
    public List<RangeConstraint> getRangeStatements() {
        return rangeStatements;
    }

    @Override
    public Integer getFractionDigits() {
        return fractionDigits;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
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
        Decimal64 other = (Decimal64) obj;
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
        return true;
    }

    @Override
    public String toString() {
        return Decimal64.class.getSimpleName() + "[qname=" + name
                + ", fractionDigits=" + fractionDigits + "]";
    }
}
