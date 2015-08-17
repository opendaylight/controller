/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership.messages;

import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;

/**
 * Message sent to the local EntityOwnershipShard to register an EntityOwnershipListener.
 *
 * @author Thomas Pantelis
 */
public class RegisterListenerLocal {
    private final EntityOwnershipListener listener;
    private final String entityType;

    public RegisterListenerLocal(EntityOwnershipListener listener, String entityType) {
        this.listener = listener;
        this.entityType = entityType;
    }

    public EntityOwnershipListener getListener() {
        return listener;
    }

    public String getEntityType() {
        return entityType;
    }

    @Override
    public String toString() {
        return "RegisterListenerLocal [entityType=" + entityType + ", listener=" + listener + "]";
    }
}
