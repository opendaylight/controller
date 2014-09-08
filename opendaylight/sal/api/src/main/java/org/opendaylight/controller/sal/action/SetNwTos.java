/*
 * Copyright (c) 2013-2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Set network TOS action
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetNwTos extends Action {
    /**
     * Size of ECN field in bits.
     */
    public static final int ECN_FIELD_SIZE = 2;

    private static final long serialVersionUID = 1L;

    @XmlElement
    private int tos;

    /* Dummy constructor for JAXB */
    @SuppressWarnings("unused")
    private SetNwTos() {
    }

    /**
     * Construct a new instance from the given DSCP value.
     *
     * @param dscp  DSCP value.
     */
    public SetNwTos(int dscp) {
        type = ActionType.SET_NW_TOS;
        this.tos = dscp << ECN_FIELD_SIZE;
        checkValue(dscp);
    }

    /**
     * Returns the network TOS value which the action will set
     *
     * @return int
     */
    public int getNwTos() {
        return tos;
    }

    /**
     * Returns DSCP value.
     *
     * @return  DSCP value.
     */
    public int getDscp() {
        return (tos >>> ECN_FIELD_SIZE);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SetNwTos other = (SetNwTos) obj;
        if (tos != other.tos) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + tos;
        return result;
    }

    @Override
    public String toString() {
        return type + "[tos = 0x" + Integer.toHexString(tos) + "]";
    }
}
