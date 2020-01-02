/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

/**
 * Implementation of EntityOwnershipCandidateRegistration.
 *
 * @author Thomas Pantelis
 */
class DistributedEntityOwnershipCandidateRegistration extends AbstractObjectRegistration<DOMEntity>
        implements DOMEntityOwnershipCandidateRegistration {
    private final DistributedEntityOwnershipService service;

    DistributedEntityOwnershipCandidateRegistration(final DOMEntity entity,
            final DistributedEntityOwnershipService service) {
        super(entity);
        this.service = service;
    }

    @Override
    protected void removeRegistration() {
        service.unregisterCandidate(getInstance());
    }
}
