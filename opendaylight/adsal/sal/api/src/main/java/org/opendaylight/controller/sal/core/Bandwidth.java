
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
 * @file   BandWidth.java
 *
 * @brief  Class representing bandwidth
 *
 * Describe Bandwidth which could be of a link or whatever could have
 * bandwidth as description. It's intended in multiple of bits per
 * seconds.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class Bandwidth extends Property {
    private static final long serialVersionUID = 1L;

    @XmlElement(name="value")
    protected long bandwidthValue;

    public static final long BWUNK = 0;
    public static final long BW1Kbps = (long) Math.pow(10, 3);
    public static final long BW1Mbps = (long) Math.pow(10, 6);
    public static final long BW10Mbps = (long) Math.pow(10, 7);
    public static final long BW100Mbps = (long) Math.pow(10, 8);
    public static final long BW1Gbps = (long) Math.pow(10, 9);
    public static final long BW10Gbps = (long) Math.pow(10, 10);
    public static final long BW40Gbps = 4 * (long) Math.pow(10, 10);
    public static final long BW100Gbps = (long) Math.pow(10, 11);
    public static final long BW400Gbps = 4 * (long) Math.pow(10, 11);
    public static final long BW1Tbps = (long) Math.pow(10, 12);
    public static final long BW1Pbps = (long) Math.pow(10, 15);

    public static final String BandwidthPropName = "bandwidth";

    /*
     * Private constructor used for JAXB mapping
     */
    private Bandwidth() {
        super(BandwidthPropName);
        this.bandwidthValue = BWUNK;
    }

    public Bandwidth(long bandwidth) {
        super(BandwidthPropName);
        this.bandwidthValue = bandwidth;
    }

    public Bandwidth(int bandwidth) {
        super(BandwidthPropName);
        this.bandwidthValue = (long) bandwidth;
    }

    public Bandwidth(short bandwidth) {
        super(BandwidthPropName);
        this.bandwidthValue = (long) bandwidth;
    }

    public Bandwidth(String name) {
        super(name);
    }

    @Override
    public Bandwidth clone() {
        return new Bandwidth(this.bandwidthValue);
    }

    public long getValue() {
        return this.bandwidthValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + (int) (bandwidthValue ^ (bandwidthValue >>> 32));
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
        Bandwidth other = (Bandwidth) obj;
        if (bandwidthValue != other.bandwidthValue)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BandWidth[" + getStringValue() + "]";
    }

    @Override
    public String getStringValue() {
        if (this.bandwidthValue == 0) {
            return("UnKnown");
        } else if (this.bandwidthValue < BW1Kbps) {
            return(this.bandwidthValue + "bps");
        } else if (this.bandwidthValue < BW1Mbps) {
            return(Long.toString(this.bandwidthValue / BW1Kbps) + "Kbps");
        } else if (this.bandwidthValue < BW1Gbps) {
            return(Long.toString(this.bandwidthValue / BW1Mbps) + "Mbps");
        } else if (this.bandwidthValue < BW1Tbps) {
            return(Long.toString(this.bandwidthValue / BW1Gbps) + "Gbps");
        } else if (this.bandwidthValue < BW1Pbps) {
            return(Long.toString(this.bandwidthValue / BW1Tbps) + "Tbps");
        } else {
            return(this.bandwidthValue + "bps");
        }
    }
}
