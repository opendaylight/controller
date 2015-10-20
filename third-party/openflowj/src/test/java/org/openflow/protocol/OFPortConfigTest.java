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
