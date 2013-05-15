/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.action;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.opendaylight.controller.sal.utils.HexEncode;

/**
 * Set source datalayer address action
 * 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetDlSrc extends ActionRTO {
    
    @XmlTransient
    private byte[] dlAddress;

    @SuppressWarnings("unused")
    private SetDlSrc() {
    }
    
    
    public SetDlSrc(byte[] dlAddress) {
        this.dlAddress = dlAddress.clone();
    }

    /**
     * Returns the datalayer address that this action will set
     * 
     * @return byte[]
     */

    public byte[] getDlAddress() {
        return dlAddress;
    }


    @XmlElement(name = "address")
    public String getDlAddressString() {
        return HexEncode.bytesToHexString(dlAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        SetDlSrc other = (SetDlSrc) obj;
        if (!Arrays.equals(dlAddress, other.getDlAddress()))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(dlAddress);
        return result;
    }

    @Override
    public String toString() {
        return "setDlSrc" + "[address = "
                + HexEncode.bytesToHexString(dlAddress) + "]";
    }
}
