/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership.messages;

import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

/**
 * Message sent to the local EntityOwnershipShard to register a candidate.
 *
 * @author Thomas Pantelis
 */
public class RegisterCandidateLocal {
    private final DOMEntity entity;

    public RegisterCandidateLocal(final DOMEntity entity) {
        this.entity = entity;
    }

    public DOMEntity getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return "RegisterCandidateLocal [entity=" + entity + "]";
    }
}
