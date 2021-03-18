/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.checker.command;

import static java.util.Objects.requireNonNull;

import akka.actor.typed.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public final class GetOwnershipState extends StateCheckerCommand {
    private final @NonNull DOMEntity entity;
    private final @NonNull ActorRef<GetOwnershipStateReply> replyTo;

    public GetOwnershipState(final DOMEntity entity, final ActorRef<GetOwnershipStateReply> replyTo) {
        this.entity = requireNonNull(entity);
        this.replyTo = requireNonNull(replyTo);
    }

    public @NonNull DOMEntity getEntity() {
        return entity;
    }

    public @NonNull ActorRef<GetOwnershipStateReply> getReplyTo() {
        return replyTo;
    }
}
