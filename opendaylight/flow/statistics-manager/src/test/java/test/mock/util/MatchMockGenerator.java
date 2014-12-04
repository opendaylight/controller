package test.mock.util;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder;

import java.math.BigInteger;
import java.util.Random;

public class MatchMockGenerator {
    private static final Random rnd = new Random();
    private static final MatchBuilder matchBuilder = new MatchBuilder();
    private static final IpMatchBuilder ipMatchBuilder = new IpMatchBuilder();
    private static final MetadataBuilder metadataBuilder = new MetadataBuilder();

    public static Match getRandomMatch() {
        matchBuilder.setInPort(new NodeConnectorId("port." + rnd.nextInt(500)));
        ipMatchBuilder.setIpDscp(new Dscp((short) rnd.nextInt(64))).build();
        ipMatchBuilder.setIpEcn((short) rnd.nextInt(256));
        ipMatchBuilder.setIpProtocol((short) rnd.nextInt(256));
        matchBuilder.setIpMatch(ipMatchBuilder.build());
        metadataBuilder.setMetadata(BigInteger.valueOf(TestUtils.nextLong(0, Long.MAX_VALUE)));
        metadataBuilder.setMetadataMask(BigInteger.valueOf(TestUtils.nextLong(0, Long.MAX_VALUE)));
        matchBuilder.setMetadata(metadataBuilder.build());
        return matchBuilder.build();
    }
}
