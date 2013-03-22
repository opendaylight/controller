
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.openflow.protocol;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.openflow.util.OFTestCase;

public class OFPortConfigTest extends OFTestCase {
    public void testWriteRead() throws Exception {
        OFPortMod msg = (OFPortMod) messageFactory
                .getMessage(OFType.PORT_MOD);
        msg.setHardwareAddress(new byte[6]);
        msg.portNumber = 1;
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.clear();
        msg.writeTo(bb);
        bb.flip();
        msg.readFrom(bb);
        TestCase.assertEquals(OFType.PORT_MOD, msg.getType());
        TestCase.assertEquals(1, msg.getPortNumber());
    }
}
