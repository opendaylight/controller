
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
 * @file   AdvertisedBandWidth.java
 *
 * @brief  Class representing advertised bandwidth
 *
 * Describes Advertised Bandwidth which could be of a link or whatever could have
 * bandwidth as description. It's intended in multiple of bits per
 * seconds.
 */
@XmlRootElement
@SuppressWarnings("serial")
public class AdvertisedBandwidth extends Bandwidth {
	public static final String AdvertisedBandwidthPropName = "advertisedBandwidth";
	
	public AdvertisedBandwidth(long value) {
		super(AdvertisedBandwidthPropName);
		this.bandwidthValue = value;
	}
	
	/*
     * Private constructor used for JAXB mapping
     */
    private AdvertisedBandwidth() {
    	super(AdvertisedBandwidthPropName);
		this.bandwidthValue = 0;
    }
	
	public AdvertisedBandwidth clone() {
		return new AdvertisedBandwidth(this.bandwidthValue);  
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
        sb.append("AdvertisedBandWidth[");
        sb.append(super.toString());
        sb.append("]");
        return sb.toString();
    }

}
