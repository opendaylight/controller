package org.openflow.protocol;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.openflow.util.OFTestCase;

public class OFVendorTest extends OFTestCase {
    public void testWriteRead() throws Exception {
        OFVendor msg = (OFVendor) messageFactory.getMessage(OFType.VENDOR);
        msg.setVendor(1);
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.clear();
        msg.writeTo(bb);
        bb.flip();
        msg.readFrom(bb);
        TestCase.assertEquals(1, msg.getVendor());
    }
}
