package test.mock.util;


import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.CommonPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.port.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.port.PortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.port.PortKey;

import java.util.Random;

public class PortMockGenerator {
    private static final Random rnd = new Random();
    private static final PortBuilder portBuilder = new PortBuilder();

    public static Port getRandomPort() {
        portBuilder.setKey(new PortKey(TestUtils.nextLong(0, 4294967295L)));
        portBuilder.setBarrier(rnd.nextBoolean());
        portBuilder.setPortNumber(new CommonPort.PortNumber(TestUtils.nextLong(0, 4294967295L)));
        portBuilder.setConfiguration(new PortConfig(rnd.nextBoolean(), rnd.nextBoolean(), rnd.nextBoolean(), rnd.nextBoolean()));
        return portBuilder.build();
    }
}
