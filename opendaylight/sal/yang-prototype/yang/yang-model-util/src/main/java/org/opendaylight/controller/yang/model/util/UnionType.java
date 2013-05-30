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
import org.opendaylight.controller.yang.model.api.type.UnionTypeDefinition;

public final class UnionType implements UnionTypeDefinition {

    private final QName name = BaseTypes.constructQName("union");
    private final SchemaPath path;
    private final String description = "The union built-in type represents a value that corresponds to one of its member types.";
    private final String reference = "https://tools.ietf.org/html/rfc6020#section-9.12";
    private final UnionTypeDefinition baseType;
    private final List<TypeDefinition<?>> types;

    private UnionType(List<TypeDefinition<?>> types) {
        if (types == null) {
            throw new NullPointerException(
                    "When the type is 'union', the 'type' statement MUST be present.");
        }
        path = BaseTypes.schemaPath(name);
        this.types = types;
        this.baseType = this;
    }

    public UnionType(final SchemaPath path, List<TypeDefinition<?>> types) {
        if (types == null) {
            throw new NullPointerException(
                    "When the type is 'union', the 'type' statement MUST be present.");
        }
        this.path = path;
        this.types = types;
        this.baseType = new UnionType(types);
    }

    @Override
    public UnionTypeDefinition getBaseType() {
        return baseType;
    }

    @Override
    public String getUnits() {
        return null;
    }

    @Override
    public Object getDefaultValue() {
        return null;
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
    public List<TypeDefinition<?>> getTypes() {
        return types;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((types == null) ? 0 : types.hashCode());
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
        UnionType other = (UnionType) obj;
        if (types == null) {
            if (other.types != null) {
                return false;
            }
        } else if (!types.equals(other.types)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UnionType [name=");
        builder.append(name);
        builder.append(", types=[");
        for (TypeDefinition<?> td : types) {
            builder.append(", " + td.getQName().getLocalName());
        }
        builder.append("]");
        builder.append("]");
        return builder.toString();
    }

}
