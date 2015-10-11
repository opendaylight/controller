/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class LeastLoadedCandidateSelectionStrategyTest {

    @Test
    public void testLeastLoadedStrategy(){
        LeastLoadedCandidateSelectionStrategy strategy = new LeastLoadedCandidateSelectionStrategy(0L);

        String owner = strategy.newOwner(prepareViableCandidates(3), new HashMap<String, Long>());
        assertEquals("member-1", owner);

        // member-2 has least load
        owner = strategy.newOwner(prepareViableCandidates(3), prepareStatistics(5,2,4));
        assertEquals("member-2", owner);

        // member-3 has least load
        owner = strategy.newOwner(prepareViableCandidates(3), prepareStatistics(5,7,4));
        assertEquals("member-3", owner);

        // member-1 has least load
        owner = strategy.newOwner(prepareViableCandidates(3), prepareStatistics(1,7,4));
        assertEquals("member-1", owner);

    }

    private Map<String, Long> prepareStatistics(long... count){
        Map<String, Long> statistics = new HashMap<>();
        for(int i=0;i<count.length;i++){
            statistics.put("member-" + (i+1), count[i]);
        }
        return statistics;
    }

    private Collection<String> prepareViableCandidates(int count){
        Collection<String> viableCandidates = new ArrayList<>();
        for(int i=0;i<count;i++){
            viableCandidates.add("member-" + (i+1));
        }
        return viableCandidates;
    }
}