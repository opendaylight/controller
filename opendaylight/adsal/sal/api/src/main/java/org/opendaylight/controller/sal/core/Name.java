
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The class represents the Name property of an element.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings("serial")
@Deprecated
public class Name extends Property {
    @XmlElement(name="value")
    private String nameValue;
    public static final String NamePropName = "name";

    /*
     * Private constructor used for JAXB mapping
     */
    private Name() {
        super(NamePropName);
        this.nameValue = null;
    }

    public Name(String name) {
        super(NamePropName);
        this.nameValue = name;
    }

    @Override
    public Name clone() {
        return new Name(this.nameValue);
    }

    public String getValue() {
        return this.nameValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((nameValue == null) ? 0 : nameValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Name other = (Name) obj;
        if (nameValue == null) {
            if (other.nameValue != null)
                return false;
        } else if (!nameValue.equals(other.nameValue))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Name[" + nameValue + "]";
    }

    @Override
    public String getStringValue() {
        return nameValue;
    }
}
