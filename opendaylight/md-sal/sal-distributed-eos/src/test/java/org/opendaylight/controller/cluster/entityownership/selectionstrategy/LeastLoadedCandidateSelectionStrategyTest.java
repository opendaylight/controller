/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership.selectionstrategy;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class LeastLoadedCandidateSelectionStrategyTest {

    @Test
    public void testLeastLoadedStrategy() {
        LeastLoadedCandidateSelectionStrategy strategy = new LeastLoadedCandidateSelectionStrategy(
                0L, Collections.<String, Long>emptyMap());

        String owner = strategy.newOwner(null, prepareViableCandidates(3));
        assertEquals("member-1", owner);

        Map<String, Long> localStatistics = strategy.getLocalStatistics();
        assertEquals(1L, (long) localStatistics.get("member-1"));

        // member-2 has least load
        strategy = new LeastLoadedCandidateSelectionStrategy(0L, prepareStatistics(5,2,4));
        owner = strategy.newOwner(null, prepareViableCandidates(3));
        assertEquals("member-2", owner);

        assertStatistics(strategy.getLocalStatistics(), 5,3,4);

        // member-3 has least load
        strategy = new LeastLoadedCandidateSelectionStrategy(0L, prepareStatistics(5,7,4));
        owner = strategy.newOwner(null, prepareViableCandidates(3));
        assertEquals("member-3", owner);

        assertStatistics(strategy.getLocalStatistics(), 5,7,5);

        // member-1 has least load
        strategy = new LeastLoadedCandidateSelectionStrategy(0L, prepareStatistics(1,7,4));
        owner = strategy.newOwner(null, prepareViableCandidates(3));
        assertEquals("member-1", owner);

        assertStatistics(strategy.getLocalStatistics(), 2,7,4);

        // Let member-3 become the owner
        strategy = new LeastLoadedCandidateSelectionStrategy(0L, prepareStatistics(3,3,0));
        owner = strategy.newOwner(null, prepareViableCandidates(3));
        assertEquals("member-3", owner);

        assertStatistics(strategy.getLocalStatistics(), 3,3,1);

        // member-3 is no longer viable so choose a new owner
        owner = strategy.newOwner("member-3", prepareViableCandidates(2));
        assertEquals("member-1", owner);

        assertStatistics(strategy.getLocalStatistics(), 4,3,0);

    }

    private static Map<String, Long> prepareStatistics(long... count) {
        Map<String, Long> statistics = new HashMap<>();
        for (int i = 0; i < count.length; i++) {
            statistics.put("member-" + (i + 1), count[i]);
        }
        return statistics;
    }

    private static Collection<String> prepareViableCandidates(int count) {
        Collection<String> viableCandidates = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            viableCandidates.add("member-" + (i + 1));
        }
        return viableCandidates;
    }

    private static void assertStatistics(Map<String, Long> statistics, long... count) {
        for (int i = 0; i < count.length; i++) {
            assertEquals(count[i], (long) statistics.get("member-" + (i + 1)));
        }
    }
}
