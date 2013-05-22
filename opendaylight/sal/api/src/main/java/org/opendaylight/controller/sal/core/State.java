
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

/**
 * The class represents the State property of an Edge
 *
 *
 */
@XmlRootElement
@SuppressWarnings("serial")
public class State extends Property {
    @XmlElement
    private short stateValue;

    public static final short EDGE_DOWN = 0;
    public static final short EDGE_UP = 1;
    public static final short EDGE_UNK = 0x7fff;
    public static final String StatePropName = "state";

    /*
     * Private constructor used for JAXB mapping
     */
    private State() {
        super(StatePropName);
        this.stateValue = EDGE_UNK;
    }

    public State(short state) {
        super(StatePropName);
        this.stateValue = state;
    }

    public State clone() {
        return new State(this.stateValue);
    }

    public short getValue() {
        return this.stateValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + stateValue;
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
        State other = (State) obj;
        if (stateValue != other.stateValue)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "State[" + stateValue + "]";
    }
}
