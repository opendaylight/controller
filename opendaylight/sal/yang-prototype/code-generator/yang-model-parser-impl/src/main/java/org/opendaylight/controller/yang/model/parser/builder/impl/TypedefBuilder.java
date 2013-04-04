/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.parser.builder.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeAwareBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.util.UnknownType;
import org.opendaylight.controller.yang.model.util.YangTypesConverter;

public class TypedefBuilder implements TypeDefinitionBuilder,
        SchemaNodeBuilder, TypeAwareBuilder {

    private final QName qname;
    private SchemaPath schemaPath;
    private TypeDefinition<?> baseType;

    private String description;
    private String reference;
    private Status status;
    private String units;
    private Object defaultValue;

    TypedefBuilder(QName qname) {
        this.qname = qname;
    }

    @Override
    public TypeDefinition<? extends TypeDefinition<?>> build() {
        final TypeDefinition<?> type = YangTypesConverter
                .javaTypeForBaseYangType(qname);
        if (type != null) {
            return type;
        } else {
            if (baseType != null) {
                // typedef
                TypeDefinitionImpl instance = new TypeDefinitionImpl(qname);
                instance.setDescription(description);
                instance.setReference(reference);
                instance.setStatus(status);
                instance.setPath(schemaPath);
                instance.setBaseType(baseType);
                instance.setUnits(units);
                instance.setDefaultValue(defaultValue);
                return instance;
            } else {
                // type
                final UnknownType.Builder unknownBuilder = new UnknownType.Builder(
                        qname, description, reference);
                unknownBuilder.status(status);
                return unknownBuilder.build();
            }
        }
    }

    @Override
    public QName getQName() {
        return qname;
    }

    @Override
    public void setPath(final SchemaPath schemaPath) {
        this.schemaPath = schemaPath;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public void setReference(final String reference) {
        this.reference = reference;
    }

    @Override
    public void setStatus(final Status status) {
        if (status != null) {
            this.status = status;
        }
    }

    @Override
    public void setUnits(String units) {
        this.units = units;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public TypeDefinition<?> getType() {
        return baseType;
    }

    @Override
    public void setType(TypeDefinition<?> baseType) {
        this.baseType = baseType;
    }

    @Override
    public TypeDefinition<?> getBaseType() {
        return baseType;
    }

    @Override
    public void addUnknownSchemaNode(UnknownSchemaNodeBuilder unknownSchemaNodeBuilder) {
        // TODO
    }

    private static class TypeDefinitionImpl<T extends TypeDefinition<T>>
            implements TypeDefinition<T> {

        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private Object defaultValue;
        private T baseType;
        private String units;
        private List<UnknownSchemaNode> unknownSchemaNodes = Collections.emptyList();

        private TypeDefinitionImpl(QName qname) {
            this.qname = qname;
        }

        @Override
        public QName getQName() {
            return qname;
        }

        @Override
        public SchemaPath getPath() {
            return path;
        }

        private void setPath(SchemaPath path) {
            this.path = path;
        }

        @Override
        public String getDescription() {
            return description;
        }

        private void setDescription(String description) {
            this.description = description;
        }

        @Override
        public String getReference() {
            return reference;
        }

        private void setReference(String reference) {
            this.reference = reference;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        private void setStatus(Status status) {
            this.status = status;
        }

        @Override
        public T getBaseType() {
            return baseType;
        }

        private void setBaseType(T type) {
            this.baseType = type;
        }

        @Override
        public String getUnits() {
            return units;
        }

        private void setUnits(String units) {
            this.units = units;
        }

        @Override
        public Object getDefaultValue() {
            return defaultValue;
        }

        private void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return unknownSchemaNodes;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((qname == null) ? 0 : qname.hashCode());
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            result = prime * result
                    + ((description == null) ? 0 : description.hashCode());
            result = prime * result
                    + ((reference == null) ? 0 : reference.hashCode());
            result = prime * result
                    + ((status == null) ? 0 : status.hashCode());
            result = prime * result
                    + ((baseType == null) ? 0 : baseType.hashCode());
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
            TypeDefinitionImpl other = (TypeDefinitionImpl) obj;
            if (qname == null) {
                if (other.qname != null) {
                    return false;
                }
            } else if (!qname.equals(other.qname)) {
                return false;
            }
            if (path == null) {
                if (other.path != null) {
                    return false;
                }
            } else if (!path.equals(other.path)) {
                return false;
            }
            if (description == null) {
                if (other.description != null) {
                    return false;
                }
            } else if (!description.equals(other.description)) {
                return false;
            }
            if (reference == null) {
                if (other.reference != null) {
                    return false;
                }
            } else if (!reference.equals(other.reference)) {
                return false;
            }
            if (status == null) {
                if (other.status != null) {
                    return false;
                }
            } else if (!status.equals(other.status)) {
                return false;
            }
            if (baseType == null) {
                if (other.baseType != null) {
                    return false;
                }
            } else if (!baseType.equals(other.baseType)) {
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
            final StringBuilder sb = new StringBuilder(
                    TypeDefinitionImpl.class.getSimpleName());
            sb.append("[");
            sb.append("qname=" + qname);
            sb.append(", path=" + path);
            sb.append(", description=" + description);
            sb.append(", reference=" + reference);
            sb.append(", status=" + status);
            sb.append(", baseType=" + baseType + "]");
            return sb.toString();
        }
    }

}
