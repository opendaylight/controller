
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import java.io.Serializable;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;

/**
 * @file   Property.java
 *
 * @brief  Abstract base class for a Property that can be attached to
 * any sal core element
 *
 * Abstract base class for a Property that can be attached to any sal
 * core element
 */

/**
 * Abstract base class for a Property that can be attached to any sal
 * core element
 *
 */
@XmlRootElement
@XmlSeeAlso({ Config.class, Name.class, State.class, TimeStamp.class,
		Latency.class, Bandwidth.class, Tier.class, Actions.class,
		AdvertisedBandwidth.class, Buffers.class, Capabilities.class,
		MacAddress.class, PeerBandwidth.class, SupportedBandwidth.class,
		Tables.class })
abstract public class Property implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;

    /**
     * Private constructor used for JAXB mapping
     */
    private Property() {
        this.name = null;
    }

    protected Property(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Used to copy the Property in a polymorphic way
     *
     *
     * @return A clone of this Property
     */
    abstract public Property clone();

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
        return "Property[" + ReflectionToStringBuilder.toString(this) + "]";
    }
}
