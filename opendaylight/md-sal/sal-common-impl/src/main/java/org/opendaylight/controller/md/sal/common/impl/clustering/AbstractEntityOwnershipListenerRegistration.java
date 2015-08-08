/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.clustering;

import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;

/**
 * Abstract base class for an EntityOwnershipListenerRegistration.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractEntityOwnershipListenerRegistration<T extends EntityOwnershipListener>
        implements EntityOwnershipListenerRegistration {
    private final T listener;
    private final Entity entity;

    protected AbstractEntityOwnershipListenerRegistration(T listener, Entity entity) {
        this.listener = listener;
        this.entity = entity;
    }

    @Override
    public T getInstance() {
        return listener;
    }

    @Override
    public Entity getEntity() {
        return entity;
    }
}
