/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import com.google.common.base.Preconditions;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

/**
 * Implementation of EntityOwnershipListenerRegistration.
 *
 * @author Thomas Pantelis
 */
class DistributedEntityOwnershipListenerRegistration extends AbstractObjectRegistration<DOMEntityOwnershipListener>
        implements DOMEntityOwnershipListenerRegistration {
    private final DistributedEntityOwnershipService service;
    private final String entityType;

    DistributedEntityOwnershipListenerRegistration(DOMEntityOwnershipListener listener, String entityType,
            DistributedEntityOwnershipService service) {
        super(listener);
        this.entityType = Preconditions.checkNotNull(entityType, "entityType cannot be null");
        this.service = Preconditions.checkNotNull(service, "DOMEntityOwnershipListener cannot be null");
    }

    @Override
    protected void removeRegistration() {
        service.unregisterListener(getEntityType(), getInstance());
    }

    @Override
    public String getEntityType() {
        return entityType;
    }

    @Override
    public String toString() {
        return "DistributedEntityOwnershipListenerRegistration [entityType=" + getEntityType()
                + ", listener=" + getInstance() + "]";
    }
}
