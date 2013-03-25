package org.openflow.protocol;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.openflow.util.OFTestCase;


public class OFFeaturesReplyTest extends OFTestCase {
    public void testWriteRead() throws Exception {
        OFFeaturesReply ofr = (OFFeaturesReply) messageFactory
                .getMessage(OFType.FEATURES_REPLY);
        List<OFPhysicalPort> ports = new ArrayList<OFPhysicalPort>();
        OFPhysicalPort port = new OFPhysicalPort();
        port.setHardwareAddress(new byte[6]);
        port.setName("eth0");
        ports.add(port);
        ofr.setPorts(ports);
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.clear();
        ofr.writeTo(bb);
        bb.flip();
        ofr.readFrom(bb);
        TestCase.assertEquals(1, ofr.getPorts().size());
        TestCase.assertEquals("eth0", ofr.getPorts().get(0).getName());

        // test a 15 character name
        ofr.getPorts().get(0).setName("012345678901234");
        bb.clear();
        ofr.writeTo(bb);
        bb.flip();
        ofr.readFrom(bb);
        TestCase.assertEquals("012345678901234", ofr.getPorts().get(0).getName());

        // test a 16 character name getting truncated
        ofr.getPorts().get(0).setName("0123456789012345");
        bb.clear();
        ofr.writeTo(bb);
        bb.flip();
        ofr.readFrom(bb);
        TestCase.assertEquals("012345678901234", ofr.getPorts().get(0).getName());
    }
}
