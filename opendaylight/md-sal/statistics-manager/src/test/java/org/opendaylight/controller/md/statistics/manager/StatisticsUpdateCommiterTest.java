/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
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
        String[][][] matchSeeds = new String[][][] {
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

        boolean[] matches = new boolean[] {
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
            checkComparisonOfL3Match(
                    matchSeeds[i][0][0], matchSeeds[i][0][1],
                    matchSeeds[i][1][0], matchSeeds[i][1][1],
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
    private static void checkComparisonOfL3Match(String m1Source, String m1Destination,
            String m2Source, String msDestination, boolean matches) {
        Ipv4Match m1Layer3 = prepareIPv4Match(m1Source, m1Destination);
        Ipv4Match m2Layer3 = prepareIPv4Match(m2Source, msDestination);
        boolean comparisonResult;
        try {
            comparisonResult = FlowComparator.layer3MatchEquals(m1Layer3, m2Layer3);
            Assert.assertEquals("failed to compare: "+m1Layer3+" vs. "+m2Layer3,
                    matches, comparisonResult);
        } catch (Exception e) {
            LOG.error("failed to compare: {} vs. {}", m1Layer3, m2Layer3, e);
            Assert.fail(e.getMessage());
        }
    }

    private static Ipv4Match prepareIPv4Match(String source, String destination) {
        Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder();
        if (source != null) {
            ipv4MatchBuilder.setIpv4Source(new Ipv4Prefix(source));
        }
        if (destination != null) {
            ipv4MatchBuilder.setIpv4Destination(new Ipv4Prefix(destination));
        }

        return ipv4MatchBuilder.build();
    }

}
