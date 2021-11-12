/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.checker.command;

import akka.actor.typed.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityType;

public abstract class AbstractEntityRequest<T extends StateCheckerReply> extends StateCheckerRequest<T> {
    private static final long serialVersionUID = 1L;

    private final @NonNull EntityType type;
    private final @NonNull EntityName name;
    private final @NonNull EntityId entity;

    AbstractEntityRequest(final ActorRef<T> replyTo, final EntityId entity) {
        super(replyTo);
        this.entity = entity;
        this.type = entity.requireType();
        this.name = entity.requireName();
    }

    public @NonNull EntityId getEntity() {
        return entity;
    }

    public final @NonNull EntityType getType() {
        return type;
    }

    public final @NonNull EntityName getName() {
        return name;
    }

    @Override
    public String toString() {
        return "AbstractEntityRequest{"
                + "type=" + type
                + ", name=" + name
                + ", entity=" + entity
                + '}';
    }
}
