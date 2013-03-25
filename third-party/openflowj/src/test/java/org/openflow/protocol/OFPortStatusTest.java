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
