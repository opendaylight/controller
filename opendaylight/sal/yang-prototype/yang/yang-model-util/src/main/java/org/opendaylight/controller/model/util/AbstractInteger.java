/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.util;

import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.model.api.type.IntegerTypeDefinition;
import org.opendaylight.controller.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;

public abstract class AbstractInteger implements IntegerTypeDefinition {

    private final QName name;
    private final SchemaPath path;

    private final String description;
    private final String reference;

    private String units = "";
    private final List<RangeConstraint> rangeStatements;

    public AbstractInteger(final QName name, final String description,
            final String reference) {
        super();
        this.name = name;
        this.description = description;
        this.reference = reference;
        this.path = BaseTypes.schemaPath(name);

        final List<? extends RangeConstraint> emptyContstraints = Collections
                .emptyList();
        this.rangeStatements = Collections.unmodifiableList(emptyContstraints);
    }

    public AbstractInteger(QName name, String description, String reference,
            List<RangeConstraint> rangeStatements) {
        super();
        this.name = name;
        this.description = description;
        this.reference = reference;
        this.rangeStatements = rangeStatements;
        this.path = BaseTypes.schemaPath(name);
    }

    public AbstractInteger(QName name, String description, String reference,
            String units) {
        super();
        this.name = name;
        this.description = description;
        this.reference = reference;
        this.units = units;
        this.path = BaseTypes.schemaPath(name);

        final List<? extends RangeConstraint> emptyContstraints = Collections
                .emptyList();
        this.rangeStatements = Collections.unmodifiableList(emptyContstraints);
    }

    public AbstractInteger(QName name, String description, String reference,
            String units, List<RangeConstraint> rangeStatements) {
        super();
        this.name = name;
        this.description = description;
        this.reference = reference;
        this.units = units;
        this.rangeStatements = rangeStatements;
        this.path = BaseTypes.schemaPath(name);
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
    public List<ExtensionDefinition> getExtensionSchemaNodes() {
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
        AbstractInteger other = (AbstractInteger) obj;
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
