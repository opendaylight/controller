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
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;

public class ExtendedType implements TypeDefinition {

    private final QName typeName;
    private final TypeDefinition<?> baseType;
    private final SchemaPath path;
    private final String description;
    private final String reference;
    private final List<UnknownSchemaNode> unknownSchemaNodes;

    private Status status;
    private String units;
    private Object defaultValue;

    public static class Builder {
        private final QName typeName;
        private final TypeDefinition<?> baseType;

        private final SchemaPath path;
        private final String description;
        private final String reference;

        private List<UnknownSchemaNode> unknownSchemaNodes = Collections.emptyList();;
        private Status status = Status.CURRENT;
        private String units = "";
        private Object defaultValue = null;

        public Builder(final QName typeName, TypeDefinition<?> baseType,
                final String description, final String reference) {
            this.typeName = typeName;
            this.baseType = baseType;
            this.path = BaseTypes.schemaPath(typeName);
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

        public Builder unknownSchemaNodes(final List<UnknownSchemaNode> unknownSchemaNodes) {
            this.unknownSchemaNodes = unknownSchemaNodes;
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((baseType == null) ? 0 : baseType.hashCode());
        result = prime * result
                + ((defaultValue == null) ? 0 : defaultValue.hashCode());
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        result = prime * result
                + ((unknownSchemaNodes == null) ? 0 : unknownSchemaNodes.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result
                + ((reference == null) ? 0 : reference.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result
                + ((typeName == null) ? 0 : typeName.hashCode());
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
        ExtendedType other = (ExtendedType) obj;
        if (baseType == null) {
            if (other.baseType != null) {
                return false;
            }
        } else if (!baseType.equals(other.baseType)) {
            return false;
        }
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
        if (unknownSchemaNodes == null) {
            if (other.unknownSchemaNodes != null) {
                return false;
            }
        } else if (!unknownSchemaNodes.equals(other.unknownSchemaNodes)) {
            return false;
        }
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
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
        if (typeName == null) {
            if (other.typeName != null) {
                return false;
            }
        } else if (!typeName.equals(other.typeName)) {
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
        builder2.append("ExtendedType [typeName=");
        builder2.append(typeName);
        builder2.append(", baseType=");
        builder2.append(baseType);
        builder2.append(", path=");
        builder2.append(path);
        builder2.append(", description=");
        builder2.append(description);
        builder2.append(", reference=");
        builder2.append(reference);
        builder2.append(", unknownSchemaNodes=");
        builder2.append(unknownSchemaNodes);
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
