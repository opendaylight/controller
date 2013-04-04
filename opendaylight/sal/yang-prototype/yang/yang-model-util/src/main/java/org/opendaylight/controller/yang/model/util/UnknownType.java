/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.api.type.UnknownTypeDefinition;

public class UnknownType implements UnknownTypeDefinition {

    private final QName name;
    private final SchemaPath path;
    private final String description;
    private final String reference;

    private final List<LengthConstraint> lengthStatements;
    private final List<PatternConstraint> patterns;
    private final List<RangeConstraint> rangeStatements;
    private final List<UnknownSchemaNode> extensions;
    private final LengthConstraint lengthConstraint;
    private final Integer fractionDigits;

    private final Status status;
    private final String units;
    private final Object defaultValue;

    public static class Builder {

        private final QName name;
        private final SchemaPath path;
        private String description;
        private String reference;

        private List<LengthConstraint> lengthStatements = Collections
                .emptyList();
        private List<PatternConstraint> patterns = Collections.emptyList();
        private List<RangeConstraint> rangeStatements = Collections.emptyList();
        private List<UnknownSchemaNode> extensions = Collections.emptyList();
        private LengthConstraint lengthConstraint = null;
        private Integer fractionDigits = null;

        private Status status = Status.CURRENT;
        private String units = "";
        private Object defaultValue = null;

        public Builder(final QName name, final String description,
                final String reference) {
            this.name = name;
            this.path = BaseTypes.schemaPath(name);
            this.description = description;
            this.reference = reference;
        }

        public Builder(final QName name) {
            this.name = name;
            this.path = BaseTypes.schemaPath(name);
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder lengthStatements(
                final List<LengthConstraint> lengthStatements) {
            this.lengthStatements = lengthStatements;
            return this;
        }

        public Builder patterns(final List<PatternConstraint> patterns) {
            this.patterns = patterns;
            return this;
        }

        public Builder rangeStatements(
                final List<RangeConstraint> rangeStatements) {
            this.rangeStatements = rangeStatements;
            return this;
        }

        public Builder extensions(final List<UnknownSchemaNode> extensions) {
            this.extensions = extensions;
            return this;
        }

        public Builder lengthConstraint(final LengthConstraint lengthConstraint) {
            this.lengthConstraint = lengthConstraint;
            return this;
        }

        public Builder fractionDigits(final Integer fractionDigits) {
            this.fractionDigits = fractionDigits;
            return this;
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

        public UnknownTypeDefinition build() {
            return new UnknownType(this);
        }
    }

    private UnknownType(Builder builder) {
        this.name = builder.name;
        this.path = builder.path;
        this.description = builder.description;
        this.reference = builder.reference;
        this.lengthStatements = builder.lengthStatements;
        this.patterns = builder.patterns;
        this.rangeStatements = builder.rangeStatements;
        this.extensions = builder.extensions;
        this.lengthConstraint = builder.lengthConstraint;
        this.status = builder.status;
        this.units = builder.units;
        this.defaultValue = builder.defaultValue;
        this.fractionDigits = builder.fractionDigits;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.controller.yang.model.api.TypeDefinition#getBaseType()
     */
    @Override
    public UnknownTypeDefinition getBaseType() {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.controller.yang.model.api.TypeDefinition#getUnits()
     */
    @Override
    public String getUnits() {
        return units;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.controller.yang.model.api.TypeDefinition#getDefaultValue
     * ()
     */
    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getQName()
     */
    @Override
    public QName getQName() {
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getPath()
     */
    @Override
    public SchemaPath getPath() {
        return path;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.controller.yang.model.api.SchemaNode#getDescription()
     */
    @Override
    public String getDescription() {
        return description;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getReference()
     */
    @Override
    public String getReference() {
        return reference;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getStatus()
     */
    @Override
    public Status getStatus() {
        return status;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.controller.yang.model.api.SchemaNode#getExtensionSchemaNodes
     * ()
     */
    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return extensions;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.controller.yang.model.api.type.UnknownTypeDefinition
     * #getRangeStatements()
     */
    @Override
    public List<RangeConstraint> getRangeStatements() {
        return rangeStatements;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.controller.yang.model.api.type.UnknownTypeDefinition
     * #getLengthStatements()
     */
    @Override
    public List<LengthConstraint> getLengthStatements() {
        return lengthStatements;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.controller.yang.model.api.type.UnknownTypeDefinition
     * #getPatterns()
     */
    @Override
    public List<PatternConstraint> getPatterns() {
        return patterns;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.controller.yang.model.api.type.UnknownTypeDefinition
     * #getLengthConstraint()
     */
    @Override
    public LengthConstraint getLengthConstraint() {
        return lengthConstraint;
    }

    @Override
    public Integer getFractionDigits() {
        return fractionDigits;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((defaultValue == null) ? 0 : defaultValue.hashCode());
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        result = prime * result
                + ((extensions == null) ? 0 : extensions.hashCode());
        result = prime
                * result
                + ((lengthConstraint == null) ? 0 : lengthConstraint.hashCode());
        result = prime
                * result
                + ((lengthStatements == null) ? 0 : lengthStatements.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result
                + ((patterns == null) ? 0 : patterns.hashCode());
        result = prime * result
                + ((rangeStatements == null) ? 0 : rangeStatements.hashCode());
        result = prime * result
                + ((reference == null) ? 0 : reference.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
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
        UnknownType other = (UnknownType) obj;
        if (defaultValue == null) {
            if (other.defaultValue != null) {
                return false;
            }
        } else if (!defaultValue.equals(other.defaultValue)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (extensions == null) {
            if (other.extensions != null) {
                return false;
            }
        } else if (!extensions.equals(other.extensions)) {
            return false;
        }
        if (lengthConstraint == null) {
            if (other.lengthConstraint != null) {
                return false;
            }
        } else if (!lengthConstraint.equals(other.lengthConstraint)) {
            return false;
        }
        if (lengthStatements == null) {
            if (other.lengthStatements != null) {
                return false;
            }
        } else if (!lengthStatements.equals(other.lengthStatements)) {
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
        if (patterns == null) {
            if (other.patterns != null) {
                return false;
            }
        } else if (!patterns.equals(other.patterns)) {
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
        if (status != other.status) {
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
        StringBuilder builder2 = new StringBuilder();
        builder2.append("UnknownType [name=");
        builder2.append(name);
        builder2.append(", path=");
        builder2.append(path);
        builder2.append(", description=");
        builder2.append(description);
        builder2.append(", reference=");
        builder2.append(reference);
        builder2.append(", lengthStatements=");
        builder2.append(lengthStatements);
        builder2.append(", patterns=");
        builder2.append(patterns);
        builder2.append(", rangeStatements=");
        builder2.append(rangeStatements);
        builder2.append(", extensions=");
        builder2.append(extensions);
        builder2.append(", lengthConstraint=");
        builder2.append(lengthConstraint);
        builder2.append(", status=");
        builder2.append(status);
        builder2.append(", units=");
        builder2.append(units);
        builder2.append(", defaultValue=");
        builder2.append(defaultValue);
        builder2.append("]");
        return builder2.toString();
    }

}
