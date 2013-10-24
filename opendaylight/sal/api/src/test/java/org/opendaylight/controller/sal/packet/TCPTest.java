
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

public class TCPTest {

    @Test
    public void testSetSourcePort() {
        TCP tcp = new TCP();
        short tcpSourcePort = 118;
        tcp.setSourcePort(tcpSourcePort);
        byte[] sourcePort = tcp.hdrFieldsMap.get(TCP.SRCPORT);
        Assert.assertTrue(sourcePort[0] == 0);
        Assert.assertTrue(sourcePort[1] == 118);

    }

    @Test
    public void testSetDestinationPort() {
        TCP tcp = new TCP();
        short tcpDestinationPort = 443;
        tcp.setDestinationPort(tcpDestinationPort);
        byte[] destinationPort = tcp.hdrFieldsMap.get(TCP.DESTPORT);
        Assert.assertTrue(destinationPort[0] == 1);
        Assert.assertTrue(destinationPort[1] == -69);

    }

    @Test
    public void testSetSequenceNumber() {
        TCP tcp = new TCP();
        short tcpSequenceNumber = 700;
        tcp.setSequenceNumber(tcpSequenceNumber);
        byte[] sequenceNumber = tcp.hdrFieldsMap.get(TCP.SEQNUMBER);
        Assert.assertTrue(sequenceNumber[0] == 0);
        Assert.assertTrue(sequenceNumber[1] == 0);
        Assert.assertTrue(sequenceNumber[2] == 2);
        Assert.assertTrue(sequenceNumber[3] == -68);
    }

    @Test
    public void testSetAckNumber() {
        TCP tcp = new TCP();
        short tcpAckNumber = 697;
        tcp.setAckNumber(tcpAckNumber);
        byte[] ackNumber = tcp.hdrFieldsMap.get(TCP.ACKNUMBER);
        Assert.assertTrue(ackNumber[0] == 0);
        Assert.assertTrue(ackNumber[1] == 0);
        Assert.assertTrue(ackNumber[2] == 2);
        Assert.assertTrue(ackNumber[3] == -71);
    }

    @Test
    public void testSetHeaderLenFlags() {
        TCP tcp = new TCP();
        short tcpFlags = 26;
        tcp.setHeaderLenFlags(tcpFlags);
        byte[] headerLenFlags = tcp.hdrFieldsMap.get(TCP.HEADERLENFLAGS);
        Assert.assertTrue(headerLenFlags[0] == 0);
        Assert.assertTrue(headerLenFlags[1] == 26);

    }

    @Test
    public void testSetWindowSize() {
        TCP tcp = new TCP();
        short tcpWindowSize = 100;
        tcp.setWindowSize(tcpWindowSize);
        byte[] windowSize = tcp.hdrFieldsMap.get(TCP.WINDOWSIZE);
        Assert.assertTrue(windowSize[0] == 0);
        Assert.assertTrue(windowSize[1] == 100);

    }

    @Test
    public void testSetChecksum() {
        TCP tcp = new TCP();
        short tcpChecksum = 134;
        tcp.setChecksum(tcpChecksum);
        byte[] checksum = tcp.hdrFieldsMap.get(TCP.CHECKSUM);
        Assert.assertTrue(checksum[0] == 0);
        Assert.assertTrue(checksum[1] == -122);

    }

    @Test
    public void testSetUrgentPointer() {
        TCP tcp = new TCP();
        short tcpUrgentPointer = 25098;
        tcp.setUrgentPointer(tcpUrgentPointer);
        byte[] urgentPointer = tcp.hdrFieldsMap.get(TCP.URGENTPOINTER);
        Assert.assertTrue(urgentPointer[0] == 98);
        Assert.assertTrue(urgentPointer[1] == 10);

    }
}
