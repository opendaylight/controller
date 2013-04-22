/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.util;

import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition.Bit;

public final class BitImpl implements BitsTypeDefinition.Bit {
    private final Long position;
    private final QName qname;
    private final SchemaPath schemaPath;
    private final String description;
    private final String reference;
    private final Status status;
    private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();

    BitImpl(final Long position, final QName qname,
            final SchemaPath schemaPath, final String description,
            final String reference, final Status status,
            final List<UnknownSchemaNode> unknownNodes) {
        this.position = position;
        this.qname = qname;
        this.schemaPath = schemaPath;
        this.description = description;
        this.reference = reference;
        this.status = status;
        if(unknownNodes != null) {
            this.unknownNodes = unknownNodes;
        }
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
    public String getDescription() {
        return description;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return unknownNodes;
    }

    @Override
    public Long getPosition() {
        return position;
    }

    @Override
    public String getName() {
        return qname.getLocalName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((qname == null) ? 0 : qname.hashCode());
        result = prime * result
                + ((schemaPath == null) ? 0 : schemaPath.hashCode());
        result = prime * result
                + ((position == null) ? 0 : position.hashCode());
        result = prime
                * result
                + ((unknownNodes == null) ? 0 : unknownNodes.hashCode());
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
        Bit other = (Bit) obj;
        if (qname == null) {
            if (other.getQName() != null) {
                return false;
            }
        } else if (!qname.equals(other.getQName())) {
            return false;
        }
        if (schemaPath == null) {
            if (other.getPath() != null) {
                return false;
            }
        } else if (!schemaPath.equals(other.getPath())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return Bit.class.getSimpleName() + "[name="
                + qname.getLocalName() + ", position=" + position + "]";
    }

}
