package org.openflow.protocol;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.openflow.util.OFTestCase;

public class OFSetConfigTest extends OFTestCase {
    public void testWriteRead() throws Exception {
        OFGetConfigReply msg = (OFGetConfigReply) messageFactory
                .getMessage(OFType.GET_CONFIG_REPLY);
        msg.setFlags((short) 1);
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.clear();
        msg.writeTo(bb);
        bb.flip();
        msg.readFrom(bb);
        TestCase.assertEquals(OFType.GET_CONFIG_REPLY, msg.getType());
        TestCase.assertEquals((short)1, msg.getFlags());
    }
}
