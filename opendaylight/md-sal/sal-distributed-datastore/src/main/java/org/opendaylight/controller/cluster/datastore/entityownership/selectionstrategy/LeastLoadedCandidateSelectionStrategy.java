/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy;

import com.google.common.base.MoreObjects;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The LeastLoadedCandidateSelectionStrategy assigns ownership for an entity to the candidate which owns the least
 * number of entities.
 */
public class LeastLoadedCandidateSelectionStrategy extends AbstractEntityOwnerSelectionStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(LeastLoadedCandidateSelectionStrategy.class);

    private Map<String, Long> localStatistics = new HashMap<>();

    protected LeastLoadedCandidateSelectionStrategy(long selectionDelayInMillis) {
        super(selectionDelayInMillis);
    }

    @Override
    public String newOwner(Collection<String> viableCandidates, Map<String, Long> statistics) {
        String leastLoadedCandidate = null;
        long leastLoadedCount = Long.MAX_VALUE;

        for(String candidateName : viableCandidates){
            long val = MoreObjects.firstNonNull(statistics.get(candidateName), 0L);
            long localVal = MoreObjects.firstNonNull(localStatistics.get(candidateName), 0L);
            if(val < localVal){
                LOG.debug("Local statistic higher - Candidate : {}, local statistic : {}, provided statistic : {}",
                        candidateName, localVal, val);
                val = localVal;
            } else {
                LOG.debug("Provided statistic higher - Candidate : {}, local statistic : {}, provided statistic : {}",
                        candidateName, localVal, val);
                localStatistics.put(candidateName, val);
            }
            if(val < leastLoadedCount){
                leastLoadedCount = val;
                leastLoadedCandidate = candidateName;
            }
        }

        if(leastLoadedCandidate == null){
            leastLoadedCandidate = viableCandidates.iterator().next();
        }

        localStatistics.put(leastLoadedCandidate, leastLoadedCount + 1);
        return leastLoadedCandidate;
    }
}
