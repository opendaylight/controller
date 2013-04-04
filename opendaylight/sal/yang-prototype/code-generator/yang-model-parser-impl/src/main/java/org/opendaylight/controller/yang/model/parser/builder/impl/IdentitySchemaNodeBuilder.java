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
import org.opendaylight.controller.yang.model.api.IdentitySchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.parser.builder.api.SchemaNodeBuilder;

public class IdentitySchemaNodeBuilder implements SchemaNodeBuilder {

    private final QName qname;
    private final IdentitySchemaNodeImpl instance;
    private IdentitySchemaNodeBuilder baseIdentity;
    private String baseIdentityName;

    IdentitySchemaNodeBuilder(QName qname) {
        this.qname = qname;
        instance = new IdentitySchemaNodeImpl(qname);
    }

    @Override
    public IdentitySchemaNode build() {
        if(baseIdentity != null) {
            IdentitySchemaNode base = baseIdentity.build();
            instance.setBaseIdentity(base);
        }
        return instance;
    }

    @Override
    public QName getQName() {
        return qname;
    }

    @Override
    public void setPath(SchemaPath path) {
        instance.setPath(path);
    }

    @Override
    public void setDescription(String description) {
        instance.setDescription(description);
    }

    @Override
    public void setReference(String reference) {
        instance.setReference(reference);
    }

    @Override
    public void setStatus(Status status) {
        if (status != null) {
            instance.setStatus(status);
        }
    }

    @Override
    public void addUnknownSchemaNode(
            UnknownSchemaNodeBuilder unknownSchemaNodeBuilder) {
        throw new IllegalStateException(
                "Can not add schema node to identity statement");
    }

    public String getBaseIdentityName() {
        return baseIdentityName;
    }

    public void setBaseIdentityName(String baseIdentityName) {
        this.baseIdentityName = baseIdentityName;
    }

    public void setBaseIdentity(IdentitySchemaNodeBuilder baseType) {
        this.baseIdentity = baseType;
    }

    private class IdentitySchemaNodeImpl implements IdentitySchemaNode {
        private final QName qname;
        private IdentitySchemaNode baseIdentity;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private SchemaPath path;

        private IdentitySchemaNodeImpl(QName qname) {
            this.qname = qname;
        }

        @Override
        public QName getQName() {
            return qname;
        }

        @Override
        public IdentitySchemaNode getBaseIdentity() {
            return baseIdentity;
        }

        private void setBaseIdentity(IdentitySchemaNode baseIdentity) {
            this.baseIdentity = baseIdentity;
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
            if (status != null) {
                this.status = status;
            }
        }

        @Override
        public SchemaPath getPath() {
            return path;
        }

        private void setPath(SchemaPath path) {
            this.path = path;
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return Collections.emptyList();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((qname == null) ? 0 : qname.hashCode());
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
            IdentitySchemaNodeImpl other = (IdentitySchemaNodeImpl) obj;
            if (qname == null) {
                if (other.qname != null) {
                    return false;
                }
            } else if (!qname.equals(other.qname)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(
                    IdentitySchemaNodeImpl.class.getSimpleName());
            sb.append("[");
            sb.append("base=" + baseIdentity);
            sb.append(", qname=" + qname);
            sb.append(", description=" + description);
            sb.append(", reference=" + reference);
            sb.append(", status=" + status);
            sb.append("]");
            return sb.toString();
        }
    }

}
