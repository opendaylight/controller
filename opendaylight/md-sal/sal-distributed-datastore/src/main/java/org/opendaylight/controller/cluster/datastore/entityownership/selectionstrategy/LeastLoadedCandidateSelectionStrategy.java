/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy;

import java.util.Collection;
import java.util.Map;

/**
 * The LeastLoadedCandidateSelectionStrategy assigns ownership for an entity to the candidate which owns the least
 * number of entities.
 */
public class LeastLoadedCandidateSelectionStrategy extends AbstractEntityOwnerSelectionStrategy {
    protected LeastLoadedCandidateSelectionStrategy(long selectionDelayInMillis) {
        super(selectionDelayInMillis);
    }

    @Override
    public String newOwner(Collection<String> viableCandidates, Map<String, Long> statistics) {
        String leastLoadedCandidate = null;
        long leastLoadedCount = Long.MAX_VALUE;

        for(String candidateName : viableCandidates){
            Long val = statistics.get(candidateName);
            if(val != null && val < leastLoadedCount){
                leastLoadedCount = val;
                leastLoadedCandidate = candidateName;
            }
        }

        if(leastLoadedCandidate == null){
            return viableCandidates.iterator().next();
        }
        return leastLoadedCandidate;
    }
}
