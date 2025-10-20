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
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.eos.akka.registry.listener.type.EntityTypeListenerActor;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipStateChange;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

/**
 * Notification sent to {@link EntityTypeListenerActor} when there is an owner change for an Entity of a given type.
 */
@NonNullByDefault
public final class EntityOwnerChanged extends TypeListenerCommand {
    private final DOMEntity entity;
    private final EntityOwnershipStateChange change;
    private final boolean inJeopardy;

    public EntityOwnerChanged(final DOMEntity entity, final EntityOwnershipStateChange change,
            final boolean inJeopardy) {
        this.entity = requireNonNull(entity);
        this.change = requireNonNull(change);
        this.inJeopardy = inJeopardy;
    }

    public DOMEntity entity() {
        return entity;
    }

    public EntityOwnershipStateChange change() {
        return change;
    }

    public boolean inJeopardy() {
        return inJeopardy;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("entity", entity)
            .add("change", change)
            .add("inJeopardy", inJeopardy)
            .toString();
    }
}
