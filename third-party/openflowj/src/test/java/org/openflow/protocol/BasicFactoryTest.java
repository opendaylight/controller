package org.openflow.protocol;

import java.nio.ByteBuffer;
import java.util.List;

import org.openflow.protocol.factory.BasicFactory;
import org.openflow.util.U16;

import junit.framework.TestCase;



public class BasicFactoryTest extends TestCase {
    public void testCreateAndParse() {
        BasicFactory factory = new BasicFactory();
        OFMessage m = factory.getMessage(OFType.HELLO);
        m.setVersion((byte) 1);
        m.setType(OFType.ECHO_REQUEST);
        m.setLength(U16.t(8));
        m.setXid(0xdeadbeef);
        ByteBuffer bb = ByteBuffer.allocate(1024);
        m.writeTo(bb);
        bb.flip();
        bb.limit(bb.limit()-1);
        TestCase.assertEquals(0, factory.parseMessages(bb).size());
        bb.limit(bb.limit()+1);
        List<OFMessage> messages = factory.parseMessages(bb);
        TestCase.assertEquals(1, messages.size());
        TestCase.assertTrue(messages.get(0).getType() == OFType.ECHO_REQUEST);
    }
}
