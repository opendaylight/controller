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
     * A notification that is generated when the ownership status of an entity changes.
     *
     * The following outlines valid combinations of the ownership status flags in the EntityOwnershipChange
     * parameter and their meanings:
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
     * @param ownershipChange contains the entity and its ownership status flags
     */
    void ownershipChanged(EntityOwnershipChange ownershipChange);
}
