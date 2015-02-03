/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.match;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the generic matching field
 *
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class MatchField implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(MatchField.class);
    private MatchType type; // the field we want to match
    private Object value; // the value of the field we want to match
    private Object mask; // the value of the mask we want to match on the
    // specified field
    private boolean isValid;

    // To satisfy JAXB
    @SuppressWarnings("unused")
    private MatchField() {
    }

    /**
     * Mask based match constructor
     *
     * @param type
     * @param value
     * @param mask
     *            has to be of the same class type of value. A null mask means
     *            full match
     */
    public MatchField(MatchType type, Object value, Object mask) {
        this.type = type;
        this.value = value;
        this.mask = mask;
        // Keep this logic, value checked only if type check is fine
        this.isValid = checkValueType() && checkValues();
    }

    /**
     * Full match constructor
     *
     * @param type
     * @param value
     */
    public MatchField(MatchType type, Object value) {
        this.type = type;
        this.value = value;
        this.mask = null;
        // Keep this logic, value checked only if type check is fine
        this.isValid = checkValueType() && checkValues();
    }

    /**
     * Returns the value set for this match field
     *
     * @return
     */
    public Object getValue() {
        return value;
    }

    @XmlElement(name = "value")
    private String getValueString() {
        return type.stringify(value);
    }

    /**
     * Returns the type field this match field object is for
     *
     * @return
     */
    public MatchType getType() {
        return type;
    }

    @XmlElement(name = "type")
    private String getTypeString() {
        return type.toString();
    }

    /**
     * Returns the mask value set for this field match A null mask means this is
     * a full match
     *
     * @return
     */
    public Object getMask() {
        return mask;
    }

    @XmlElement(name = "mask")
    private String getMaskString() {
        return type.stringify(mask);
    }

    /**
     * Returns the bitmask set for this field match
     *
     * @return
     */
    public long getBitMask() {
        return type.getBitMask(mask);
    }

    /**
     * Returns whether the field match configuration is valid or not
     *
     * @return
     */
    public boolean isValid() {
        return isValid;
    }

    private boolean checkValueType() {
        if (type.isCongruentType(value, mask) == false) {
            String valueClass = (value == null) ? "null" : value.getClass().getSimpleName();
            String maskClass = (mask == null) ? "null" : mask.getClass().getSimpleName();
            String error = "Invalid match field's value or mask types.For field: " + type.id() + " Expected:"
                    + type.dataType().getSimpleName() + " or equivalent," + " Got:(" + valueClass + "," + maskClass
                    + ")";
            throwException(error);
            return false;
        }
        return true;
    }

    private boolean checkValues() {
        if (type.isValid(value, mask) == false) {
            String maskString = (mask == null) ? "null" : ("0x" + Integer
                    .toHexString(Integer.parseInt(mask.toString())));
            String error = "Invalid match field's value or mask assignement.For field: " + type.id() + " Expected: "
                    + type.getRange() + ", Got:(0x" + Integer.toHexString(Integer.parseInt(value.toString())) + ","
                    + maskString + ")";

            throwException(error);
            return false;
        }
        return true;
    }

    private static void throwException(String error) {
        try {
            throw new Exception(error);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public MatchField clone() {
        MatchField cloned = null;
        try {
            cloned = (MatchField) super.clone();
            if (value instanceof byte[]) {
                cloned.value = ((byte[]) this.value).clone();
                if (this.mask != null) {
                    cloned.mask = ((byte[]) this.mask).clone();
                }
            }
            cloned.type = this.type;
            cloned.isValid = this.isValid;
        } catch (CloneNotSupportedException e) {
            logger.error("", e);
        }
        return cloned;
    }

    @Override
    public String toString() {
        return (mask == null) ? String.format("%s(%s)", getTypeString(), getValueString()) :
            String.format("%s(%s,%s)", getTypeString(), getValueString(), getMaskString());
    }

    @Override
    public int hashCode() {
        return type.hashCode(value, mask);
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
        MatchField other = (MatchField) obj;
        if (type != other.type) {
            return false;
        }
        return type.equals(this.value, other.value, this.mask, other.mask);
    }
}
