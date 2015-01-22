/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
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
@Deprecated
public class SetNwTos extends Action {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private int tos;

    /* Dummy constructor for JAXB */
    @SuppressWarnings("unused")
    private SetNwTos() {
    }

    public SetNwTos(int tos) {
        type = ActionType.SET_NW_TOS;
        this.tos = tos;
        checkValue(tos);
    }

    /**
     * Returns the network TOS value which the action will set
     *
     * @return int
     */
    public int getNwTos() {
        return tos;
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
