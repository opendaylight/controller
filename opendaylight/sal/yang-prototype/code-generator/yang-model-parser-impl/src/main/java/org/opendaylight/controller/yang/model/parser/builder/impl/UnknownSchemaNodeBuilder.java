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
    private final UnknownSchemaNodeImpl instance;
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    UnknownSchemaNodeBuilder(final QName qname) {
        this.qname = qname;
        instance = new UnknownSchemaNodeImpl(qname);
    }


    @Override
    public UnknownSchemaNode build() {
        // UNKNOWN NODES
        final List<UnknownSchemaNode> unknownNodes = new ArrayList<UnknownSchemaNode>();
        for(UnknownSchemaNodeBuilder b : addedUnknownNodes) {
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
    public void setPath(SchemaPath schemaPath) {
        instance.setPath(schemaPath);
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
        instance.setStatus(status);
    }

    @Override
    public void addUnknownSchemaNode(UnknownSchemaNodeBuilder unknownSchemaNodeBuilder) {
        addedUnknownNodes.add(unknownSchemaNodeBuilder);
    }

    private static class UnknownSchemaNodeImpl implements UnknownSchemaNode {
        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private List<UnknownSchemaNode> unknownSchemaNodes = Collections.emptyList();

        private UnknownSchemaNodeImpl(QName qname) {
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
            if(status != null) {
                this.status = status;
            }
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return unknownSchemaNodes;
        }

        private void setUnknownSchemaNodes(List<UnknownSchemaNode> unknownSchemaNodes) {
            if(unknownSchemaNodes != null) {
                this.unknownSchemaNodes = unknownSchemaNodes;
            }
        }
    }

}
