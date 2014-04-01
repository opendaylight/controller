/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;

/**
 * Set destination datalayer address action
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetDlDst extends Action {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "SET_DL_DST";
    public static final Pattern PATTERN = Pattern.compile(NAME + "=(.*)", Pattern.CASE_INSENSITIVE);
    private byte[] address;

    public SetDlDst() {
        super(NAME);
    }

    public SetDlDst(byte[] dlAddress) {
        super(NAME);
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
        return HexEncode.bytesToHexStringFormat(address);
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
        return NAME + "=" + HexEncode.bytesToHexStringFormat(address);
    }

    @Override
    public SetDlDst fromString(String actionString, Node node) {
        Matcher matcher = PATTERN.matcher(removeSpaces(actionString));
        if (matcher.matches()) {
            return new SetDlDst(HexEncode.bytesFromHexString(matcher.group(1)));
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return address != null && address.length == NetUtils.MACAddrLengthInBytes;
    }
}
