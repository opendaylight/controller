/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership.messages;

import org.opendaylight.mdsal.dom.api.clustering.DOMEntity;

/**
 * Message sent to the local EntityOwnershipShard to register a candidate.
 *
 */
public class DOMRegisterCandidateLocal {

    private final DOMEntity entity;

    public DOMRegisterCandidateLocal(final DOMEntity entity) {
        this.entity = entity;
    }

    public DOMEntity getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return "DOMRegisterCandidateLocal [DOMentity=" + entity + "]";
    }
}
