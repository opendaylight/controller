/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.parser.builder.impl.UnknownSchemaNodeBuilder;

/**
 * Basic implementation of SchemaNodeBuilder.
 */
public abstract class AbstractSchemaNodeBuilder implements SchemaNodeBuilder {
    protected final int line;
    protected final QName qname;
    protected SchemaPath path;
    protected String description;
    protected String reference;
    protected Status status = Status.CURRENT;
    protected final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    protected AbstractSchemaNodeBuilder(final QName qname, final int line) {
        this.qname = qname;
        this.line = line;
    }

    @Override
    public int getLine() {
        return line;
    }

    public QName getQName() {
        return qname;
    }

    public SchemaPath getPath() {
        return path;
    }

    public void setPath(SchemaPath schemaPath) {
        this.path = schemaPath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        if (status != null) {
            this.status = status;
        }
    }

    @Override
    public void addUnknownSchemaNode(UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
    }

}
