/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.match.extensible;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents the generic matching field object
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public abstract class MatchField<T> implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    private String type;

    // To satisfy JAXB
    @SuppressWarnings("unused")
    private MatchField() {
    }

    public MatchField(String type) {
        this.type = type;
    }

    @XmlElement(name = "type")
    public String getType() {
        return type;
    }

    /**
     * Returns the value set for this match field
     *
     * @return
     */
    public abstract T getValue();

    @XmlElement(name = "value")
    protected abstract String getValueString();

    /**
     * Returns the mask value set for this field match A null mask means this is
     * a full match
     *
     * @return
     */
    public abstract T getMask();

    @XmlElement(name = "mask")
    protected abstract String getMaskString();

    /**
     * Returns whether the field match configuration is valid or not
     *
     * @return true if valid, false otherwise
     */
    public abstract boolean isValid();

    public abstract boolean hasReverse();

    /**
     * Returns the reverse match field. For example for a MatchField matching on
     * source ip 1.1.1.1 it will return a MatchField matching on destination IP
     * 1.1.1.1. For not reversable MatchField, a copy of this MatchField will be
     * returned
     *
     * @return the correspondent reverse MatchField object or a copy of this
     *         object if the field is not reversable
     */
    public abstract MatchField<T> getReverse();

    /**
     * Returns whether the match field is congruent with IPv6 frames
     *
     * @return true if congruent with IPv6 frames
     */
    public abstract boolean isV6();

    @Override
    public abstract MatchField<T> clone();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        if (!(obj instanceof MatchField)) {
            return false;
        }
        MatchField<?> other = (MatchField<?>) obj;
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return (getMask() == null) ? String.format("%s(%s)", getType(), getValueString()) :
            String.format("%s(%s,%s)", getType(), getValueString(), getMaskString());
    }

}
