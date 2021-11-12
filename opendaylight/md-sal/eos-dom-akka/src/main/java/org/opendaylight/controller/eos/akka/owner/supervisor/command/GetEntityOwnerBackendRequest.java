/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import akka.actor.typed.ActorRef;
import akka.pattern.StatusReply;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityId;

public final class GetEntityOwnerBackendRequest extends AbstractEntityRequest<GetEntityOwnerBackendReply> {
    private static final long serialVersionUID = 1L;

    public GetEntityOwnerBackendRequest(final ActorRef<StatusReply<GetEntityOwnerBackendReply>> replyTo,
                                        final EntityId entity) {
        super(replyTo, entity);
    }
}
