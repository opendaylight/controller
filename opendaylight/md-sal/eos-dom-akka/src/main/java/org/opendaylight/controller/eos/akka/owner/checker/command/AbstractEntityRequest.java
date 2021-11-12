/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.checker.command;

import akka.actor.typed.ActorRef;
import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.get.entities.output.EntitiesBuilder;

public abstract class AbstractEntityRequest<T extends StateCheckerReply> extends StateCheckerRequest<T> {
    private static final long serialVersionUID = 1L;

    private final @NonNull EntityType type;
    private final @NonNull EntityName name;

    AbstractEntityRequest(final ActorRef<T> replyTo, final EntityId entity) {
        super(replyTo);
        type = entity.requireType();
        name = entity.requireName();
    }

    public final @NonNull EntityId getEntity() {
        return new EntitiesBuilder().setType(type).setName(name).build();
    }

    public final @NonNull EntityType getType() {
        return type;
    }

    public final @NonNull EntityName getName() {
        return name;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("type", type).add("name", name).toString();
    }
}
