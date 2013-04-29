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
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.StringTypeDefinition;

/**
 * The <code>default</code> implementation of String Type Definition interface.
 *
 * @see StringTypeDefinition
 */
public class StringType implements StringTypeDefinition {

    private final QName name = BaseTypes.constructQName("string");;
    private final SchemaPath path;
    private String defaultValue = "";
    private final String description = "";
    private final String reference = "";
    private final List<LengthConstraint> lengthStatements;
    private final List<PatternConstraint> patterns;
    private String units = "";

    /**
     * Default Constructor.
     */
    public StringType() {
        super();
        path = BaseTypes.schemaPath(name);
        final List<LengthConstraint> constraints = new ArrayList<LengthConstraint>();
        constraints.add(BaseConstraints.lengthConstraint(0, Long.MAX_VALUE, "", ""));
        lengthStatements = Collections.unmodifiableList(constraints);

        this.patterns = Collections.emptyList();
    }

    /**
     *
     *
     * @param lengthStatements
     * @param patterns
     */
    public StringType(final List<LengthConstraint> lengthStatements,
            final List<PatternConstraint> patterns) {
        super();
        path = BaseTypes.schemaPath(name);
        if(lengthStatements == null || lengthStatements.size() == 0) {
            final List<LengthConstraint> constraints = new ArrayList<LengthConstraint>();
            constraints.add(BaseConstraints.lengthConstraint(0, Long.MAX_VALUE, "", ""));
            this.lengthStatements = Collections.unmodifiableList(constraints);
        } else {
            this.lengthStatements = Collections.unmodifiableList(lengthStatements);
        }
        this.patterns = Collections.unmodifiableList(patterns);
    }

    /**
     *
     *
     * @param defaultValue
     * @param lengthStatements
     * @param patterns
     * @param units
     */
    public StringType(final String defaultValue,
            final List<LengthConstraint> lengthStatements,
            final List<PatternConstraint> patterns, final String units) {
        super();
        path = BaseTypes.schemaPath(name);
        this.defaultValue = defaultValue;
        if(lengthStatements == null || lengthStatements.size() == 0) {
            final List<LengthConstraint> constraints = new ArrayList<LengthConstraint>();
            constraints.add(BaseConstraints.lengthConstraint(0, Long.MAX_VALUE, "", ""));
            this.lengthStatements = Collections.unmodifiableList(constraints);
        } else {
            this.lengthStatements = Collections.unmodifiableList(lengthStatements);
        }
        this.patterns = patterns;
        this.units = units;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.controller.yang.model.api.TypeDefinition#getBaseType()
     */
    @Override
    public StringTypeDefinition getBaseType() {
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
     * @see org.opendaylight.controller.yang.model.api.TypeDefinition#getDefaultValue()
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
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getDescription()
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
        return Status.CURRENT;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.csico.yang.model.base.type.api.StringTypeDefinition#getLengthStatements
     * ()
     */
    @Override
    public List<LengthConstraint> getLengthStatements() {
        return lengthStatements;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.csico.yang.model.base.type.api.StringTypeDefinition#getPatterns()
     */
    @Override
    public List<PatternConstraint> getPatterns() {
        return patterns;
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
                + ((defaultValue == null) ? 0 : defaultValue.hashCode());
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        result = prime
                * result
                + ((lengthStatements == null) ? 0 : lengthStatements.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result
                + ((patterns == null) ? 0 : patterns.hashCode());
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
        StringType other = (StringType) obj;
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
        } else if ((path != null) && (other.path != null)) {
            if (!path.getPath().equals(other.path.getPath())) {
                return false;
            }
        }
        if (patterns == null) {
            if (other.patterns != null) {
                return false;
            }
        } else if (!patterns.equals(other.patterns)) {
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
        builder.append("StringType [name=");
        builder.append(name);
        builder.append(", path=");
        builder.append(path);
        builder.append(", defaultValue=");
        builder.append(defaultValue);
        builder.append(", description=");
        builder.append(description);
        builder.append(", reference=");
        builder.append(reference);
        builder.append(", lengthStatements=");
        builder.append(lengthStatements);
        builder.append(", patterns=");
        builder.append(patterns);
        builder.append(", units=");
        builder.append(units);
        builder.append("]");
        return builder.toString();
    }
}
