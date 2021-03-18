/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.listener.type.command;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipChange;

/**
 * Notification sent to EntityTypeListenerActor when there is an owner change for an Entity of a given type.
 */
public final class EntityOwnerChanged extends TypeListenerCommand {
    private final @NonNull DOMEntityOwnershipChange ownershipChange;

    public EntityOwnerChanged(final DOMEntityOwnershipChange ownershipChange) {
        this.ownershipChange = requireNonNull(ownershipChange);
    }

    public @NonNull DOMEntityOwnershipChange getOwnershipChange() {
        return ownershipChange;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("ownershipChange", ownershipChange).toString();
    }
}
