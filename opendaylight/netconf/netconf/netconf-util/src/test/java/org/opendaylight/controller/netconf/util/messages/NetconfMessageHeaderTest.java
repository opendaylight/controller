/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.common.base.Charsets;
import org.junit.Test;

@Deprecated
public class NetconfMessageHeaderTest {
    @Test
    public void testGet() throws Exception {
        NetconfMessageHeader header = new NetconfMessageHeader(10);
        assertEquals(header.getLength(), 10);

        byte[] expectedValue = "\n#10\n".getBytes(Charsets.US_ASCII);
        assertArrayEquals(expectedValue, header.toBytes());
    }
}
