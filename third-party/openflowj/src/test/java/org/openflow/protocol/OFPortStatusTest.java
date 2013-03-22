
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

import org.openflow.protocol.OFPortStatus.OFPortReason;
import org.openflow.util.OFTestCase;

public class OFPortStatusTest extends OFTestCase {
    public void testWriteRead() throws Exception {
        OFPortStatus msg = (OFPortStatus) messageFactory
                .getMessage(OFType.PORT_STATUS);
        msg.setDesc(new OFPhysicalPort());
        msg.getDesc().setHardwareAddress(new byte[6]);
        msg.getDesc().setName("eth0");
        msg.setReason((byte) OFPortReason.OFPPR_ADD.ordinal());
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.clear();
        msg.writeTo(bb);
        bb.flip();
        msg.readFrom(bb);
        TestCase.assertEquals(OFType.PORT_STATUS, msg.getType());
        TestCase.assertEquals((byte) OFPortReason.OFPPR_ADD.ordinal(), msg
                .getReason());
        TestCase.assertNotNull(msg.getDesc());
    }
}
