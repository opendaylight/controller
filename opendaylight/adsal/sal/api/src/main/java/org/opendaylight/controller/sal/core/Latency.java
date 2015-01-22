
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
 * @file   Latency.java
 *
 * @brief  Class representing Latency
 *
 * Describe a latency in picoseconds or multiple of its.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class Latency extends Property {
    private static final long serialVersionUID = 1L;

    @XmlElement(name="value")
    private long latencyValue;

    public static final long LATENCYUNK = 0;
    public static final long LATENCY1ns = (long) Math.pow(10, 3);
    public static final long LATENCY10ns = (long) Math.pow(10, 4);
    public static final long LATENCY100ns = (long) Math.pow(10, 5);
    public static final long LATENCY1us = (long) Math.pow(10, 6);
    public static final long LATENCY10us = (long) Math.pow(10, 7);
    public static final long LATENCY100us = (long) Math.pow(10, 8);
    public static final long LATENCY1ms = (long) Math.pow(10, 9);
    public static final long LATENCY1s = (long) Math.pow(10, 12);

    public static final String LatencyPropName = "latency";

    /*
     * Private constructor used for JAXB mapping
     */
    private Latency() {
        super(LatencyPropName);
        this.latencyValue = LATENCYUNK;
    }

    public Latency(long latency) {
        super(LatencyPropName);
        this.latencyValue = latency;
    }

    public Latency(int latency) {
        super(LatencyPropName);
        this.latencyValue = (long) latency;
    }

    public Latency(short latency) {
        super(LatencyPropName);
        this.latencyValue = (long) latency;
    }

    @Override
    public Latency clone() {
        return new Latency(this.latencyValue);
    }

    public long getValue() {
        return this.latencyValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (int) (latencyValue ^ (latencyValue >>> 32));
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
        Latency other = (Latency) obj;
        if (latencyValue != other.latencyValue)
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("Latency[");
        if (this.latencyValue == 0) {
            sb.append("UnKnown");
        } else if (this.latencyValue < LATENCY1ns) {
            sb.append(this.latencyValue).append("psec");
        } else if (this.latencyValue < LATENCY1us) {
            sb.append(Long.toString(this.latencyValue / LATENCY1ns)).append("nsec");
        } else if (this.latencyValue < LATENCY1ms) {
            sb.append(Long.toString(this.latencyValue / LATENCY1us)).append("usec");
        } else if (this.latencyValue < LATENCY1s) {
            sb.append(Long.toString(this.latencyValue / LATENCY1ms)).append("msec");
        }

        sb.append("]");
        return sb.toString();
    }

    @Override
    public String getStringValue() {
        if (this.latencyValue == 0) {
            return("UnKnown");
        } else if (this.latencyValue < LATENCY1ns) {
            return(this.latencyValue + "psec");
        } else if (this.latencyValue < LATENCY1us) {
            return(Long.toString(this.latencyValue / LATENCY1ns) + "nsec");
        } else if (this.latencyValue < LATENCY1ms) {
            return(Long.toString(this.latencyValue / LATENCY1us) + "usec");
        } else if (this.latencyValue < LATENCY1s) {
            return(Long.toString(this.latencyValue / LATENCY1ms) + "msec");
        } else {
            return Long.toString(this.latencyValue) + "sec";
        }
    }
}
