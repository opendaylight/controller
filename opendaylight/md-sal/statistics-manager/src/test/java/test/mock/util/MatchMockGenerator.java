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
    private static final Random RND = new Random();
    private static final MatchBuilder MATCH_BUILDER = new MatchBuilder();
    private static final IpMatchBuilder IP_MATCH_BUILDER = new IpMatchBuilder();
    private static final MetadataBuilder METADATA_BUILDER = new MetadataBuilder();

    public static Match getRandomMatch() {
        MATCH_BUILDER.setInPort(new NodeConnectorId("port." + RND.nextInt(500)));
        IP_MATCH_BUILDER.setIpDscp(new Dscp((short) RND.nextInt(64))).build();
        IP_MATCH_BUILDER.setIpEcn((short) RND.nextInt(256));
        IP_MATCH_BUILDER.setIpProtocol((short) RND.nextInt(256));
        MATCH_BUILDER.setIpMatch(IP_MATCH_BUILDER.build());
        METADATA_BUILDER.setMetadata(BigInteger.valueOf(nextLong(0, Long.MAX_VALUE)));
        METADATA_BUILDER.setMetadataMask(BigInteger.valueOf(nextLong(0, Long.MAX_VALUE)));
        MATCH_BUILDER.setMetadata(METADATA_BUILDER.build());
        return MATCH_BUILDER.build();
    }

    private static long nextLong(long RangeBottom, long rangeTop) {
        return RangeBottom + ((long)(RND.nextDouble()*(rangeTop - RangeBottom)));
    }
}
