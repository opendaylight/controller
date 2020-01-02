/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership.selectionstrategy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The LeastLoadedCandidateSelectionStrategy assigns ownership for an entity to the candidate which owns the least
 * number of entities.
 */
public class LeastLoadedCandidateSelectionStrategy extends AbstractEntityOwnerSelectionStrategy {
    private final Map<String, Long> localStatistics = new HashMap<>();

    protected LeastLoadedCandidateSelectionStrategy(final long selectionDelayInMillis,
            final Map<String, Long> initialStatistics) {
        super(selectionDelayInMillis, initialStatistics);

        localStatistics.putAll(initialStatistics);
    }

    @Override
    public String newOwner(final String currentOwner, final Collection<String> viableCandidates) {
        Preconditions.checkArgument(viableCandidates.size() > 0);
        String leastLoadedCandidate = null;
        long leastLoadedCount = Long.MAX_VALUE;

        if (!Strings.isNullOrEmpty(currentOwner)) {
            long localVal = MoreObjects.firstNonNull(localStatistics.get(currentOwner), 0L);
            localStatistics.put(currentOwner, localVal - 1);
        }

        for (String candidateName : viableCandidates) {
            long val = MoreObjects.firstNonNull(localStatistics.get(candidateName), 0L);
            if (val < leastLoadedCount) {
                leastLoadedCount = val;
                leastLoadedCandidate = candidateName;
            }
        }

        if (leastLoadedCandidate == null) {
            leastLoadedCandidate = viableCandidates.iterator().next();
        }

        localStatistics.put(leastLoadedCandidate, leastLoadedCount + 1);
        return leastLoadedCandidate;
    }

    @VisibleForTesting
    Map<String, Long> getLocalStatistics() {
        return localStatistics;
    }
}
