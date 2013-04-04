/*
  * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
  *
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  */
package org.opendaylight.controller.yang.model.util;

import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition;


/**
 * The <code>default</code> implementation of Bits Type Definition interface.
 * 
 * @see BitsTypeDefinition
 */
public class BitsType implements BitsTypeDefinition {

    private final QName name = BaseTypes.constructQName("bits");
    private final SchemaPath path = BaseTypes.schemaPath(name);
    private final String description = "The bits built-in type represents a bit set.  " +
    		"That is, a bits value is a set of flags identified by small integer position " +
    		"numbers starting at 0.  Each bit number has an assigned name.";
    
    private final String reference = "https://tools.ietf.org/html/rfc6020#section-9.7";

    private final List<Bit> bits;
    private String units = "";

    /**
     * Default constructor. <br>
     * Instantiates Bits type as empty bits list.
     */
    public BitsType() {
        super();
        bits = Collections.emptyList();
    }

    /**
     * Constructor with explicit definition of bits assigned to
     * BitsType.
     * 
     * @param bits
     *            The bits assigned for Bits Type
     */
    public BitsType(final List<Bit> bits) {
        super();
        this.bits = Collections.unmodifiableList(bits);
        this.units = "";
    }

    /**
     * Constructor with explicit definition of bits assigned to
     * BitsType and Units.
     * <br>
     * The default value of Bits Type is List of bits.
     * 
     * @param bits The bits assigned for Bits Type
     * @param units units for bits type
     */
    public BitsType(List<Bit> bits, String units) {
        super();
        this.bits = Collections.unmodifiableList(bits);
        this.units = units;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.TypeDefinition#getBaseType()
     */
    @Override
    public BitsTypeDefinition getBaseType() {
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
        return bits;
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

    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return Collections.emptyList();
    }

    @Override
    public List<Bit> getBits() {
        return bits;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bits == null) ? 0 : bits.hashCode());
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
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
        BitsType other = (BitsType) obj;
        if (bits == null) {
            if (other.bits != null) {
                return false;
            }
        } else if (!bits.equals(other.bits)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
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
        builder.append("BitsType [name=");
        builder.append(name);
        builder.append(", path=");
        builder.append(path);
        builder.append(", description=");
        builder.append(description);
        builder.append(", reference=");
        builder.append(reference);
        builder.append(", bits=");
        builder.append(bits);
        builder.append(", units=");
        builder.append(units);
        builder.append("]");
        return builder.toString();
    }
}
