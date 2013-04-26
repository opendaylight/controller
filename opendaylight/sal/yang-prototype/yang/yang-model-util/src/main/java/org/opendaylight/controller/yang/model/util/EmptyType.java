/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.EmptyTypeDefinition;

public class EmptyType implements EmptyTypeDefinition {

    private final QName name = BaseTypes.constructQName("empty");
    private final SchemaPath path;
    private final String description = "The empty built-in type represents a leaf that does not have any value, it conveys information by its presence or absence.";
    private final String reference = "https://tools.ietf.org/html/rfc6020#page-131";
    private final EmptyTypeDefinition baseType;

    private EmptyType() {
        path = BaseTypes.schemaPath(name);
        this.baseType = this;
    }

    public EmptyType(final List<String> actualPath,
            final URI namespace, final Date revision) {
        path = BaseTypes.schemaPath(actualPath, namespace, revision);
        this.baseType = new EmptyType();
    }

    @Override
    public EmptyTypeDefinition getBaseType() {
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

}
