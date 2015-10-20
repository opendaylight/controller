package org.openflow.protocol;

import java.nio.ByteBuffer;
import java.util.List;

import junit.framework.TestCase;

import org.openflow.protocol.OFError.OFErrorType;
import org.openflow.protocol.OFError.OFHelloFailedCode;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.OFMessageFactory;
import org.openflow.util.OFTestCase;

public class OFErrorTest extends OFTestCase {
    public void testWriteRead() throws Exception {
        OFError msg = (OFError) messageFactory.getMessage(OFType.ERROR);
        msg.setMessageFactory(messageFactory);
        msg.setErrorType((short) OFErrorType.OFPET_HELLO_FAILED.ordinal());
        msg.setErrorCode((short) OFHelloFailedCode.OFPHFC_INCOMPATIBLE
                .ordinal());
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.clear();
        msg.writeTo(bb);
        bb.flip();
        msg.readFrom(bb);
        TestCase.assertEquals((short) OFErrorType.OFPET_HELLO_FAILED.ordinal(),
                msg.getErrorType());
        TestCase.assertEquals((short) OFHelloFailedCode.OFPHFC_INCOMPATIBLE
                .ordinal(), msg.getErrorType());
        TestCase.assertNull(msg.getOffendingMsg());

        msg.setOffendingMsg(new OFHello());
        bb.clear();
        msg.writeTo(bb);
        bb.flip();
        msg.readFrom(bb);
        TestCase.assertEquals((short) OFErrorType.OFPET_HELLO_FAILED.ordinal(),
                msg.getErrorType());
        TestCase.assertEquals((short) OFHelloFailedCode.OFPHFC_INCOMPATIBLE
                .ordinal(), msg.getErrorType());
        TestCase.assertNotNull(msg.getOffendingMsg());
        TestCase.assertEquals(OFHello.MINIMUM_LENGTH,
                msg.getOffendingMsg().length);
    }

    public void testGarbageAtEnd() {
        // This is a OFError msg (12 bytes), that encaps a OFVendor msg (24
        // bytes)
        // AND some zeros at the end (40 bytes) for a total of 76 bytes
        // THIS is what an NEC sends in reply to Nox's VENDOR request
        byte[] oferrorRaw = { 0x01, 0x01, 0x00, 0x4c, 0x00, 0x00, 0x10,
                (byte) 0xcc, 0x00, 0x01, 0x00, 0x01, 0x01, 0x04, 0x00, 0x18,
                0x00, 0x00, 0x10, (byte) 0xcc, 0x00, 0x00, 0x23, 0x20, 0x00,
                0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00 };
        OFMessageFactory factory = new BasicFactory();
        ByteBuffer oferrorBuf = ByteBuffer.wrap(oferrorRaw);
        List<OFMessage> msgs = factory.parseMessages(oferrorBuf,
                oferrorRaw.length);
        TestCase.assertEquals(1, msgs.size());
        OFMessage msg = msgs.get(0);
        TestCase.assertEquals(76, msg.getLengthU());
        ByteBuffer out = ByteBuffer.allocate(1024);
        msg.writeTo(out);
        TestCase.assertEquals(76, out.position());
    }
}
