
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
 * @file   Tables.java
 *
 * @brief  Class representing tables
 *
 * Describes supported # of datapath tables
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class Tables extends Property {
        private static final long serialVersionUID = 1L;
    @XmlElement(name="value")
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

    @Override
    public Tables clone() {
        return new Tables(this.tablesValue);
    }

    public byte getValue() {
        return this.tablesValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + tablesValue;
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
        Tables other = (Tables) obj;
        if (tablesValue != other.tablesValue)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Tables[" + tablesValue + "]";
    }

    @Override
    public String getStringValue() {
        return String.format("%02x", tablesValue);
    }
}
