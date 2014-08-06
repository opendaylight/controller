package test.mock.util;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.port.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;

import java.math.BigInteger;
import java.util.Random;

public class TestUtils {
    private static Random rnd = new Random();

    public static final Counter64 COUNTER_64_TEST_VALUE = new Counter64(BigInteger.valueOf(128));
    public static final Counter32 COUNTER_32_TEST_VALUE = new Counter32(64L);
    public static final BigInteger BIG_INTEGER_TEST_VALUE = BigInteger.valueOf(1000);

    private static Flow flow = FlowMockGenerator.getRandomFlow();
    private static Group group = GroupMockGenerator.getRandomGroup();
    private static Meter meter = MeterMockGenerator.getRandomMeter();
    private static Port port = PortMockGenerator.getRandomPort();
    private static Queue queue = QueueMockGenerator.getRandomQueueWithPortNum(port.getPortNumber().getUint32());
    private static TableId tableId = new TableId((short) 2);
    private static NodeConnectorId nodeConnectorId = new NodeConnectorId("connector.1");

    public static Flow getFlow() {
        return flow;
    }

    public static Group getGroup() {
        return group;
    }

    public static Meter getMeter() {
        return meter;
    }

    public static Port getPort() {
        return port;
    }

    public static Queue getQueue() {
        return queue;
    }

    public static TableId getTableId() {
        return tableId;
    }

    public static NodeConnectorId getNodeConnectorId() {
        return nodeConnectorId;
    }

    public static long nextLong(long RangeBottom, long rangeTop) {
        return RangeBottom + ((long)(rnd.nextDouble()*(rangeTop - RangeBottom)));
    }
}
