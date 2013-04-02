
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;

/**
 * The class represents the Name property of an element.
 */
@XmlRootElement
@SuppressWarnings("serial")
public class Name extends Property {
    @XmlElement
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

    public Name clone() {
        return new Name(this.nameValue);
    }

    public String getValue() {
        return this.nameValue;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return "Name[" + ReflectionToStringBuilder.toString(this) + "]";
    }
}
