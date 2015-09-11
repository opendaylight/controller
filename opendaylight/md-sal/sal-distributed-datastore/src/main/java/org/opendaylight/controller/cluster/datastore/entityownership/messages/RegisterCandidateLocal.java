/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership.messages;

import org.opendaylight.controller.md.sal.common.api.clustering.Entity;

/**
 * Message sent to the local EntityOwnershipShard to register a candidate.
 *
 * @author Thomas Pantelis
 */
public class RegisterCandidateLocal {
    private final Entity entity;

    public RegisterCandidateLocal(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return "RegisterCandidateLocal [entity=" + entity + "]";
    }
}
