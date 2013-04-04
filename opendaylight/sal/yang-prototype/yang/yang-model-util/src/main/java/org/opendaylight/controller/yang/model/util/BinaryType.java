/*
  * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
  *
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  */
package org.opendaylight.controller.yang.model.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.BinaryTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;

/**
 * The <code>default</code> implementation of Binary Type Definition interface.
 * 
 * @see BinaryTypeDefinition
 */
public class BinaryType implements BinaryTypeDefinition {

    private final QName name = BaseTypes.constructQName("binary");
    private final SchemaPath path = BaseTypes.schemaPath(name);
    private final String description = "The binary built-in type represents any binary data, i.e., a sequence of octets.";
    private final String reference = "https://tools.ietf.org/html/rfc6020#section-9.8";

    private List<Byte> bytes;
    private final List<LengthConstraint> lengthConstraints;
    private String units = "";

    /**
     * 
     */
    public BinaryType() {
        super();
        
        final List<LengthConstraint> constraints = new ArrayList<LengthConstraint>();
        constraints.add(BaseConstraints.lengthConstraint(0, Long.MAX_VALUE, "", ""));
        lengthConstraints = Collections.unmodifiableList(constraints);
        bytes = Collections.emptyList();
    }

    /**
     * 
     * 
     * @param bytes
     * @param lengthConstraints
     * @param units
     */
    public BinaryType(final List<Byte> bytes,
            final List<LengthConstraint> lengthConstraints, final String units) {
        super();
        
        if ((lengthConstraints == null) || (lengthConstraints.isEmpty())) {
            final List<LengthConstraint> constraints = new ArrayList<LengthConstraint>();
            constraints.add(BaseConstraints.lengthConstraint(0, Long.MAX_VALUE, "", ""));
            this.lengthConstraints = Collections.unmodifiableList(constraints);
        } else {
            this.lengthConstraints = Collections.unmodifiableList(lengthConstraints);
        }
        
        this.bytes = Collections.unmodifiableList(bytes);
        this.units = units;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.controller.yang.model.api.TypeDefinition#getBaseType()
     */
    @Override
    public BinaryTypeDefinition getBaseType() {
        return this;
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
     * @see org.opendaylight.controller.yang.model.api.TypeDefinition#getDefaultValue()
     */
    @Override
    public Object getDefaultValue() {
        return bytes;
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
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getDescription()
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
     * org.opendaylight.controller.yang.model.base.type.api.BinaryTypeDefinition#getLengthConstraint
     * ()
     */
    @Override
    public List<LengthConstraint> getLengthConstraints() {
        return lengthConstraints;
    }

    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return Collections.emptyList();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bytes == null) ? 0 : bytes.hashCode());
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        result = prime
                * result
                + ((lengthConstraints == null) ? 0 : lengthConstraints.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result
                + ((reference == null) ? 0 : reference.hashCode());
        result = prime * result + ((units == null) ? 0 : units.hashCode());
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
        BinaryType other = (BinaryType) obj;
        if (bytes == null) {
            if (other.bytes != null) {
                return false;
            }
        } else if (!bytes.equals(other.bytes)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (lengthConstraints == null) {
            if (other.lengthConstraints != null) {
                return false;
            }
        } else if (!lengthConstraints.equals(other.lengthConstraints)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        if (reference == null) {
            if (other.reference != null) {
                return false;
            }
        } else if (!reference.equals(other.reference)) {
            return false;
        }
        if (units == null) {
            if (other.units != null) {
                return false;
            }
        } else if (!units.equals(other.units)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BinaryType [name=");
        builder.append(name);
        builder.append(", path=");
        builder.append(path);
        builder.append(", description=");
        builder.append(description);
        builder.append(", reference=");
        builder.append(reference);
        builder.append(", bytes=");
        builder.append(bytes);
        builder.append(", lengthConstraints=");
        builder.append(lengthConstraints);
        builder.append(", units=");
        builder.append(units);
        builder.append("]");
        return builder.toString();
    }
}
