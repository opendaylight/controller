
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
 *
 * TODO: Datalayer Address should be encapsulated in type.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class SetDlDst  extends AbstractParameterAction<byte[]> {

    public SetDlDst(byte[] dlAddress) {
        super(dlAddress.clone());
    }

    /**
     * Returns the datalayer address that this action will set
     *
     * @return byte[]
     */
    @Deprecated
    public byte[] getDlAddress() {
        return getValue().clone();
    }
    
    // FIXME: Should be moved into RTO
    @Deprecated
    @XmlElement(name = "address")
    public String getDlAddressString() {
        return HexEncode.bytesToHexString(getValue());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        SetDlDst other = (SetDlDst) obj;
        if (!Arrays.equals(getValue(), other.getValue()))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(getValue());
        return result;
    }

    @Override
    public String toString() {
        return "setDlDst" + "[address = " + HexEncode.bytesToHexString(getValue()) + "]";
    }
}
