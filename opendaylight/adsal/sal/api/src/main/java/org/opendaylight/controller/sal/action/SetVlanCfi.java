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
 * Set vlan CFI action
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class SetVlanCfi extends Action {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private int cfi;

    /* Dummy constructor for JAXB */
    @SuppressWarnings("unused")
    private SetVlanCfi() {
    }

    public SetVlanCfi(int cfi) {
        type = ActionType.SET_VLAN_CFI;
        this.cfi = cfi;
        checkValue(cfi);
    }

    /**
     * Returns the 802.1q CFI value that this action will set
     *
     * @return
     */
    public int getCfi() {
        return cfi;
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
        SetVlanCfi other = (SetVlanCfi) obj;
        if (cfi != other.cfi) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + cfi;
        return result;
    }

    @Override
    public String toString() {
        return type + "[cfi = " + Integer.toHexString(cfi) + "]";
    }
}
