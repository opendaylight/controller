/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.entityownership.selectionstrategy;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Map;

public class LastCandidateSelectionStrategy extends AbstractEntityOwnerSelectionStrategy {
    public LastCandidateSelectionStrategy(long selectionDelayInMillis, Map<String, Long> initialStatistics) {
        super(selectionDelayInMillis, initialStatistics);
    }

    @Override
    public String newOwner(String currentOwner, Collection<String> viableCandidates) {
        return Iterables.getLast(viableCandidates);
    }
}
