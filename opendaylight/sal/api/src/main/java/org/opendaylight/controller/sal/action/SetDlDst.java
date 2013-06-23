/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.utils.HexEncode;

/**
 * Set destination datalayer address action
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetDlDst extends Action {
    private static final long serialVersionUID = 1L;
    private byte[] address;

    /* Dummy constructor for JAXB */
    @SuppressWarnings("unused")
    private SetDlDst() {
    }

    public SetDlDst(byte[] dlAddress) {
        type = ActionType.SET_DL_DST;
        this.address = dlAddress.clone();
    }

    /**
     * Returns the datalayer address that this action will set
     *
     * @return byte[]
     */
    public byte[] getDlAddress() {
        return address.clone();
    }

    @XmlElement(name = "address")
    public String getDlAddressString() {
        return HexEncode.bytesToHexString(address);
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
        SetDlDst other = (SetDlDst) obj;
        if (!Arrays.equals(address, other.address)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(address);
        return result;
    }

    @Override
    public String toString() {
        return type + "[address = " + HexEncode.bytesToHexString(address) + "]";
    }
}
