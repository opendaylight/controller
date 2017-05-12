/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.liblldp;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class EthernetAddress extends DataLinkAddress {
    private static final long serialVersionUID = 1L;
    @XmlTransient
    private byte[] macAddress;

    public static final EthernetAddress BROADCASTMAC = createWellKnownAddress(new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff });

    public static final EthernetAddress INVALIDHOST = BROADCASTMAC;

    public static final String addressName = "Ethernet MAC Address";
    public static final int SIZE = 6;

    private static final EthernetAddress createWellKnownAddress(final byte[] mac) {
        try {
            return new EthernetAddress(mac);
        } catch (final ConstructionException ce) {
            return null;
        }
    }

    /* Private constructor to satisfy JAXB */
    @SuppressWarnings("unused")
    private EthernetAddress() {
    }

    /**
     * Public constructor for an Ethernet MAC address starting from
     * the byte constituing the address, the constructor validate the
     * size of the arrive to make sure it met the expected size
     *
     * @param macAddress A byte array in big endian format
     * representing the Ethernet MAC Address
     */
    public EthernetAddress(final byte[] macAddress) throws ConstructionException {
        super(addressName);

        if (macAddress == null) {
            throw new ConstructionException("Null input parameter passed");
        }

        if (macAddress.length != SIZE) {
            throw new ConstructionException(
                    "Wrong size of passed byte array, expected:" + SIZE
                            + " got:" + macAddress.length);
        }
        this.macAddress = new byte[SIZE];
        System.arraycopy(macAddress, 0, this.macAddress, 0, SIZE);
    }

    public EthernetAddress clone() {
        try {
            return new EthernetAddress(this.macAddress.clone());
        } catch (final ConstructionException ce) {
            return null;
        }
    }

    /**
     * Return the Ethernet Mac address in byte array format
     *
     * @return The Ethernet Mac address in byte array format
     */
    public byte[] getValue() {
        return this.macAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(macAddress);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EthernetAddress other = (EthernetAddress) obj;
        if (!Arrays.equals(macAddress, other.macAddress)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "EthernetAddress [macAddress=" + HexEncode.bytesToHexStringFormat(macAddress)
                + "]";
    }

    @XmlElement(name = "macAddress")
    public String getMacAddress() {
        return HexEncode.bytesToHexStringFormat(macAddress);
    }
}
