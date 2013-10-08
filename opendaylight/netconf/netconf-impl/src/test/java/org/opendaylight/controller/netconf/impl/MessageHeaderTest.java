/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageHeader;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MessageHeaderTest {

    private NetconfMessageHeader header = null;

    @Before
    public void setUp() {
        this.header = new NetconfMessageHeader();
    }

    @Test
    public void testFromBytes() {
        final byte[] raw = new byte[] { (byte) 0x0a, (byte) 0x23, (byte) 0x35, (byte) 0x38, (byte) 0x0a };
        this.header.fromBytes(raw);
        assertEquals(58, this.header.getLength());
    }

    @Test
    public void testToBytes() {
        this.header.setLength(123);
        assertArrayEquals(new byte[] { (byte) 0x0a, (byte) 0x23, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x0a },
                this.header.toBytes());
    }
}
