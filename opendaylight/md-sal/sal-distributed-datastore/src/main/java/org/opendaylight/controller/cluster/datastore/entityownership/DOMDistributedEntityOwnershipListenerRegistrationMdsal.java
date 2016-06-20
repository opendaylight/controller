/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership;

import org.opendaylight.mdsal.common.api.clustering.AbstractGenericEntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class DOMDistributedEntityOwnershipListenerRegistrationMdsal extends
        AbstractGenericEntityOwnershipListenerRegistration<YangInstanceIdentifier, DOMEntityOwnershipListener>
        implements DOMEntityOwnershipListenerRegistration {

    private final DOMDistributedEntityOwnershipServiceMdsal service;

    DOMDistributedEntityOwnershipListenerRegistrationMdsal(final DOMEntityOwnershipListener listener,
            final String entityType, final DOMDistributedEntityOwnershipServiceMdsal service) {
        super(listener, entityType);
        this.service = service;
    }

    @Override
    protected void removeRegistration() {
        service.unregisterListener(getEntityType(), getInstance());
    }

    @Override
    public String toString() {
        return "DOMDistributedEntityOwnershipListenerRegistration [entityType=" + getEntityType() + ", listener="
                + getInstance() + "]";
    }

}
