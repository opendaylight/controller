package test.mock.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.queue.rev130925.QueueId;

import java.util.Random;

public class QueueMockGenerator {
    private static final Random RND = new Random();
    private static final QueueBuilder QUEUE_BUILDER = new QueueBuilder();

    public static Queue getRandomQueue() {
        QUEUE_BUILDER.setKey(new QueueKey(new QueueId(Util.nextLong(0, 4294967295L))));
        QUEUE_BUILDER.setPort(Util.nextLong(0, 4294967295L));
        QUEUE_BUILDER.setProperty(RND.nextInt(65535));
        return QUEUE_BUILDER.build();
    }

    public static Queue getRandomQueueWithPortNum(long portNum) {
        QUEUE_BUILDER.setKey(new QueueKey(new QueueId(Util.nextLong(0, 4294967295L))));
        QUEUE_BUILDER.setPort(portNum);
        QUEUE_BUILDER.setProperty(RND.nextInt(65535));
        return QUEUE_BUILDER.build();
    }
}
