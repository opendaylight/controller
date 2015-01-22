
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
 * @file   Buffers.java
 *
 * @brief  Class representing buffers
 *
 * Describes supported buffers (#packets)
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class Buffers extends Property {
        private static final long serialVersionUID = 1L;
    @XmlElement(name="value")
    private int buffersValue;

    public static final String BuffersPropName = "buffers";

    /**
     * Construct a Buffers property
     *
     * @param buffers the Buffers
     * @return Constructed object
     */
    public Buffers(int buffers) {
        super(BuffersPropName);
        this.buffersValue = buffers;
    }

    /*
     * Private constructor used for JAXB mapping
     */
    private Buffers() {
        super(BuffersPropName);
        this.buffersValue = 0;
    }

    @Override
    public Buffers clone() {
        return new Buffers(this.buffersValue);
    }

    public int getValue() {
        return this.buffersValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + buffersValue;
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
        Buffers other = (Buffers) obj;
        if (buffersValue != other.buffersValue)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Buffers[" + buffersValue + "]";
    }

    @Override
    public String getStringValue() {
        return Integer.toHexString(buffersValue);
    }
}
