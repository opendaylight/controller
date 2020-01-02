/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership.messages;

import java.util.Collection;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Message sent when a candidate is removed for an entity.
 *
 * @author Moiz Raja
 * @author Thomas Pantelis
 */
public class CandidateRemoved {
    private final YangInstanceIdentifier entityPath;
    private final String removedCandidate;
    private final Collection<String> remainingCandidates;

    public CandidateRemoved(final YangInstanceIdentifier entityPath, final String removedCandidate,
            final Collection<String> remainingCandidates) {
        this.entityPath = entityPath;
        this.removedCandidate = removedCandidate;
        this.remainingCandidates = remainingCandidates;
    }

    public YangInstanceIdentifier getEntityPath() {
        return entityPath;
    }

    public String getRemovedCandidate() {
        return removedCandidate;
    }

    public Collection<String> getRemainingCandidates() {
        return remainingCandidates;
    }

    @Override
    public String toString() {
        return "CandidateRemoved [entityPath=" + entityPath + ", removedCandidate=" + removedCandidate
                + ", remainingCandidates=" + remainingCandidates + "]";
    }
}
