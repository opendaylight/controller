package org.openflow.protocol;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.openflow.util.OFTestCase;

public class OFBarrierReplyTest extends OFTestCase {
    public void testWriteRead() throws Exception {
        OFBarrierReply msg = (OFBarrierReply) messageFactory
                .getMessage(OFType.BARRIER_REPLY);
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.clear();
        msg.writeTo(bb);
        bb.flip();
        msg.readFrom(bb);
        TestCase.assertEquals(OFType.BARRIER_REPLY, msg.getType());
    }
}
