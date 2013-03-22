
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.packet;

import junit.framework.Assert;

import org.junit.Test;
import org.opendaylight.controller.sal.packet.UDP;

public class UDPTest {

    @Test
    public void testGetSourcePort() {
        UDP udp = new UDP();
        byte[] udpSourcePort = { 0, 118 };
        udp.hdrFieldsMap.put("SourcePort", udpSourcePort);
        short sourcePort = udp.getSourcePort();
        Assert.assertTrue(sourcePort == 118);
    }

    @Test
    public void testGetDestinationPort() {
        UDP udp = new UDP();
        byte[] udpDestinationPort = { 1, -69 };
        udp.hdrFieldsMap.put("DestinationPort", udpDestinationPort);
        short destinationPort = udp.getDestinationPort();
        Assert.assertTrue(destinationPort == 443);
    }

    @Test
    public void testGetLength() {
        UDP udp = new UDP();
        byte[] udpLength = { 0, 20 };
        udp.hdrFieldsMap.put("Length", udpLength);
        short length = udp.getLength();
        Assert.assertTrue(length == 20);
    }

    @Test
    public void testGetChecksum() {
        UDP udp = new UDP();
        byte[] udpChecksum = { 0, -56 };
        udp.hdrFieldsMap.put("Checksum", udpChecksum);
        short checksum = udp.getChecksum();
        Assert.assertTrue(checksum == 200);
    }

    @Test
    public void testSetSourcePort() {
        UDP udp = new UDP();
        short tcpSourcePort = 118;
        udp.setSourcePort(tcpSourcePort);
        byte[] sourcePort = udp.hdrFieldsMap.get("SourcePort");
        Assert.assertTrue(sourcePort[0] == 0);
        Assert.assertTrue(sourcePort[1] == 118);

    }

    @Test
    public void testSetDestinationPort() {
        UDP udp = new UDP();
        short tcpDestinationPort = 443;
        udp.setDestinationPort(tcpDestinationPort);
        byte[] destinationPort = udp.hdrFieldsMap.get("DestinationPort");
        Assert.assertTrue(destinationPort[0] == 1);
        Assert.assertTrue(destinationPort[1] == -69);

    }

    @Test
    public void testSetLength() {
        UDP udp = new UDP();
        short udpLength = 20;
        udp.setLength(udpLength);
        byte[] length = udp.hdrFieldsMap.get("Length");
        Assert.assertTrue(length[0] == 0);
        Assert.assertTrue(length[1] == 20);

    }

    @Test
    public void testSetChecksum() {
        UDP udp = new UDP();
        short udpChecksum = 200;
        udp.setChecksum(udpChecksum);
        byte[] checksum = udp.hdrFieldsMap.get("Checksum");
        Assert.assertTrue(checksum[0] == 0);
        Assert.assertTrue(checksum[1] == -56);

    }

}
