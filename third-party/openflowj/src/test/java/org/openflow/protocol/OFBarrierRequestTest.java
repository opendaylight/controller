package org.openflow.protocol;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.openflow.util.OFTestCase;

public class OFBarrierRequestTest extends OFTestCase {
    public void testWriteRead() throws Exception {
        OFBarrierRequest msg = (OFBarrierRequest) messageFactory
                .getMessage(OFType.BARRIER_REQUEST);
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.clear();
        msg.writeTo(bb);
        bb.flip();
        msg.readFrom(bb);
        TestCase.assertEquals(OFType.BARRIER_REQUEST, msg.getType());
    }
}
