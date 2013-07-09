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
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.parser.builder.api.AbstractSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.util.Comparators;

public final class UnknownSchemaNodeBuilder extends AbstractSchemaNodeBuilder {
    private boolean isBuilt;
    private final UnknownSchemaNodeImpl instance;
    private boolean addedByUses;
    private QName nodeType;
    private String nodeParameter;

    public UnknownSchemaNodeBuilder(final int line, final QName qname) {
        super(line, qname);
        instance = new UnknownSchemaNodeImpl(qname);
    }

    public UnknownSchemaNodeBuilder(UnknownSchemaNodeBuilder b) {
        super(b.getLine(), b.getQName());
        instance = new UnknownSchemaNodeImpl(qname);
        schemaPath = b.getPath();
        description = b.getDescription();
        reference = b.getReference();
        status = b.getStatus();
        addedByUses = b.isAddedByUses();
        unknownNodes = b.unknownNodes;
        addedUnknownNodes.addAll(b.addedUnknownNodes);
        nodeType = b.getNodeType();
        nodeParameter = b.getNodeParameter();
    }

    @Override
    public UnknownSchemaNode build() {
        if (!isBuilt) {
            instance.setPath(schemaPath);
            instance.setNodeType(nodeType);
            instance.setNodeParameter(nodeParameter);
            instance.setDescription(description);
            instance.setReference(reference);
            instance.setStatus(status);
            instance.setAddedByUses(addedByUses);

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

    public boolean isAddedByUses() {
        return addedByUses;
    }

    public void setAddedByUses(final boolean addedByUses) {
        this.addedByUses = addedByUses;
    }

    public QName getNodeType() {
        return nodeType;
    }

    public void setNodeType(final QName nodeType) {
        this.nodeType = nodeType;
    }

    public String getNodeParameter() {
        return nodeParameter;
    }

    public void setNodeParameter(final String nodeParameter) {
        this.nodeParameter = nodeParameter;
    }

    private final class UnknownSchemaNodeImpl implements UnknownSchemaNode {
        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();
        private QName nodeType;
        private String nodeParameter;
        private boolean addedByUses;

        private UnknownSchemaNodeImpl(final QName qname) {
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

        private void setPath(final SchemaPath path) {
            this.path = path;
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
        public boolean isAddedByUses() {
            return addedByUses;
        }

        private void setAddedByUses(final boolean addedByUses) {
            this.addedByUses = addedByUses;
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return unknownNodes;
        }

        private void setUnknownSchemaNodes(final List<UnknownSchemaNode> unknownNodes) {
            if (unknownNodes != null) {
                this.unknownNodes = unknownNodes;
            }
        }

        @Override
        public QName getNodeType() {
            return nodeType;
        }

        private void setNodeType(final QName nodeType) {
            this.nodeType = nodeType;
        }

        @Override
        public String getNodeParameter() {
            return nodeParameter;
        }

        private void setNodeParameter(final String nodeParameter) {
            this.nodeParameter = nodeParameter;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(UnknownSchemaNodeImpl.class.getSimpleName());
            sb.append("[");
            sb.append(qname);
            sb.append("]");
            return sb.toString();
        }
    }

}
