/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership.messages;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;

/**
 * Message encapsulating an entity ownership change.
 *
 * @author Thomas Pantelis
 */
public class EntityOwnershipChanged {
    private final Entity entity;
    private final boolean wasOwner;
    private final boolean isOwner;
    private final boolean hasOwner;

    public EntityOwnershipChanged(@Nonnull Entity entity, boolean wasOwner, boolean isOwner, boolean hasOwner) {
        this.entity = Preconditions.checkNotNull(entity, "entity can't be null");
        this.wasOwner = wasOwner;
        this.isOwner = isOwner;
        this.hasOwner = hasOwner;
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean wasOwner() {
        return wasOwner;
    }

    public boolean isOwner() {
        return isOwner;
    }


    public boolean hasOwner() {
        return hasOwner;
    }

    @Override
    public String toString() {
        return "EntityOwnershipChanged [entity=" + entity + ", wasOwner=" + wasOwner + ", isOwner=" + isOwner
                + ", hasOwner=" + hasOwner + "]";
    }
}
