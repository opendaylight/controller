
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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.utils.HexEncode;

/**
 * Set destination datalayer address action
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class SetDlDst extends Action {
    private byte[] address;

    /* Dummy constructor for JAXB */
    private SetDlDst () {
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
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return type + "[address = " + HexEncode.bytesToHexString(address) + "]";
    }
}
