
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * @file   Tables.java
 *
 * @brief  Class representing tables
 *
 * Describes supported # of datapath tables
 */
@XmlRootElement
public class Tables extends Property {
	private static final long serialVersionUID = 1L;
    @XmlElement
    private byte tablesValue;
    
    public static final String TablesPropName = "tables";
    /**
     * Construct a Tables property
     *
     * @param tables the Tables 
     * @return Constructed object
     */
    public Tables(byte tables) {
        super(TablesPropName);
        this.tablesValue = tables;
    }

    /*
     * Private constructor used for JAXB mapping
     */
    private Tables() {
        super(TablesPropName);
        this.tablesValue = 0;
    }

    public Tables clone() {
        return new Tables(this.tablesValue);
    }

    public byte getValue() {
        return this.tablesValue;
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
        return "Tables[" + ReflectionToStringBuilder.toString(this) + "]";
    }
}
