
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

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * @file   Latency.java
 *
 * @brief  Class representing Latency
 *
 * Describe a latency in picoseconds or multiple of its.
 */
@XmlRootElement
public class Latency extends Property {
    private static final long serialVersionUID = 1L;
    private long latency;

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
        this.latency = LATENCYUNK;
    }

    public Latency(long latency) {
        super(LatencyPropName);
        this.latency = latency;
    }

    public Latency(int latency) {
        super(LatencyPropName);
        this.latency = (long) latency;
    }

    public Latency(short latency) {
        super(LatencyPropName);
        this.latency = (long) latency;
    }

    public Latency clone() {
        return new Latency(this.latency);
    }

    public long getValue() {
        return this.latency;
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

        sb.append("Latency[");
        if (this.latency == 0) {
            sb.append("UnKnown");
        } else if (this.latency < LATENCY1ns) {
            sb.append(this.latency + "psec");
        } else if (this.latency < LATENCY1us) {
            sb.append(Long.toString(this.latency / LATENCY1ns) + "nsec");
        } else if (this.latency < LATENCY1ms) {
            sb.append(Long.toString(this.latency / LATENCY1us) + "usec");
        } else if (this.latency < LATENCY1s) {
            sb.append(Long.toString(this.latency / LATENCY1ms) + "msec");
        }

        sb.append("]");
        return sb.toString();
    }
}
