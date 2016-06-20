/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership;

import org.opendaylight.mdsal.common.api.clustering.AbstractGenericEntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntity;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Implementation of DOMEntityOwnershipCandidateRegistration.
 *
 */
public class DOMDistributedEntityOwnershipCandidateRegistrationMdsal
        extends AbstractGenericEntityOwnershipCandidateRegistration<YangInstanceIdentifier, DOMEntity>
        implements DOMEntityOwnershipCandidateRegistration {

    private final DOMDistributedEntityOwnershipServiceMdsal service;

    DOMDistributedEntityOwnershipCandidateRegistrationMdsal(final DOMEntity entity,
            final DOMDistributedEntityOwnershipServiceMdsal service) {
        super(entity);
        this.service = service;
    }

    @Override
    protected void removeRegistration() {
        service.unregisterCandidate(getInstance());
    }

    @Override
    public String toString() {
        return "DOMDistributedEntityOwnershipCandidateRegistration [DOMentity=" + getInstance() + "]";
    }

}
