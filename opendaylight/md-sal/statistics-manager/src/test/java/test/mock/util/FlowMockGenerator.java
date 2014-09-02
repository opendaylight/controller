package test.mock.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;

import java.math.BigInteger;
import java.util.Random;

public class FlowMockGenerator {
    private static final Random RND = new Random();
    private static final FlowBuilder FLOW_BUILDER = new FlowBuilder();

    public static Flow getRandomFlow() {
        FLOW_BUILDER.setKey(new FlowKey(new FlowId("flow." + RND.nextInt(1000))));
        FLOW_BUILDER.setOutGroup(Util.nextLong(0, 4294967296L));
        FLOW_BUILDER.setTableId((short) RND.nextInt(256));
        FLOW_BUILDER.setOutPort(BigInteger.valueOf(Util.nextLong(0, Long.MAX_VALUE)));
        FLOW_BUILDER.setStrict(RND.nextBoolean());
        FLOW_BUILDER.setContainerName("container." + RND.nextInt(1000));
        FLOW_BUILDER.setBarrier(RND.nextBoolean());
        FLOW_BUILDER.setMatch(MatchMockGenerator.getRandomMatch());
        FLOW_BUILDER.setPriority(RND.nextInt(65535));
        FLOW_BUILDER.setCookie(new FlowCookie(BigInteger.valueOf(Util.nextLong(0, Long.MAX_VALUE))));
        return FLOW_BUILDER.build();
    }
}
