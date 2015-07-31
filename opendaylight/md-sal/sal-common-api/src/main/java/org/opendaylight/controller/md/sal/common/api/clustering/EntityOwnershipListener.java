/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.clustering;

/**
 * An EntityOwnershipListener is a component that represents a listener for entity ownership changes
 */
public interface EntityOwnershipListener {

    /**
     * A notification that is generated when the ownership status for a given entity changes in the current process.
     *
     * @param entity the entity whose ownership status has changed
     * @param wasOwner true if this process was the owner of the given entity right before this notification
     *                 was generated
     * @param isOwner true if this process now owns the given entity
     */
    void ownershipChanged(Entity entity, boolean wasOwner, boolean isOwner);
}
