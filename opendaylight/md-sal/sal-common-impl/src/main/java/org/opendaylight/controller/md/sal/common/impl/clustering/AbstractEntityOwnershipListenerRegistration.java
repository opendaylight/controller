/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.clustering;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

/**
 * Abstract base class for an EntityOwnershipListenerRegistration.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractEntityOwnershipListenerRegistration extends AbstractObjectRegistration<EntityOwnershipListener>
        implements EntityOwnershipListenerRegistration {
    private final String entityType;

    protected AbstractEntityOwnershipListenerRegistration(@Nonnull EntityOwnershipListener listener,
            @Nonnull String entityType) {
        super(listener);
        this.entityType = Preconditions.checkNotNull(entityType, "entityType cannot be null");
    }

    @Override
    public String getEntityType() {
        return entityType;
    }
}
