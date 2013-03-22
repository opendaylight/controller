
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @file   SupportedBandWidth.java
 *
 * @brief  Class representing supported bandwidth
 *
 * Describes Supported Bandwidth which could be of a link or whatever could have
 * bandwidth as description. It's intended in multiple of bits per
 * seconds.
 */
@XmlRootElement
public class SupportedBandwidth extends Bandwidth {
	private static final long serialVersionUID = 1L;
	public static final String SupportedBandwidthPropName = "supportedBandwidth";
	
	public SupportedBandwidth(long value) {
		super(SupportedBandwidthPropName);
		this.bandwidthValue = value;
	}
	
	/*
     * Private constructor used for JAXB mapping
     */
    private SupportedBandwidth() {
    	super(SupportedBandwidthPropName);
		this.bandwidthValue = 0;
    }
	
	public SupportedBandwidth clone() {
		return new SupportedBandwidth(this.bandwidthValue);  
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
        StringBuffer sb = new StringBuffer();
        sb.append("SupportedBandWidth[");
        sb.append(super.toString());
        sb.append("]");
        return sb.toString();
    }


}