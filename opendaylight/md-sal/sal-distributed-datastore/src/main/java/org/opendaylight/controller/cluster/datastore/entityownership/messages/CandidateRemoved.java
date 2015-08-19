/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership.messages;

import java.util.Collection;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

public class CandidateRemoved {
    private final YangInstanceIdentifier entityId;
    private final String removedCandidateName;
    private final Collection<MapEntryNode> candidates;

    public CandidateRemoved(YangInstanceIdentifier entityId, String removedCandidateName, Collection<MapEntryNode> candidates) {
        this.entityId = entityId;
        this.removedCandidateName = removedCandidateName;
        this.candidates = candidates;
    }

    public YangInstanceIdentifier getEntityId() {
        return entityId;
    }

    public String getRemovedCandidateName() {
        return removedCandidateName;
    }

    public Collection<MapEntryNode> getCandidates() {
        return candidates;
    }
}
