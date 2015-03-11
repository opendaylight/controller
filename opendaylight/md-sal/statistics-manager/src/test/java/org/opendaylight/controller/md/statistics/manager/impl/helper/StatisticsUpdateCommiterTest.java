/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl.helper;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class StatisticsUpdateCommiterTest {

    private static final Logger LOG = LoggerFactory
            .getLogger(StatisticsUpdateCommiterTest.class);

    /**
     * Test method for {@link org.opendaylight.controller.md.statistics.manager.StatisticsListener#layer3MatchEquals(org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match, org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match)}.
     */
    @Test
    public void testLayer3MatchEquals() {
        final String[][][] matchSeeds = new String[][][] {
                {{"10.1.2.0/24", "10.1.2.0/24"}, {"10.1.2.0/24", "10.1.2.0/24"}},
                {{"10.1.2.0/24", "10.1.2.0/24"}, {"10.1.2.0/24", "10.1.1.0/24"}},
                {{"10.1.1.0/24", "10.1.2.0/24"}, {"10.1.2.0/24", "10.1.2.0/24"}},
                {{"10.1.1.0/24", "10.1.1.0/24"}, {"10.1.2.0/24", "10.1.2.0/24"}},

                {{"10.1.1.0/24", null}, {"10.1.1.0/24", "10.1.2.0/24"}},
                {{"10.1.1.0/24", null}, {"10.1.2.0/24", "10.1.2.0/24"}},
                {{"10.1.1.0/24", null}, {"10.1.2.0/24", null}},
                {{"10.1.1.0/24", null}, {"10.1.1.0/24", null}},

                {{null, "10.1.1.0/24"}, {"10.1.2.0/24", "10.1.1.0/24"}},
                {{null, "10.1.1.0/24"}, {"10.1.2.0/24", "10.1.2.0/24"}},
                {{null, "10.1.1.0/24"}, {null, "10.1.2.0/24"}},
                {{null, "10.1.1.0/24"}, {null, "10.1.1.0/24"}},

                {{null, null}, {null, "10.1.1.0/24"}},
                {{null, null}, {null, null}},
        };

        final String[][][] matchSeedsIpv6 = new String[][][] {
                {{"aabb:1234:2acf:e9ff::fe21:6431/64", "aabb:1234:2acf:e9ff::fe21:6431/64"}, {"aabb:1234:2acf:e9ff::fe21:6431/64", "aabb:1234:2acf:e9ff::fe21:6431/64"}},
                {{"aabb:1234:2acf:e9ff::fe21:6431/64", "aabb:1234:2acf:e9ff::fe21:6431/64"}, {"aabb:1234:2acf:e9ff::fe21:6431/64", "fe80::2acf:e9ff:fe21:6431/128"}},
                {{"fe80::2acf:e9ff:fe21:6431/128", "aabb:1234:2acf:e9ff::fe21:6431/64"}, {"aabb:1234:2acf:e9ff::fe21:6431/64", "aabb:1234:2acf:e9ff::fe21:6431/64"}},
                {{"fe80::2acf:e9ff:fe21:6431/128", "fe80::2acf:e9ff:fe21:6431/128"}, {"aabb:1234:2acf:e9ff::fe21:6431/64", "aabb:1234:2acf:e9ff::fe21:6431/64"}},

                {{"fe80::2acf:e9ff:fe21:6431/128", null}, {"fe80::2acf:e9ff:fe21:6431/128", "aabb:1234:2acf:e9ff::fe21:6431/64"}},
                {{"fe80::2acf:e9ff:fe21:6431/128", null}, {"aabb:1234:2acf:e9ff::fe21:6431/64", "aabb:1234:2acf:e9ff::fe21:6431/64"}},
                {{"fe80::2acf:e9ff:fe21:6431/128", null}, {"aabb:1234:2acf:e9ff::fe21:6431/64", null}},
                {{"fe80::2acf:e9ff:fe21:6431/128", null}, {"fe80::2acf:e9ff:fe21:6431/128", null}},

                {{null, "fe80::2acf:e9ff:fe21:6431/128"}, {"aabb:1234:2acf:e9ff::fe21:6431/64", "fe80::2acf:e9ff:fe21:6431/128"}},
                {{null, "fe80::2acf:e9ff:fe21:6431/128"}, {"aabb:1234:2acf:e9ff::fe21:6431/64", "aabb:1234:2acf:e9ff::fe21:6431/64"}},
                {{null, "fe80::2acf:e9ff:fe21:6431/128"}, {null, "aabb:1234:2acf:e9ff::fe21:6431/64"}},
                {{null, "fe80::2acf:e9ff:fe21:6431/128"}, {null, "fe80::2acf:e9ff:fe21:6431/128"}},

                {{null, null}, {null, "fe80::2acf:e9ff:fe21:6431/128"}},
                {{null, null}, {null, null}},
        };

        final boolean[] matches = new boolean[] {
                true,
                false,
                false,
                false,

                false,
                false,
                false,
                true,

                false,
                false,
                false,
                true,

                false,
                true
        };

        for (int i = 0; i < matches.length; i++) {
            checkComparisonOfL3MatchIpv4(
                    matchSeeds[i][0][0], matchSeeds[i][0][1],
                    matchSeeds[i][1][0], matchSeeds[i][1][1],
                    matches[i]);
        }

       for (int i = 0; i < matches.length; i++) {
            checkComparisonOfL3MatchIpv6(
                    matchSeedsIpv6[i][0][0], matchSeedsIpv6[i][0][1],
                    matchSeedsIpv6[i][1][0], matchSeedsIpv6[i][1][1],
                    matches[i]);
        }
    }

    /**
     * @param m1Source match1 - src
     * @param m1Destination match1 - dest
     * @param m2Source match2 - src
     * @param msDestination match2 - dest
     * @param matches expected match output
     *
     */
    private static void checkComparisonOfL3MatchIpv4(final String m1Source, final String m1Destination,
            final String m2Source, final String msDestination, final boolean matches) {
        final Ipv4Match m1Layer3 = prepareIPv4Match(m1Source, m1Destination);
        final Ipv4Match m2Layer3 = prepareIPv4Match(m2Source, msDestination);
        boolean comparisonResult;
        try {
            comparisonResult = FlowComparator.layer3MatchEquals(m1Layer3, m2Layer3);
            Assert.assertEquals("failed to compare: "+m1Layer3+" vs. "+m2Layer3,
                    matches, comparisonResult);
        } catch (final Exception e) {
            LOG.error("failed to compare: {} vs. {}", m1Layer3, m2Layer3, e);
            Assert.fail(e.getMessage());
        }
    }

    private static Ipv4Match prepareIPv4Match(final String source, final String destination) {
        final Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder();
        if (source != null) {
            ipv4MatchBuilder.setIpv4Source(new Ipv4Prefix(source));
        }
        if (destination != null) {
            ipv4MatchBuilder.setIpv4Destination(new Ipv4Prefix(destination));
        }

        return ipv4MatchBuilder.build();
    }

    /**
     * @param m1Source match1 - src
     * @param m1Destination match1 - dest
     * @param m2Source match2 - src
     * @param msDestination match2 - dest
     * @param matches expected match output
     *
     */
    private static void checkComparisonOfL3MatchIpv6(final String m1Source, final String m1Destination,
                                                 final String m2Source, final String msDestination, final boolean matches) {
        final Ipv6Match m1Layer3 = prepareIPv6Match(m1Source, m1Destination);
        final Ipv6Match m2Layer3 = prepareIPv6Match(m2Source, msDestination);
        boolean comparisonResult;
        try {
            comparisonResult = FlowComparator.layer3MatchEquals(m1Layer3, m2Layer3);
            Assert.assertEquals("failed to compare: "+m1Layer3+" vs. "+m2Layer3,
                    matches, comparisonResult);
        } catch (final Exception e) {
            LOG.error("failed to compare: {} vs. {}", m1Layer3, m2Layer3, e);
            Assert.fail(e.getMessage());
        }
    }

    private static Ipv6Match prepareIPv6Match(final String source, final String destination) {
        final Ipv6MatchBuilder ipv6MatchBuilder = new Ipv6MatchBuilder();
        if (source != null) {
            ipv6MatchBuilder.setIpv6Source(new Ipv6Prefix(source));
        }
        if (destination != null) {
            ipv6MatchBuilder.setIpv6Destination(new Ipv6Prefix(destination));
        }

        return ipv6MatchBuilder.build();
    }
    /**
     * Test method for {@link org.opendaylight.controller.md.statistics.manager.impl.helper.FlowComparator#ethernetMatchEquals(EthernetMatch, EthernetMatch)
     */
    @Test
    public void testEthernetMatchEquals() {
        final String[][][] ethernetMatchSeeds = new String[][][] {
                {{"aa:bb:cc:dd:ee:ff", "ff:ff:ff:ff:ff:ff","0800"}, {"aa:bb:cc:dd:ee:ff", "ff:ff:ff:ff:ff:ff","0800"}},
                {{"aa:bb:cc:dd:ee:ff", "ff:ff:ff:ff:ff:ff","0800"}, {"aa:bb:bc:cd:ee:ff", "ff:ff:ff:ff:ff:ff","0800"}},
                {{"aa:bb:cc:dd:ee:ff", "ff:ff:ff:ff:ff:ff","0800"}, {"AA:BB:CC:DD:EE:FF", "ff:ff:ff:ff:ff:ff","0800"}},
                {{"AA:BB:CC:dd:ee:ff", "ff:ff:ff:ff:ff:ff","0800"}, {"aa:bb:cc:dd:ee:ff", "ff:ff:ff:ff:ff:ff","0800"}},
                {{"AA:BB:CC:dd:ee:ff", "ff:ff:ff:ff:ff:ff","0800"}, {"aa:bb:cc:dd:ee:ff", "FF:FF:FF:FF:FF:FF","0800"}},
                {{"AA:BB:CC:dd:ee:ff", "ff:ff:ff:ee:ee:ee","0800"}, {"aa:bb:cc:dd:ee:ff", "FF:FF:FF:FF:FF:FF","0800"}},

                {{"AA:BB:CC:dd:ee:ff", null,"0800"}, {"aa:bb:cc:dd:ee:ff", null,"0800"}},
                {{"AA:BB:CC:dd:ee:ff", null,"0800"}, {"aa:bb:cc:dd:ee:ff", null,"0806"}},
                {{"AA:BB:CC:dd:ee:ff", null,"0800"}, {"aa:bb:cc:dd:ee:ff", "FF:FF:FF:FF:FF:FF","0800"}},
                {{"AA:BB:CC:dd:ee:ff", null,"0800"}, {null, "FF:FF:FF:FF:FF:FF","0800"}},

                {{"AA:BB:CC:dd:ee:ff", "ff:ff:ff:ff:ff:ff",null}, {null, "FF:FF:FF:FF:FF:FF","0800"}},
                {{"AA:BB:CC:dd:ee:ff", "ff:ff:ff:ff:ff:ff",null}, {"aa:bb:cc:dd:ee:ff", "FF:FF:FF:FF:FF:FF",null}},
                {{"AA:BB:CC:dd:ee:ff", "ff:ff:ff:ff:ff:ff",null}, {null, "FF:FF:FF:FF:FF:FF",null}},

                {{null, null,null}, {null, null,"0800"}},
                {{null, null,null}, {null, null,null}},
        };

        final boolean[] matches = new boolean[] {
                true,
                false,
                true,
                true,
                true,
                false,

                true,
                false,
                false,
                false,

                false,
                true,
                false,

                false,
                true
        };

        for (int i = 0; i < matches.length; i++) {
            checkComparisonOfEthernetMatch(
                    ethernetMatchSeeds[i][0][0], ethernetMatchSeeds[i][0][1],ethernetMatchSeeds[i][0][2],
                    ethernetMatchSeeds[i][1][0], ethernetMatchSeeds[i][1][1],ethernetMatchSeeds[i][1][2],
                    matches[i]);
        }
    }

    /*
     * @param ethernetMatch1
     * @param ethernetMatch2
     */
    private static void checkComparisonOfEthernetMatch(final String macAddress1, final String macAddressMask1,final String etherType1,
            final String macAddress2, final String macAddressMask2,final String etherType2, final boolean expectedResult) {
        final EthernetMatch ethernetMatch1 = prepareEthernetMatch(macAddress1, macAddressMask1,etherType1);
        final EthernetMatch ethernetMatch2 = prepareEthernetMatch(macAddress2, macAddressMask2,etherType2);
        boolean comparisonResult;
        try {
            comparisonResult = FlowComparator.ethernetMatchEquals(ethernetMatch1, ethernetMatch2);
            Assert.assertEquals("failed to compare: "+ethernetMatch1+" vs. "+ethernetMatch2,
                    expectedResult, comparisonResult);
        } catch (final Exception e) {
            LOG.error("failed to compare: {} vs. {}", ethernetMatch1, ethernetMatch2, e);
            Assert.fail(e.getMessage());
        }
    }

    private static EthernetMatch prepareEthernetMatch(final String macAddress, final String macAddressMask, final String etherType) {
        final EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder();
        final EthernetSourceBuilder ethernetSourceBuilder =  new EthernetSourceBuilder();
        if (macAddress != null) {
            ethernetSourceBuilder.setAddress(new MacAddress(macAddress));
        }
        if (macAddressMask != null) {
            ethernetSourceBuilder.setMask(new MacAddress(macAddressMask));
        }
        if(etherType != null){
            final EthernetTypeBuilder ethernetType = new EthernetTypeBuilder();
            ethernetType.setType(new EtherType(Long.parseLong(etherType,16)));
            ethernetMatchBuilder.setEthernetType(ethernetType.build());
        }
        ethernetMatchBuilder.setEthernetSource(ethernetSourceBuilder.build());

        return ethernetMatchBuilder.build();
    }
}
