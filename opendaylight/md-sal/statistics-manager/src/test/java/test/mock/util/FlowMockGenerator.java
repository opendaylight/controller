package test.mock.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;

import java.math.BigInteger;
import java.util.Random;

public class FlowMockGenerator {
    private static final Random rnd = new Random();
    private static final FlowBuilder flowBuilder = new FlowBuilder();

    public static Flow getRandomFlow() {
        flowBuilder.setKey(new FlowKey(new FlowId("flow." + rnd.nextInt(1000))));
        flowBuilder.setOutGroup(TestUtils.nextLong(0, 4294967296L));
        flowBuilder.setTableId((short) rnd.nextInt(256));
        flowBuilder.setOutPort(BigInteger.valueOf(TestUtils.nextLong(0, Long.MAX_VALUE)));
        flowBuilder.setStrict(rnd.nextBoolean());
        flowBuilder.setContainerName("container." + rnd.nextInt(1000));
        flowBuilder.setBarrier(rnd.nextBoolean());
        flowBuilder.setMatch(MatchMockGenerator.getRandomMatch());
        flowBuilder.setPriority(rnd.nextInt(65535));
        flowBuilder.setCookie(new FlowCookie(BigInteger.valueOf(TestUtils.nextLong(0, Long.MAX_VALUE))));
        flowBuilder.setCookieMask(flowBuilder.getCookie());
        return flowBuilder.build();
    }
}
