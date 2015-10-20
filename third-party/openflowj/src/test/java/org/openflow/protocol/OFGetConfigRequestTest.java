package org.openflow.protocol;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.openflow.util.OFTestCase;

public class OFGetConfigRequestTest extends OFTestCase {
    public void testWriteRead() throws Exception {
        OFGetConfigRequest msg = (OFGetConfigRequest) messageFactory
                .getMessage(OFType.GET_CONFIG_REQUEST);
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.clear();
        msg.writeTo(bb);
        bb.flip();
        msg.readFrom(bb);
        TestCase.assertEquals(OFType.GET_CONFIG_REQUEST, msg.getType());
    }
}
