/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.clustering;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;

/**
 * A DTO that encapsulates an ownership change for an entity.
 *
 * @author Thomas Pantelis
 */
public class EntityOwnershipChange {
    private final Entity entity;
    private final boolean wasOwner;
    private final boolean isOwner;
    private final boolean hasOwner;
    private final boolean inJeopardy;

    public EntityOwnershipChange(@Nonnull Entity entity, boolean wasOwner, boolean isOwner, boolean hasOwner) {
        this(entity, wasOwner, isOwner, hasOwner, false);
    }

    public EntityOwnershipChange(@Nonnull Entity entity, boolean wasOwner, boolean isOwner, boolean hasOwner,
            boolean inJeopardy) {
        this.entity = Preconditions.checkNotNull(entity, "entity can't be null");
        this.wasOwner = wasOwner;
        this.isOwner = isOwner;
        this.hasOwner = hasOwner;
        this.inJeopardy = inJeopardy;
    }

    /**
     * Returns the entity whose ownership status changed.
     * @return the entity
     */
    @Nonnull public Entity getEntity() {
        return entity;
    }

    /**
     * Returns the previous ownership status of the entity for this process instance.
     * @return true if this process was the owner of the entity at the time this notification was generated
     */
    public boolean wasOwner() {
        return wasOwner;
    }

    /**
     * Returns the current ownership status of the entity for this process instance.
     * @return true if this process is now the owner of the entity
     */
    public boolean isOwner() {
        return isOwner;
    }

    /**
     * Returns the current ownership status of the entity across all process instances.
     * @return true if the entity has an owner which may or may not be this process. If false, then
     *         the entity has no candidates and thus no owner.
     */
    public boolean hasOwner() {
        return hasOwner;
    }

    /**
     * Returns the current jeopardy state. When in a jeopardy state, the values from other methods may potentially
     * be out of date.
     *
     * @return true if the local node is in a jeopardy state. If false, the reported information is accurate.
     */
    public boolean inJeopardy() {
        return inJeopardy;
    }

    @Override
    public String toString() {
        return "EntityOwnershipChanged [entity=" + entity + ", wasOwner=" + wasOwner + ", isOwner=" + isOwner
                + ", hasOwner=" + hasOwner + ", inJeopardy=" + inJeopardy + "]";
    }
}
