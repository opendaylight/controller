/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.IdentitySchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.parser.builder.api.SchemaNodeBuilder;

public final class IdentitySchemaNodeBuilder implements SchemaNodeBuilder {
    private final IdentitySchemaNodeImpl instance;
    private final int line;
    private final QName qname;
    private SchemaPath schemaPath;
    private IdentitySchemaNodeBuilder baseIdentity;
    private String baseIdentityName;
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    IdentitySchemaNodeBuilder(final QName qname, final int line) {
        this.qname = qname;
        this.line = line;
        instance = new IdentitySchemaNodeImpl(qname);
    }

    @Override
    public IdentitySchemaNode build() {
        instance.setPath(schemaPath);
        if (baseIdentity != null) {
            instance.setBaseIdentity(baseIdentity.build());
        }

        // UNKNOWN NODES
        final List<UnknownSchemaNode> unknownNodes = new ArrayList<UnknownSchemaNode>();
        for (UnknownSchemaNodeBuilder b : addedUnknownNodes) {
            unknownNodes.add(b.build());
        }
        instance.setUnknownSchemaNodes(unknownNodes);

        return instance;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public QName getQName() {
        return qname;
    }

    @Override
    public SchemaPath getPath() {
        return schemaPath;
    }

    @Override
    public void setPath(SchemaPath schemaPath) {
        this.schemaPath = schemaPath;
    }

    @Override
    public void setDescription(final String description) {
        instance.setDescription(description);
    }

    @Override
    public void setReference(final String reference) {
        instance.setReference(reference);
    }

    @Override
    public void setStatus(final Status status) {
        if (status != null) {
            instance.setStatus(status);
        }
    }

    @Override
    public void addUnknownSchemaNode(final UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
    }

    public String getBaseIdentityName() {
        return baseIdentityName;
    }

    public void setBaseIdentityName(final String baseIdentityName) {
        this.baseIdentityName = baseIdentityName;
    }

    public void setBaseIdentity(final IdentitySchemaNodeBuilder baseType) {
        this.baseIdentity = baseType;
    }

    private final class IdentitySchemaNodeImpl implements IdentitySchemaNode {
        private final QName qname;
        private IdentitySchemaNode baseIdentity;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private SchemaPath path;
        private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();

        private IdentitySchemaNodeImpl(final QName qname) {
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

        private void setBaseIdentity(final IdentitySchemaNode baseIdentity) {
            this.baseIdentity = baseIdentity;
        }

        @Override
        public String getDescription() {
            return description;
        }

        private void setDescription(final String description) {
            this.description = description;
        }

        @Override
        public String getReference() {
            return reference;
        }

        private void setReference(final String reference) {
            this.reference = reference;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        private void setStatus(final Status status) {
            if (status != null) {
                this.status = status;
            }
        }

        @Override
        public SchemaPath getPath() {
            return path;
        }

        private void setPath(final SchemaPath path) {
            this.path = path;
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return unknownNodes;
        }

        private void setUnknownSchemaNodes(
                List<UnknownSchemaNode> unknownSchemaNodes) {
            if (unknownSchemaNodes != null) {
                this.unknownNodes = unknownSchemaNodes;
            }
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
            sb.append("]");
            return sb.toString();
        }
    }

}
