/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership.selectionstrategy;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * The FirstCandidateSelectionStrategy always selects the first viable candidate from the list of candidates.
 */
public class FirstCandidateSelectionStrategy extends AbstractEntityOwnerSelectionStrategy {

    public static final FirstCandidateSelectionStrategy INSTANCE =
            new FirstCandidateSelectionStrategy(0L, Collections.emptyMap());

    public FirstCandidateSelectionStrategy(final long selectionDelayInMillis,
            final Map<String, Long> initialStatistics) {
        super(selectionDelayInMillis, initialStatistics);
    }

    @Override
    public String newOwner(final String currentOwner, final Collection<String> viableCandidates) {
        Preconditions.checkArgument(viableCandidates.size() > 0, "No viable candidates provided");
        return viableCandidates.iterator().next();
    }
}
