/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LastCandidateSelectionStrategy implements EntityOwnerSelectionStrategy {
    @Override
    public long selectionDelayInMillis() {
        return 500;
    }

    @Override
    public String newOwner(Collection<String> viableCandidates) {
        List<String> candidates = new ArrayList<>(viableCandidates);
        return candidates.get(candidates.size()-1);
    }
}
