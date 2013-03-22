
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
 * @file   Capabilities.java
 *
 * @brief  Class representing capabilities
 *
 * Describes supported capabilities
 */
@XmlRootElement
public class Capabilities extends Property {
	private static final long serialVersionUID = 1L;
    @XmlElement
    private int capabilitiesValue;
    
    public enum CapabilitiesType { 
    	FLOW_STATS_CAPABILITY(1<<0),
    	TABLE_STATS_CAPABILITY(1<<1),
    	PORT_STATS_CAPABILITY(1<<2),
    	STP_CAPABILITY(1<<3),
    	RSVD_CAPABILITY(1<<4),
    	IP_REASSEM_CAPABILITY(1<<5),
    	QUEUE_STATS_CAPABILITY(1<<6),
    	ARP_MATCH_IP_CAPABILITY(1<<7);
    	private final int ct;
    	CapabilitiesType(int val) {
    		this.ct = val;
    	}
    	public int getValue() {
    		return ct;
    	}
    }
   
    public static final String CapabilitiesPropName = "capabilities";
    /**
     * Construct a Capabilities property
     *
     * @param capabilities the Capabilities value
     * @return Constructed object
     */
    public Capabilities(int capabilities) {
        super(CapabilitiesPropName);
        this.capabilitiesValue = capabilities;
    }

    /*
     * Private constructor used for JAXB mapping
     */
    private Capabilities() {
        super(CapabilitiesPropName);
        this.capabilitiesValue = 0;
    }

    public Capabilities clone() {
        return new Capabilities(this.capabilitiesValue);
    }

    public int getValue() {
    	return this.capabilitiesValue;
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
        return "Capabilities[" + ReflectionToStringBuilder.toString(this) + "]";
    }
}
