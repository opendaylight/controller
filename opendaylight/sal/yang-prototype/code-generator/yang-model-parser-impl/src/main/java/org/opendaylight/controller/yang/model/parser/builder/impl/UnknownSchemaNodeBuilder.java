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
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.parser.builder.api.SchemaNodeBuilder;

public class UnknownSchemaNodeBuilder implements SchemaNodeBuilder {

    private final QName qname;
    private SchemaPath schemaPath;
    private final UnknownSchemaNodeImpl instance;
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    UnknownSchemaNodeBuilder(final QName qname) {
        this.qname = qname;
        instance = new UnknownSchemaNodeImpl(qname);
    }

    @Override
    public UnknownSchemaNode build() {
        instance.setPath(schemaPath);

        // UNKNOWN NODES
        final List<UnknownSchemaNode> unknownNodes = new ArrayList<UnknownSchemaNode>();
        for (UnknownSchemaNodeBuilder b : addedUnknownNodes) {
            unknownNodes.add(b.build());
        }
        instance.setUnknownSchemaNodes(unknownNodes);
        return instance;
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
        instance.setStatus(status);
    }

    @Override
    public void addUnknownSchemaNode(final UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
    }

    private static class UnknownSchemaNodeImpl implements UnknownSchemaNode {
        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();

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
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return unknownNodes;
        }

        private void setUnknownSchemaNodes(
                final List<UnknownSchemaNode> unknownNodes) {
            if (unknownNodes != null) {
                this.unknownNodes = unknownNodes;
            }
        }
    }

}
