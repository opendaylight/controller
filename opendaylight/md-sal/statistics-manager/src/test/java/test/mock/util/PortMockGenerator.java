package test.mock.util;


import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.CommonPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.port.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.port.PortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.port.PortKey;

import java.util.Random;

public class PortMockGenerator {
    private static final Random RND = new Random();
    private static final PortBuilder PORT_BUILDER = new PortBuilder();

    public static Port getRandomPort() {
        PORT_BUILDER.setKey(new PortKey(Util.nextLong(0, 4294967295L)));
        PORT_BUILDER.setBarrier(RND.nextBoolean());
        PORT_BUILDER.setPortNumber(new CommonPort.PortNumber(Util.nextLong(0, 4294967295L)));
        PORT_BUILDER.setConfiguration(new PortConfig(RND.nextBoolean(), RND.nextBoolean(), RND.nextBoolean(), RND.nextBoolean()));
        return PORT_BUILDER.build();
    }
}
