
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
 * The class represents the Tier property of a node
 *
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings("serial")
@Deprecated
public class Tier extends Property {
    @XmlElement(name="value")
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

    @Override
    public Tier clone() {
        return new Tier(this.tierValue);
    }

    public int getValue() {
        return this.tierValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + tierValue;
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
        Tier other = (Tier) obj;
        if (tierValue != other.tierValue)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Tier[" + tierValue + "]";
    }

    @Override
    public String getStringValue() {
        return String.valueOf(tierValue);
    }
}
