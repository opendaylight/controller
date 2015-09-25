/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.clustering;

/**
 * A DTO that encapsulates ownership state for an entity.
 *
 * @author Thomas Pantelis
 */
public class EntityOwnershipState {
    private final boolean isOwner;
    private final boolean hasOwner;

    public EntityOwnershipState(boolean isOwner, boolean hasOwner) {
        this.isOwner = isOwner;
        this.hasOwner = hasOwner;
    }

    /**
     * Returns the current ownership status of the entity for this process instance.
     * @return true if this process is the owner of the entity
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

    @Override
    public String toString() {
        return "EntityOwnershipState [isOwner=" + isOwner + ", hasOwner=" + hasOwner + "]";
    }
}
