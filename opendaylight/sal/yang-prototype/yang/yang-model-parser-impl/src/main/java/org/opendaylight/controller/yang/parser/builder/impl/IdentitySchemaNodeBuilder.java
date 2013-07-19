/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.IdentitySchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.parser.builder.api.AbstractSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.util.Comparators;

public final class IdentitySchemaNodeBuilder extends AbstractSchemaNodeBuilder {
    private boolean isBuilt;
    private final IdentitySchemaNodeImpl instance;
    private IdentitySchemaNodeBuilder baseIdentityBuilder;
    private IdentitySchemaNode baseIdentity;
    private String baseIdentityName;

    IdentitySchemaNodeBuilder(final String moduleName, final int line, final QName qname) {
        super(moduleName, line, qname);
        instance = new IdentitySchemaNodeImpl(qname);
    }

    @Override
    public IdentitySchemaNode build() {
        if (!isBuilt) {
            instance.setPath(schemaPath);
            instance.setDescription(description);
            instance.setReference(reference);
            instance.setStatus(status);

            if (baseIdentity == null) {
                if (baseIdentityBuilder != null) {
                    instance.setBaseIdentity(baseIdentityBuilder.build());
                }
            } else {
                instance.setBaseIdentity(baseIdentity);
            }

            // UNKNOWN NODES
            if (unknownNodes == null) {
                unknownNodes = new ArrayList<UnknownSchemaNode>();
                for (UnknownSchemaNodeBuilder b : addedUnknownNodes) {
                    unknownNodes.add(b.build());
                }
                Collections.sort(unknownNodes, Comparators.SCHEMA_NODE_COMP);
            }
            instance.setUnknownSchemaNodes(unknownNodes);

            isBuilt = true;
        }

        return instance;
    }

    public String getBaseIdentityName() {
        return baseIdentityName;
    }

    public void setBaseIdentityName(final String baseIdentityName) {
        this.baseIdentityName = baseIdentityName;
    }

    public void setBaseIdentity(final IdentitySchemaNodeBuilder baseType) {
        this.baseIdentityBuilder = baseType;
    }

    public void setBaseIdentity(final IdentitySchemaNode baseType) {
        this.baseIdentity = baseType;
    }

    @Override
    public String toString() {
        return "identity " + qname.getLocalName();
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

        private void setUnknownSchemaNodes(List<UnknownSchemaNode> unknownSchemaNodes) {
            if (unknownSchemaNodes != null) {
                this.unknownNodes = unknownSchemaNodes;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((qname == null) ? 0 : qname.hashCode());
            result = prime * result + ((path == null) ? 0 : path.hashCode());
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
            if (path == null) {
                if (other.path != null) {
                    return false;
                }
            } else if (!path.equals(other.path)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(IdentitySchemaNodeImpl.class.getSimpleName());
            sb.append("[");
            sb.append("base=" + baseIdentity);
            sb.append(", qname=" + qname);
            sb.append("]");
            return sb.toString();
        }
    }

}
