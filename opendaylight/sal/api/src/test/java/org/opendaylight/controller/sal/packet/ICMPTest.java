
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
import org.opendaylight.controller.sal.packet.ICMP;

public class ICMPTest {

    @Test
    public void testSetTypeCode() {
        ICMP icmp = new ICMP();
        short icmpTypeCode = 2;
        icmp.setTypeCode(icmpTypeCode);
        byte[] typeCode = icmp.hdrFieldsMap.get("TypeCode");
        Assert.assertTrue(typeCode[0] == 0);
        Assert.assertTrue(typeCode[1] == 2);

    }

    @Test
    public void testSetChecksum() {
        ICMP icmp = new ICMP();
        short icmpChecksum = 200;
        icmp.setChecksum(icmpChecksum);
        byte[] checksum = icmp.hdrFieldsMap.get("HeaderChecksum");
        Assert.assertTrue(checksum[0] == 0);
        Assert.assertTrue(checksum[1] == -56);

    }

    @Test
    public void testSetIdentifier() {
        ICMP icmp = new ICMP();
        short icmpIdentifier = 1201;
        icmp.setIdentifier(icmpIdentifier);
        byte[] identifier = icmp.hdrFieldsMap.get("Identifier");
        Assert.assertTrue(identifier[0] == 4);
        Assert.assertTrue(identifier[1] == -79);

    }

    @Test
    public void testSetSequenceNumber() {
        ICMP icmp = new ICMP();
        short icmpSequenceNumber = 5000;
        icmp.setSequenceNumber(icmpSequenceNumber);
        byte[] sequenceNumber = icmp.hdrFieldsMap.get("SequenceNumber");
        Assert.assertTrue(sequenceNumber[0] == 19);
        Assert.assertTrue(sequenceNumber[1] == -120);

    }
}
