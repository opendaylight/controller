
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

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * The class represents the Tier property of a node
 *
 *
 */
@XmlRootElement
@SuppressWarnings("serial")
public class Tier extends Property {
    @XmlElement
    private int tierValue;
    public static final String TierPropName = "tier";

    public Tier(int tier) {
        super(TierPropName);
        this.tierValue = tier;
    }

    /*
     * Private constructor used for JAXB mapping
     */
    private Tier() {
        super(TierPropName);
        this.tierValue = 0;
    }

    public Tier clone() {
        return new Tier(this.tierValue);
    }

    public int getValue() {
        return this.tierValue;
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
        return "Tier[" + ReflectionToStringBuilder.toString(this) + "]";
    }
}
