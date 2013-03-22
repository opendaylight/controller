
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   IDataPacketService.java
 *
 * @brief  SAL exported Data Packet services
 *
 * Data Packet Services SAL provides to the components
 */
package org.opendaylight.controller.sal.packet;

/**
 * Data Packet Services SAL provides to the components
 */
public interface IDataPacketService {
    /**
     * Transmit a data Packet. Transmission will ONLY happen if the
     * RawPacket has the OutgoingNodeConnector set else of course
     * transmission cannot happen.
     *
     * @param outPkt Packet to be transmitted out
     */
    void transmitDataPacket(RawPacket outPkt);

    /**
     * Decode a Data Packet received as a raw stream
     *
     * @param pkt Raw Packet to be decoded
     *
     * @return The formatted Data Packet
     */
    Packet decodeDataPacket(RawPacket pkt);

    /**
     * Encode a Formatted Data Packet in a raw bytestream
     *
     * @param pkt Formatted Data Packet to be encoded
     *
     * @return a RawPacket representation, good for transmission purpose
     */
    RawPacket encodeDataPacket(Packet pkt);
}
