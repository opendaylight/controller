/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageHeader;

public class MessageHeaderTest {
    @Test
    public void testFromBytes() {
        final byte[] raw = new byte[] { (byte) 0x0a, (byte) 0x23, (byte) 0x35, (byte) 0x38, (byte) 0x0a };
        NetconfMessageHeader header = NetconfMessageHeader.fromBytes(raw);
        assertEquals(58, header.getLength());
    }

    @Test
    public void testToBytes() {
        NetconfMessageHeader header = new NetconfMessageHeader(123);
        assertArrayEquals(new byte[] { (byte) 0x0a, (byte) 0x23, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x0a },
                header.toBytes());
    }
}
