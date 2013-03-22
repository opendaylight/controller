
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
 * @file   Buffers.java
 *
 * @brief  Class representing buffers
 *
 * Describes supported buffers (#packets)
 */
@XmlRootElement
public class Buffers extends Property {
	private static final long serialVersionUID = 1L;
    @XmlElement
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

    public Buffers clone() {
        return new Buffers(this.buffersValue);
    }

    public int getValue() {
        return this.buffersValue;
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
        return "Buffers[" + ReflectionToStringBuilder.toString(this) + "]";
    }
}
