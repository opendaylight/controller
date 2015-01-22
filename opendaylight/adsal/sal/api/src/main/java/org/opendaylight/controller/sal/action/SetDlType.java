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

import org.opendaylight.controller.sal.utils.EtherTypes;

/**
 * Set ethertype/length field action
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class SetDlType extends Action {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private int dlType;

    /* Dummy constructor for JAXB */
    @SuppressWarnings("unused")
    private SetDlType() {
    }

    public SetDlType(int dlType) {
        type = ActionType.SET_DL_TYPE;
        this.dlType = dlType;
        checkValue(dlType);
    }

    public SetDlType(EtherTypes dlType) {
        type = ActionType.SET_DL_TYPE;
        this.dlType = dlType.intValue();
        checkValue(this.dlType);
    }

    /**
     * Returns the ethertype/lenght value that this action will set
     *
     * @return byte[]
     */
    public int getDlType() {
        return dlType;
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
        SetDlType other = (SetDlType) obj;
        if (dlType != other.dlType) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + dlType;
        return result;
    }

    @Override
    public String toString() {
        return type + "[dlType = 0x" + Integer.toHexString(dlType) + "]";
    }
}
