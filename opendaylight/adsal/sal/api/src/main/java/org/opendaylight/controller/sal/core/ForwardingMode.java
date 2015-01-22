/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
 * The class represents the forwarding mode property of a node.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings("serial")
@Deprecated
public class ForwardingMode extends Property {
    @XmlElement(name="value")
    private final int modeValue;
    public static final int REACTIVE_FORWARDING = 0;
    public static final int PROACTIVE_FORWARDING = 1;
    public static final String name = "forwarding";

    /*
     * Private constructor used for JAXB mapping
     */
    private ForwardingMode() {
        super(name);
        this.modeValue = REACTIVE_FORWARDING;
    }

    public ForwardingMode(int mode) {
        super(name);
        this.modeValue = mode;
    }

    public int getValue() {
        return this.modeValue;
    }

    public boolean isProactive() {
        return (modeValue == ForwardingMode.PROACTIVE_FORWARDING);
    }

    public boolean isValid() {
        return ((modeValue >= 0) && (modeValue <= 1));
    }

    @Override
    public ForwardingMode clone() {
        return new ForwardingMode(this.modeValue);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + modeValue;
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
        ForwardingMode other = (ForwardingMode) obj;
        if (modeValue != other.modeValue)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Mode[" + modeValue + "]";
    }

    @Override
    public String getStringValue() {
        return (modeValue == ForwardingMode.PROACTIVE_FORWARDING) ? "Proactive" : "Reactive";
    }
}
