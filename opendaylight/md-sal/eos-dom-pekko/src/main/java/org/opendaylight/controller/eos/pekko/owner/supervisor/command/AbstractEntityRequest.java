/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.pekko.owner.supervisor.command;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.pattern.StatusReply;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityType;

public abstract class AbstractEntityRequest<T extends OwnerSupervisorReply> extends OwnerSupervisorRequest<T> {
    private static final long serialVersionUID = 1L;

    private final @NonNull EntityType type;
    private final @NonNull EntityName name;

    AbstractEntityRequest(final ActorRef<StatusReply<T>> replyTo, final EntityId entity) {
        super(replyTo);
        this.type = entity.requireType();
        this.name = entity.requireName();
    }

    public final @NonNull EntityType getType() {
        return type;
    }

    public final @NonNull EntityName getName() {
        return name;
    }
}
