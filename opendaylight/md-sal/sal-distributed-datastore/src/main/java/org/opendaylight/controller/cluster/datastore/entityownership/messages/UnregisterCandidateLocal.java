/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership.messages;

import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidate;

/**
 * Message sent to the local EntityOwnershipShard to unregister a candidate.
 *
 * @author Thomas Pantelis
 */
public class UnregisterCandidateLocal {
    private final EntityOwnershipCandidate candidate;
    private final Entity entity;

    public UnregisterCandidateLocal(EntityOwnershipCandidate candidate, Entity entity) {
        this.candidate = candidate;
        this.entity = entity;
    }

    public EntityOwnershipCandidate getCandidate() {
        return candidate;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return "UnregisterCandidateLocal [entity=" + entity + ", candidate=" + candidate + "]";
    }
}
