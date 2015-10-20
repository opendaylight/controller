package org.openflow.protocol;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.openflow.util.OFTestCase;

public class OFGetConfigReplyTest extends OFTestCase {
    public void testWriteRead() throws Exception {
        OFSetConfig msg = (OFSetConfig) messageFactory
                .getMessage(OFType.SET_CONFIG);
        msg.setFlags((short) 1);
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.clear();
        msg.writeTo(bb);
        bb.flip();
        msg.readFrom(bb);
        TestCase.assertEquals(OFType.SET_CONFIG, msg.getType());
        TestCase.assertEquals((short)1, msg.getFlags());
    }
}
