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
     * The following outlines valid combinations of the status parameters and their meanings:
     * <ul>
     * <li><b>wasOwner = false, isOwner = true, hasOwner = true</b> - this process has been granted ownership</li>
     * <li><b>wasOwner = true, isOwner = false, hasOwner = true</b> - this process was the owner but ownership
     *     transitioned to another process</li>
     * <li><b>wasOwner = false, isOwner = false, hasOwner = true</b> - ownership transitioned to another process
     *     and this process was not the previous owner</li>
     * <li><b>wasOwner = false, isOwner = false, hasOwner = false</b> - the entity no longer has any candidates and
     *     thus no owner and this process was not the previous owner</li>
     * <li><b>wasOwner = true, isOwner = false, hasOwner = false</b> - the entity no longer has any candidates and
     *     thus no owner and this process was the previous owner</li>
     * </ul>
     * @param entity the entity whose ownership status has changed
     * @param wasOwner true if this process was the owner of the given entity at the time this notification
     *                 was generated
     * @param isOwner true if this process is now the owner the given entity
     * @param hasOwner true if the given entity has an owner although not necessarily this process. If false,
     *                 then the entity has no candidates and thus no owner.
     */
    void ownershipChanged(Entity entity, boolean wasOwner, boolean isOwner, boolean hasOwner);
}
