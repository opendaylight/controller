/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv4DestinationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.Ipv4SourceCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv4.destination._case.Ipv4DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.attributes.match.ipv4.source._case.Ipv4SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder;
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
        String[][] matchSeeds = new String[][] {
                {"10.1.2.0/24", "10.1.2.0/24"},// true
                {"10.1.2.0/24", "10.1.1.0/24"},  // false
                {"10.1.1.0/24", "10.1.2.0/24"},  // false
                {"10.1.2.0/24", "10.1.2.0/24"},  // true
                {"10.1.1.0/24", "10.1.1.0/24"},  // true

                {"10.1.1.0/24", null},  // false
                {"10.1.1.0/24", "10.1.2.0/24"}, // false
                {"10.1.1.0/24", null},  // false
                {"10.1.2.0/24", "10.1.2.0/24"},  // true
                {"10.1.1.0/24", null},  // false
                {"10.1.2.0/24", null},  // false
                {"10.1.1.0/24", null}, //  false
                {"10.1.1.0/24", null}, // false

                {null, "10.1.1.0/24"},  // false
                {"10.1.2.0/24", "10.1.1.0/24"},  // false
                {null, "10.1.1.0/24"},  // false
                {"10.1.2.0/24", "10.1.2.0/24"},  // true
                {null, "10.1.1.0/24"},  // false
                {null, "10.1.2.0/24"},  // false
                {null, "10.1.1.0/24"},  // false
                {null, "10.1.1.0/24"},  // false

                {null, null},  // false
                {null, "10.1.1.0/24"}, // false
                {null, null},  // false
                {null, null},  // false
        };

        boolean[] matches = new boolean[] {
                true,
                false,
                false,
                true,
                true,

                false,
                false,
                false,
                true,
                false,
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
                false,

                false,
                false,
                false,
                false,
        };

        for (int i = 0; i < matches.length; i++) {
            checkComparisonOfIpV4Source(
                    matchSeeds[i][0], matchSeeds[i][1],
                    matches[i]);
            checkComparisonOfIpV4Destination(
                    matchSeeds[i][0], matchSeeds[i][1],
                    matches[i]);
        }
    }

    private static void addMatch(List<Match> matches,org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.list.MatchBuilder match) {
        if(match != null) {
            match.setOrder(matches.size());
            matches.add(match.getOrder(), match.build());
        }
    }

    private static void checkComparisonOfIpV4Source(String m1Source,
            String m2Source, boolean matches) {
        MatchBuilder m1 = prepareIPv4Source(m1Source);
        MatchBuilder m2 = prepareIPv4Source(m2Source);
        checkComparisonOfMatches(matches, m1, m2);
    }

    private static void checkComparisonOfIpV4Destination(String m1Source,
            String m2Source, boolean matches) {
        MatchBuilder m1 = prepareIPv4Destination(m1Source);
        MatchBuilder m2 = prepareIPv4Destination(m2Source);
        checkComparisonOfMatches(matches, m1, m2);
    }

    protected static void checkComparisonOfMatches(boolean matches,
            MatchBuilder m1, MatchBuilder m2) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder flowMatchBuilder1 = new org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder flowMatchBuilder2 = new org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder();
        List<Match> matchList1 = new ArrayList<Match>();
        List<Match> matchList2 = new ArrayList<Match>();
        addMatch(matchList1,m1);
        addMatch(matchList2,m2);
        flowMatchBuilder1.setMatch(matchList1);
        flowMatchBuilder2.setMatch(matchList2);
        boolean comparisonResult;
        comparisonResult = FlowComparator.matchEquals(flowMatchBuilder1.build(), flowMatchBuilder2.build());
        Assert.assertEquals("failed to compare: "+flowMatchBuilder1.build()+" vs. "+flowMatchBuilder2.build(),
                matches, comparisonResult);

    }

    private static MatchBuilder prepareIPv4Source(String source) {
        Ipv4SourceBuilder ipv4SourceBuilder = new Ipv4SourceBuilder();
        if (source != null) {
            ipv4SourceBuilder.setIpv4Source(new Ipv4Prefix(source));
        }
        Ipv4SourceCaseBuilder ipv4src = new Ipv4SourceCaseBuilder()
                .setIpv4Source(ipv4SourceBuilder.build());
        MatchBuilder mb = new MatchBuilder().setMatch(ipv4src.build());
        return mb;
    }

    private static MatchBuilder prepareIPv4Destination(String source) {
        Ipv4DestinationBuilder ipv4DestinationBuilder = new Ipv4DestinationBuilder();
        if (source != null) {
            ipv4DestinationBuilder.setIpv4Destination(new Ipv4Prefix(source));
        }
        Ipv4DestinationCaseBuilder ipv4dst= new Ipv4DestinationCaseBuilder()
                .setIpv4Destination(ipv4DestinationBuilder.build());
        MatchBuilder mb = new MatchBuilder().setMatch(ipv4dst.build());
        return mb;
    }

}
