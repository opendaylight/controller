/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @file   Property.java
 *
 * @brief  Abstract base class for a Property that can be attached to
 * any sal core element
 *
 * Abstract base class for a Property that can be attached to any sal
 * core element
 */

/**
 * Abstract base class for a Property that can be attached to any sal core
 * element
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
abstract public class Property implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private final String name;

    /**
     * Private constructor used for JAXB mapping
     */
    @SuppressWarnings("unused")
    private Property() {
        this.name = null;
    }

    protected Property(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public abstract String getStringValue();

    /**
     * Used to copy the Property in a polymorphic way
     *
     * @return A clone of this Property
     */
    @Override
    public abstract Property clone();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Property other = (Property) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Property [name=" + name + "]";
    }
}
