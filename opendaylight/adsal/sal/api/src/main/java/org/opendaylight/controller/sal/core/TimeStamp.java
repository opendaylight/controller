
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @file   TimeStamp.java
 *
 * @brief  Class representing a TimeStamp
 *
 * A property describing a timestamp based following the rules of
 * java.util.Date, also given the time stamp represent the time when
 * something happened, then a name is attached to this property so
 * to qualify what are we talking about
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class TimeStamp extends Property {
    private static final long serialVersionUID = 1L;
    @XmlElement(name = "value")
    private long timestamp;
    @XmlElement(name = "name")
    private String timestampName;

    public static final String TimeStampPropName = "timeStamp";

    /**
     * Construct a TimeStamp proporty
     *
     * @param timestamp the time stampt we want to describe in "epoch"
     * format following the rules of java.util.Date
     * @param timestampName A qualifier for the timestamp, for example
     * "JoinTime" or any even qualifier could come up
     *
     * @return Constructed object
     */
    public TimeStamp(long timestamp, String timestampName) {
        super(TimeStampPropName);
        this.timestamp = timestamp;
        this.timestampName = timestampName;
    }

    /*
     * Private constructor used for JAXB mapping
     */
    private TimeStamp() {
        super(TimeStampPropName);
        this.timestamp = 0;
        this.timestampName = null;
    }

    @Override
    public TimeStamp clone() {
        return new TimeStamp(this.timestamp, this.timestampName);
    }

    public long getValue() {
        return this.timestamp;
    }

    public String getTimeStampName() {
        return this.timestampName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        result = prime * result
                + ((timestampName == null) ? 0 : timestampName.hashCode());
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
        TimeStamp other = (TimeStamp) obj;
        if (timestamp != other.timestamp)
            return false;
        if (timestampName == null) {
            if (other.timestampName != null)
                return false;
        } else if (!timestampName.equals(other.timestampName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TimeStamp[" + timestampName + ": " + timestamp +"]";
    }

    @Override
    public String getStringValue() {
        return timestampName + ": " + new Date(timestamp);
    }
}
