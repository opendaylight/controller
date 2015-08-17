/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.impl.clustering.AbstractEntityOwnershipListenerRegistration;

/**
 * Implementation of EntityOwnershipListenerRegistration.
 *
 * @author Thomas Pantelis
 */
class DistributedEntityOwnershipListenerRegistration
        extends AbstractEntityOwnershipListenerRegistration<EntityOwnershipListener>{

    private final DistributedEntityOwnershipService service;

    DistributedEntityOwnershipListenerRegistration(EntityOwnershipListener listener, Entity entity,
            DistributedEntityOwnershipService service) {
        super(listener, entity);
        this.service = service;
    }

    @Override
    public void close() {
        service.unregisterListener(getEntity(), getInstance());
    }
}
