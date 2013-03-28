package org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension;

import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension.V6Match;
import org.openflow.protocol.OFMatch;

public class V6ExtensionTest {

    @Test
    public void testFromString() throws UnknownHostException {
        // This tests creating V6Match using fromString and OFMatch by comparing
        // the results to each other
        V6Match match = new V6Match();
        V6Match match2 = new V6Match();

        OFMatch ofm = new OFMatch();
        V6Match match4 = new V6Match(ofm);

        match.fromString("");
        Assert.assertTrue(match.equals(match2));
        match.fromString("any");
        Assert.assertTrue(match.equals(match2));
        Assert.assertTrue(match.equals(match4));
        try {
            match.fromString("invalidArgument");

            fail("Did not throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // passed test for throwing exception.
        }
        try {
            match.fromString("invalidParameter=abcdefg");
            fail("Did not throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // passed test for throwing exception.
        }

        match.fromString("input_port=1");
        match.fromString("dl_dst=20:A0:11:10:00:99");
        match.fromString("dl_src=00:10:08:22:12:75");

        match.fromString("ip_src=10.1.1.1");
        match.fromString("ip_dst=1.2.3.4");
        match.fromString("eth_type=0x800");
        match.fromString("dl_vlan=10");
        match.fromString("dl_vpcp=1");
        match.fromString("nw_proto=6");
        match.fromString("nw_tos=100");
        match.fromString("tp_dst=8080");
        match.fromString("tp_src=60");

        Assert.assertTrue(match.getInputPort() == 1);
        // Assert.assertTrue(match.getIPv6MatchLen()==6);

        ofm.setInputPort((short) 1);
        // V6Match is meant for IPv6, but if using OFMatch, it will be set to
        // IPv4 values, as OF1.0 doesn't support IPv6.
        InetAddress addr = InetAddress.getByName("10.1.1.1");
        int ipsrc = ByteBuffer.wrap(addr.getAddress()).getInt();
        ofm.setNetworkSource(ipsrc);

        addr = InetAddress.getByName("1.2.3.4");
        int ipdst = ByteBuffer.wrap(addr.getAddress()).getInt();
        ofm.setNetworkDestination(ipdst);

        byte[] macSrc = { 0x00, 0x10, 0x08, 0x22, 0x12, 0x75 };
        ofm.setDataLayerSource(macSrc);
        byte[] macDst = { 0x20, (byte) 0xA0, 0x11, 0x10, 0x00, (byte) 0x99 };
        ofm.setDataLayerDestination(macDst);
        ofm.setDataLayerType((short) 0x800);
        ofm.setDataLayerVirtualLan((short) 10);
        ofm.setDataLayerVirtualLanPriorityCodePoint((byte) 1);
        ofm.setNetworkProtocol((byte) 6);
        ofm.setNetworkTypeOfService((byte) 100);
        ofm.setTransportSource((short) 60);
        ofm.setTransportDestination((short) 8080);

        V6Match match3 = new V6Match(ofm);

        Assert.assertTrue(match.getInputPort() == match3.getInputPort());
        Assert.assertTrue(Arrays.equals(match.getDataLayerSource(),
                match3.getDataLayerSource()));
        Assert.assertTrue(Arrays.equals(match.getDataLayerDestination(),
                match3.getDataLayerDestination()));
        Assert.assertTrue(match.getNetworkSrc().equals(match3.getNetworkSrc()));
        Assert.assertTrue(match.getNetworkDest()
                .equals(match3.getNetworkDest()));
        Assert.assertTrue(match.getDataLayerVirtualLan() == match3
                .getDataLayerVirtualLan());
        Assert.assertTrue(match.getDataLayerVirtualLanPriorityCodePoint() == match3
                .getDataLayerVirtualLanPriorityCodePoint());
        Assert.assertTrue(match.getNetworkProtocol() == match3
                .getNetworkProtocol());
        Assert.assertTrue(match.getNetworkTypeOfService() == match3
                .getNetworkTypeOfService());
        Assert.assertTrue(match.getTransportSource() == match3
                .getTransportSource());
        Assert.assertTrue(match.getTransportDestination() == match3
                .getTransportDestination());
        Assert.assertTrue(match.getWildcards() == match3.getWildcards());

    }

    @Test
    public void testReadWriteBuffer() {
        V6Match match = new V6Match();
        match.fromString("input_port=1");
        match.fromString("dl_dst=20:A0:11:10:00:99");
        match.fromString("dl_src=00:10:08:22:12:75");
        // writeTo(ByteBuffer) will only write IPv6
        match.fromString("ip_src=2001:ddd:3e1:1234:0000:1111:2222:3333/64");
        match.fromString("ip_dst=2001:123:222:abc:111:aaa:1111:2222/64");
        match.fromString("dl_vlan=10");
        match.fromString("nw_proto=6");
        match.fromString("nw_tos=100");
        match.fromString("tp_dst=8080");
        match.fromString("tp_src=60");
        match.fromString("dl_type=0x800");

        ByteBuffer data = ByteBuffer.allocateDirect(10000);
        match.writeTo(data);
        data.flip();
        V6Match match2 = new V6Match();
        match2.readFrom(data);
        Assert.assertTrue(match.getInputPort() == match2.getInputPort());
        Assert.assertTrue(Arrays.equals(match.getDataLayerSource(),
                match2.getDataLayerSource()));
        Assert.assertTrue(Arrays.equals(match.getDataLayerDestination(),
                match2.getDataLayerDestination()));

        Assert.assertTrue(match.getNetworkSrc().equals(match2.getNetworkSrc()));
        Assert.assertTrue(match.getNetworkDest()
                .equals(match2.getNetworkDest()));

        Assert.assertTrue(match.getDataLayerVirtualLan() == match2
                .getDataLayerVirtualLan());
        // vlan pcp isn't part of write/read buffer
        Assert.assertTrue(match.getNetworkProtocol() == match2
                .getNetworkProtocol());
        Assert.assertTrue(match.getNetworkTypeOfService() == match2
                .getNetworkTypeOfService());
        Assert.assertTrue(match.getTransportSource() == match2
                .getTransportSource());
        Assert.assertTrue(match.getTransportDestination() == match2
                .getTransportDestination());

    }

    @Test
    public void testClone() {
        V6Match match = new V6Match();
        match.fromString("input_port=1");
        match.fromString("dl_dst=20:A0:11:10:00:99");
        match.fromString("dl_src=00:10:08:22:12:75");
        match.fromString("ip_src=2001:ddd:3e1:1234:0000:1111:2222:3333/64");
        match.fromString("ip_dst=2001:123:222:abc:111:aaa:1111:2222/64");
        match.fromString("dl_vlan=10");
        match.fromString("dl_vpcp=1");
        match.fromString("nw_proto=6");
        match.fromString("nw_tos=100");
        match.fromString("tp_dst=8080");
        match.fromString("tp_src=60");
        match.fromString("dl_type=0x800");

        V6Match match2 = match.clone();
        Assert.assertTrue(match.getInputPort() == match2.getInputPort());
        Assert.assertTrue(Arrays.equals(match.getDataLayerSource(),
                match2.getDataLayerSource()));
        Assert.assertTrue(Arrays.equals(match.getDataLayerDestination(),
                match2.getDataLayerDestination()));
        Assert.assertTrue(match.getNetworkSrc().equals(match2.getNetworkSrc()));
        Assert.assertTrue(match.getNetworkDest()
                .equals(match2.getNetworkDest()));
        Assert.assertTrue(match.getDataLayerVirtualLan() == match2
                .getDataLayerVirtualLan());
        Assert.assertTrue(match.getDataLayerVirtualLanPriorityCodePoint() == match2
                .getDataLayerVirtualLanPriorityCodePoint());
        Assert.assertTrue(match.getNetworkProtocol() == match2
                .getNetworkProtocol());
        Assert.assertTrue(match.getNetworkTypeOfService() == match2
                .getNetworkTypeOfService());
        Assert.assertTrue(match.getTransportSource() == match2
                .getTransportSource());
        Assert.assertTrue(match.getTransportDestination() == match2
                .getTransportDestination());
        Assert.assertTrue(match.getWildcards() == match2.getWildcards());
    }

    @Test
    public void testPadding() {
        // testing that matchlen+pad keeps the 8byte alignment
        V6Match match = new V6Match();

        match.fromString("input_port=1");
        Assert.assertTrue((match.getPadSize() + match.getIPv6MatchLen()) % 8 == 0);
        match.fromString("dl_dst=20:A0:11:10:00:99");
        match.fromString("dl_src=00:10:08:22:12:75");
        Assert.assertTrue((match.getPadSize() + match.getIPv6MatchLen()) % 8 == 0);
        match.fromString("ip_src=2001:ddd:3e1:1234:0000:1111:2222:3333");
        Assert.assertTrue((match.getPadSize() + match.getIPv6MatchLen()) % 8 == 0);
        match.fromString("ip_dst=2001:123:222:abc:111:aaa:1111:2222");
        Assert.assertTrue((match.getPadSize() + match.getIPv6MatchLen()) % 8 == 0);
        match.fromString("dl_vlan=10");
        match.fromString("dl_vpcp=1");
        match.fromString("nw_proto=6");
        Assert.assertTrue((match.getPadSize() + match.getIPv6MatchLen()) % 8 == 0);
        match.fromString("nw_tos=100");
        match.fromString("tp_dst=8080");
        Assert.assertTrue((match.getPadSize() + match.getIPv6MatchLen()) % 8 == 0);
        match.fromString("tp_src=60");
        Assert.assertTrue((match.getPadSize() + match.getIPv6MatchLen()) % 8 == 0);
    }
}
