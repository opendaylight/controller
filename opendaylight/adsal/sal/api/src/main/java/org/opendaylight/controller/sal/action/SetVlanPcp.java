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
 * Set vlan PCP action
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class SetVlanPcp extends Action {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private int pcp;

    @SuppressWarnings("unused")
    private SetVlanPcp() {

    }

    public SetVlanPcp(int pcp) {
        type = ActionType.SET_VLAN_PCP;
        this.pcp = pcp;
        checkValue(pcp);
    }

    /**
     * Returns the value of the vlan PCP this action will set
     *
     * @return int
     */
    public int getPcp() {
        return pcp;
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
        SetVlanPcp other = (SetVlanPcp) obj;
        if (pcp != other.pcp) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + pcp;
        return result;
    }

    @Override
    public String toString() {
        return type + "[pcp = " + Integer.toHexString(pcp) + "]";
    }
}
