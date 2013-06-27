/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;

public class ExtendedType implements TypeDefinition<TypeDefinition<?>> {

    private final QName typeName;
    private final TypeDefinition<?> baseType;
    private final SchemaPath path;
    private final String description;
    private final String reference;
    private final List<UnknownSchemaNode> unknownSchemaNodes;

    private List<RangeConstraint> ranges = Collections.emptyList();
    private List<LengthConstraint> lengths = Collections.emptyList();
    private List<PatternConstraint> patterns = Collections.emptyList();
    private Integer fractionDigits = null;

    private Status status;
    private String units;
    private Object defaultValue;
    private boolean addedByUses;

    public static class Builder {
        private final QName typeName;
        private final TypeDefinition<?> baseType;

        private final SchemaPath path;
        private final String description;
        private final String reference;

        private List<UnknownSchemaNode> unknownSchemaNodes = Collections
                .emptyList();
        private Status status = Status.CURRENT;
        private String units = "";
        private Object defaultValue = null;
        private boolean addedByUses;

        private List<RangeConstraint> ranges = Collections.emptyList();
        private List<LengthConstraint> lengths = Collections.emptyList();
        private List<PatternConstraint> patterns = Collections.emptyList();
        private Integer fractionDigits = null;

        public Builder(final List<String> actualPath, final URI namespace,
                final Date revision, final QName typeName,
                TypeDefinition<?> baseType, final String description,
                final String reference) {
            this.typeName = typeName;
            this.baseType = baseType;
            this.path = BaseTypes.schemaPath(actualPath, namespace, revision);
            this.description = description;
            this.reference = reference;
        }

        public Builder(final QName typeName, TypeDefinition<?> baseType,
                final String description, final String reference,
                SchemaPath path) {
            this.typeName = typeName;
            this.baseType = baseType;
            this.path = path;
            this.description = description;
            this.reference = reference;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder units(String units) {
            this.units = units;
            return this;
        }

        public Builder defaultValue(final Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder addedByUses(final boolean addedByUses) {
            this.addedByUses = addedByUses;
            return this;
        }

        public Builder unknownSchemaNodes(
                final List<UnknownSchemaNode> unknownSchemaNodes) {
            this.unknownSchemaNodes = unknownSchemaNodes;
            return this;
        }

        public Builder ranges(final List<RangeConstraint> ranges) {
            if (ranges != null) {
                this.ranges = ranges;
            }
            return this;
        }

        public Builder lengths(final List<LengthConstraint> lengths) {
            if (lengths != null) {
                this.lengths = lengths;
            }
            return this;
        }

        public Builder patterns(final List<PatternConstraint> patterns) {
            if (patterns != null) {
                this.patterns = patterns;
            }
            return this;
        }

        public Builder fractionDigits(final Integer fractionDigits) {
            this.fractionDigits = fractionDigits;
            return this;
        }

        public ExtendedType build() {
            return new ExtendedType(this);
        }
    }

    private ExtendedType(Builder builder) {
        this.typeName = builder.typeName;
        this.baseType = builder.baseType;
        this.path = builder.path;
        this.description = builder.description;
        this.reference = builder.reference;
        this.unknownSchemaNodes = builder.unknownSchemaNodes;
        this.status = builder.status;
        this.units = builder.units;
        this.defaultValue = builder.defaultValue;
        this.addedByUses = builder.addedByUses;

        this.ranges = builder.ranges;
        this.lengths = builder.lengths;
        this.patterns = builder.patterns;
        this.fractionDigits = builder.fractionDigits;
    }

    @Override
    public TypeDefinition<?> getBaseType() {
        return baseType;
    }

    @Override
    public String getUnits() {
        return units;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean isAddedByUses() {
        return addedByUses;
    }

    @Override
    public QName getQName() {
        return typeName;
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
        return status;
    }

    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return unknownSchemaNodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExtendedType)) {
            return false;
        }

        ExtendedType that = (ExtendedType) o;
        if (path != null ? !path.equals(that.path) : that.path != null) {
            return false;
        }
        if (typeName != null ? !typeName.equals(that.typeName) : that.typeName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = typeName != null ? typeName.hashCode() : 0;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExtendedType [typeName=");
        builder.append(typeName);
        builder.append(", baseType=");
        builder.append(baseType);
        builder.append(", path=");
        builder.append(path);
        builder.append(", description=");
        builder.append(description);
        builder.append(", reference=");
        builder.append(reference);
        builder.append(", unknownSchemaNodes=");
        builder.append(unknownSchemaNodes);
        builder.append(", status=");
        builder.append(status);
        builder.append(", units=");
        builder.append(units);
        builder.append(", defaultValue=");
        builder.append(defaultValue);
        builder.append("]");
        return builder.toString();
    }

    public List<RangeConstraint> getRanges() {
        return ranges;
    }

    public List<LengthConstraint> getLengths() {
        return lengths;
    }

    public List<PatternConstraint> getPatterns() {
        return patterns;
    }

    public Integer getFractionDigits() {
        return fractionDigits;
    }
}
