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
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.LeafrefTypeDefinition;

/**
 * The <code>default</code> implementation of Instance Leafref Type Definition
 * interface.
 * 
 * @see LeafrefTypeDefinition
 */
public class Leafref implements LeafrefTypeDefinition {
    private static final QName name = BaseTypes.constructQName("leafref");
    private static final String description = "The leafref type is used to reference a "
            + "particular leaf instance in the data tree.";
    private static final String reference = "https://tools.ietf.org/html/rfc6020#section-9.9";
    private final SchemaPath path;
    private final RevisionAwareXPath xpath;
    private final String units = "";
    private final LeafrefTypeDefinition baseType;

    private Leafref(final RevisionAwareXPath xpath) {
        this.xpath = xpath;
        this.path = BaseTypes.schemaPath(name);
        this.baseType = this;
    }
    
    public Leafref(final List<String> actualPath, final URI namespace,
            final Date revision, final RevisionAwareXPath xpath) {
        super();
        this.path = BaseTypes.schemaPath(actualPath, namespace, revision);
        this.xpath = xpath;
        baseType = new Leafref(xpath);
    }
    
    public Leafref(final List<String> actualPath, final URI namespace,
            final Date revision, final LeafrefTypeDefinition baseType,
            final RevisionAwareXPath xpath) {
        super();
        this.path = BaseTypes.schemaPath(actualPath, namespace, revision);
        this.xpath = xpath;
        this.baseType = baseType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.controller.yang.model.api.TypeDefinition#getBaseType()
     */
    @Override
    public LeafrefTypeDefinition getBaseType() {
        return baseType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.TypeDefinition#getUnits()
     */
    @Override
    public String getUnits() {
        return units;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.controller.yang.model.api.TypeDefinition#getDefaultValue
     * ()
     */
    @Override
    public Object getDefaultValue() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getQName()
     */
    @Override
    public QName getQName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getPath()
     */
    @Override
    public SchemaPath getPath() {
        return path;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.controller.yang.model.api.SchemaNode#getDescription()
     */
    @Override
    public String getDescription() {
        return description;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getReference()
     */
    @Override
    public String getReference() {
        return reference;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getStatus()
     */
    @Override
    public Status getStatus() {
        return Status.CURRENT;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.controller.yang.model.api.SchemaNode#getExtensionSchemaNodes
     * ()
     */
    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.controller.yang.model.api.type.LeafrefTypeDefinition
     * #getPathStatement()
     */
    @Override
    public RevisionAwareXPath getPathStatement() {
        return xpath;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((units == null) ? 0 : units.hashCode());
        result = prime * result + ((xpath == null) ? 0 : xpath.hashCode());
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
        Leafref other = (Leafref) obj;
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        if (units == null) {
            if (other.units != null) {
                return false;
            }
        } else if (!units.equals(other.units)) {
            return false;
        }
        if (xpath == null) {
            if (other.xpath != null) {
                return false;
            }
        } else if (!xpath.equals(other.xpath)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Leafref [path=");
        builder.append(path);
        builder.append(", xpath=");
        builder.append(xpath);
        builder.append(", units=");
        builder.append(units);
        builder.append("]");
        return builder.toString();
    }
}
