package org.openflow.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.queue.OFPacketQueue;
import org.openflow.protocol.queue.OFQueueProperty;
import org.openflow.protocol.queue.OFQueuePropertyMinRate;
import org.openflow.protocol.queue.OFQueuePropertyType;
import org.openflow.util.OFTestCase;

public class OFQueueConfigTest extends OFTestCase {
    public void testRequest() throws Exception {
        OFQueueConfigRequest req = new OFQueueConfigRequest();
        req.setPort((short) 5);
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.clear();
        req.writeTo(bb);
        bb.flip();

        OFQueueConfigRequest req2 = new OFQueueConfigRequest();
        req2.readFrom(bb);
        TestCase.assertEquals(req, req2);
    }

    public void testReply() throws Exception {
        OFQueueConfigReply reply = new OFQueueConfigReply();
        reply.setPort((short) 5);

        OFPacketQueue queue = new OFPacketQueue();
        queue.setQueueId(1);
        List<OFQueueProperty> properties = new ArrayList<OFQueueProperty>();
        properties.add(new OFQueuePropertyMinRate().setRate((short) 1));
        queue.setProperties(properties);
        queue.setLength((short) (OFPacketQueue.MINIMUM_LENGTH + OFQueuePropertyMinRate.MINIMUM_LENGTH));

        List<OFPacketQueue> queues = new ArrayList<OFPacketQueue>();
        queues.add(queue);
        reply.setQueues(queues);
        reply.setLengthU(OFQueueConfigReply.MINIMUM_LENGTH + queue.getLength());

        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.clear();
        reply.writeTo(bb);
        bb.flip();

        OFQueueConfigReply reply2 = new OFQueueConfigReply();
        reply2.setQueuePropertyFactory(new BasicFactory());
        reply2.readFrom(bb);
        TestCase.assertEquals(reply, reply2);
        TestCase.assertEquals(1, reply2.getQueues().size());
        TestCase.assertEquals(1, reply2.getQueues().get(0).getProperties().size());
        TestCase.assertTrue(reply2.getQueues().get(0).getProperties().get(0) instanceof OFQueuePropertyMinRate);
        TestCase.assertEquals(OFQueuePropertyType.MIN_RATE, reply2.getQueues().get(0).getProperties().get(0).getType());

        reply.getQueues().add(queue.clone());
        reply.setLengthU(reply.getLengthU() + queue.getLength());
        bb.clear();
        reply.writeTo(bb);
        bb.flip();
        reply2 = new OFQueueConfigReply();
        reply2.setQueuePropertyFactory(new BasicFactory());
        reply2.readFrom(bb);
        TestCase.assertEquals(reply, reply2);
        TestCase.assertEquals(2, reply2.getQueues().size());

        queue.getProperties().add(new OFQueuePropertyMinRate().setRate((short) 2));
        queue.setLength((short) (queue.getLength() + OFQueuePropertyMinRate.MINIMUM_LENGTH));
        reply.setLengthU(reply.getLengthU() + OFQueuePropertyMinRate.MINIMUM_LENGTH);
        bb.clear();
        reply.writeTo(bb);
        bb.flip();
        reply2 = new OFQueueConfigReply();
        reply2.setQueuePropertyFactory(new BasicFactory());
        reply2.readFrom(bb);
        TestCase.assertEquals(reply, reply2);
        TestCase.assertEquals(2, reply2.getQueues().size());
        TestCase.assertEquals(2, reply2.getQueues().get(0).getProperties().size());
    }
}
