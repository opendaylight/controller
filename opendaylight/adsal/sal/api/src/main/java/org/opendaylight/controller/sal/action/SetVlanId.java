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
 * Set vlan id action
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class SetVlanId extends Action {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private int vlanId;

    @SuppressWarnings("unused")
    private SetVlanId() {

    }

    public SetVlanId(int vlanId) {
        type = ActionType.SET_VLAN_ID;
        this.vlanId = vlanId;
        checkValue(vlanId);
    }

    /**
     * Returns the vlan id this action will set
     *
     * @return int
     */
    public int getVlanId() {
        return vlanId;
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
        SetVlanId other = (SetVlanId) obj;
        if (vlanId != other.vlanId) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + vlanId;
        return result;
    }

    @Override
    public String toString() {
        return type + "[vlanId = " + vlanId + "]";
    }
}
