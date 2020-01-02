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
 * Message sent when a new candidate is added for an entity.
 *
 * @author Moiz Raja
 * @author Thomas Pantelis
 */
public class CandidateAdded {
    private final YangInstanceIdentifier entityPath;
    private final Collection<String> allCandidates;
    private final String newCandidate;

    public CandidateAdded(final YangInstanceIdentifier entityPath, final String newCandidate,
            final Collection<String> allCandidates) {
        this.entityPath = entityPath;
        this.newCandidate = newCandidate;
        this.allCandidates = allCandidates;
    }

    public YangInstanceIdentifier getEntityPath() {
        return entityPath;
    }

    public Collection<String> getAllCandidates() {
        return allCandidates;
    }

    public String getNewCandidate() {
        return newCandidate;
    }

    @Override
    public String toString() {
        return "CandidateAdded [entityPath=" + entityPath + ", newCandidate=" + newCandidate + ", allCandidates="
                + allCandidates + "]";
    }
}
