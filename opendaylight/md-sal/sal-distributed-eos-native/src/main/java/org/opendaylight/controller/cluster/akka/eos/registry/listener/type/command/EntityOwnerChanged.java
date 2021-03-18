/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.akka.eos.registry.listener.type.command;

import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipChange;

/**
 * Notification sent to EntityTypeListenerActor when there is an owner change for an Entity of a given type.
 */
public class EntityOwnerChanged implements TypeListenerCommand {

    private final DOMEntityOwnershipChange ownershipChange;

    public EntityOwnerChanged(final DOMEntityOwnershipChange ownershipChange) {
        this.ownershipChange = ownershipChange;
    }

    public DOMEntityOwnershipChange getOwnershipChange() {
        return ownershipChange;
    }

    @Override
    public String toString() {
        return "EntityOwnerChanged{"
                + "ownershipChange=" + ownershipChange
                + '}';
    }
}
