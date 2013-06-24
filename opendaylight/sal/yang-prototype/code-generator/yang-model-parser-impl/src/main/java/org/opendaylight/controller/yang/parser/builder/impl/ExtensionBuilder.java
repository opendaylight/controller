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
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.parser.builder.api.SchemaNodeBuilder;

public final class ExtensionBuilder implements SchemaNodeBuilder {
    private boolean isBuilt;
    private final ExtensionDefinitionImpl instance;
    private final int line;
    private final QName qname;
    private SchemaPath schemaPath;
    private String description;
    private String reference;
    private Status status = Status.CURRENT;
    private final List<UnknownSchemaNodeBuilder> addedExtensions = new ArrayList<UnknownSchemaNodeBuilder>();
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    ExtensionBuilder(final QName qname, final int line) {
        this.qname = qname;
        this.line = line;
        instance = new ExtensionDefinitionImpl(qname);
    }

    @Override
    public ExtensionDefinition build() {
        if(!isBuilt) {
            instance.setPath(schemaPath);
            instance.setDescription(description);
            instance.setReference(reference);
            instance.setStatus(status);

            // UNKNOWN NODES
            final List<UnknownSchemaNode> extensions = new ArrayList<UnknownSchemaNode>();
            for (UnknownSchemaNodeBuilder e : addedExtensions) {
                extensions.add(e.build());
            }
            instance.setUnknownSchemaNodes(extensions);

            isBuilt = true;
        }

        return instance;
    }

    @Override
    public int getLine() {
        return line;
    }

    public void addExtension(UnknownSchemaNodeBuilder extension) {
        addedExtensions.add(extension);
    }

    public void setYinElement(boolean yin) {
        instance.setYinElement(yin);
    }

    public void setArgument(String argument) {
        instance.setArgument(argument);
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
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public void setReference(final String reference) {
        this.reference = reference;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(final Status status) {
        instance.setStatus(status);
    }

    @Override
    public void addUnknownSchemaNode(final UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
    }

    private final class ExtensionDefinitionImpl implements ExtensionDefinition {
        private final QName qname;
        private String argument;
        private SchemaPath schemaPath;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private List<UnknownSchemaNode> unknownNodes = Collections
                .emptyList();
        private boolean yin;

        private ExtensionDefinitionImpl(QName qname) {
            this.qname = qname;
        }

        @Override
        public QName getQName() {
            return qname;
        }

        @Override
        public SchemaPath getPath() {
            return schemaPath;
        }

        private void setPath(SchemaPath schemaPath) {
            this.schemaPath = schemaPath;
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
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return unknownNodes;
        }

        private void setUnknownSchemaNodes(
                List<UnknownSchemaNode> unknownNodes) {
            if(unknownNodes != null) {
                this.unknownNodes = unknownNodes;
            }
        }

        @Override
        public String getArgument() {
            return argument;
        }

        private void setArgument(String argument) {
            this.argument = argument;
        }

        @Override
        public boolean isYinElement() {
            return yin;
        }

        private void setYinElement(boolean yin) {
            this.yin = yin;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((qname == null) ? 0 : qname.hashCode());
            result = prime * result
                    + ((schemaPath == null) ? 0 : schemaPath.hashCode());
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
            ExtensionDefinitionImpl other = (ExtensionDefinitionImpl) obj;
            if (qname == null) {
                if (other.qname != null) {
                    return false;
                }
            } else if (!qname.equals(other.qname)) {
                return false;
            }
            if (schemaPath == null) {
                if (other.schemaPath != null) {
                    return false;
                }
            } else if (!schemaPath.equals(other.schemaPath)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(
                    ExtensionDefinitionImpl.class.getSimpleName());
            sb.append("[");
            sb.append("argument="+ argument);
            sb.append(", qname=" + qname);
            sb.append(", schemaPath=" + schemaPath);
            sb.append(", extensionSchemaNodes=" + unknownNodes);
            sb.append(", yin=" + yin);
            sb.append("]");
            return sb.toString();
        }
    }

}
