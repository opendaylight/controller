/*
  * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
  *
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  */
package org.opendaylight.controller.model.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.model.api.type.DecimalTypeDefinition;
import org.opendaylight.controller.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;

public class Decimal64 implements DecimalTypeDefinition {

    private final QName name = BaseTypes.constructQName("decimal64");
    private final SchemaPath path;
    private String units = "";
    private BigDecimal defaultValue = null;

    private final String description = "The decimal64 type represents a subset of the real numbers, which can "
            + "be represented by decimal numerals.  The value space of decimal64 is the set of numbers that can "
            + "be obtained by multiplying a 64-bit signed integer by a negative power of ten, i.e., expressible as "
            + "'i x 10^-n' where i is an integer64 and n is an integer between 1 and 18, inclusively.";

    private final String reference = "https://tools.ietf.org/html/rfc6020#section-9.3";

    private final List<RangeConstraint> rangeStatements;
    private final Integer fractionDigits;

    public Decimal64(final Integer fractionDigits) {
        super();
        this.fractionDigits = fractionDigits;
        rangeStatements = new ArrayList<RangeConstraint>();
        this.path = BaseTypes.schemaPath(name);
    }

    public Decimal64(final List<RangeConstraint> rangeStatements,
            Integer fractionDigits) {
        super();
        this.rangeStatements = rangeStatements;
        this.fractionDigits = fractionDigits;
        this.path = BaseTypes.schemaPath(name);
    }

    public Decimal64(final String units, final BigDecimal defaultValue,
            final List<RangeConstraint> rangeStatements,
            final Integer fractionDigits) {
        super();
        this.units = units;
        this.defaultValue = defaultValue;
        this.rangeStatements = rangeStatements;
        this.fractionDigits = fractionDigits;
        this.path = BaseTypes.schemaPath(name);
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
    public List<ExtensionDefinition> getExtensionSchemaNodes() {
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
    public String toString() {
        return Decimal64.class.getSimpleName() + "[qname=" + name
                + ", fractionDigits=" + fractionDigits + "]";
    }
}
