/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.impl.clustering.AbstractEntityOwnershipCandidateRegistration;

/**
 * Implementation of EntityOwnershipCandidateRegistration.
 *
 * @author Thomas Pantelis
 */
class DistributedEntityOwnershipCandidateRegistration extends AbstractEntityOwnershipCandidateRegistration {
    private final DistributedEntityOwnershipService service;

    DistributedEntityOwnershipCandidateRegistration(Entity entity, DistributedEntityOwnershipService service) {
        super(entity);
        this.service = service;
    }

    @Override
    protected void removeRegistration() {
        service.unregisterCandidate(getInstance());
    }

    @Override
    public String toString() {
        return "DistributedEntityOwnershipCandidateRegistration [entity=" + getInstance() + "]";
    }


}
