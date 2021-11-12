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
import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.typed.javadsl.Replicator.GetResponse;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public class SingleEntityOwnerDataResponse extends StateCheckerCommand {
    private final @NonNull GetResponse<LWWRegister<String>> response;
    private final DOMEntity entity;
    private final ActorRef<GetEntityReply> replyTo;

    public SingleEntityOwnerDataResponse(final @NonNull GetResponse<LWWRegister<String>> response,
                                         final DOMEntity entity,
                                         final ActorRef<GetEntityReply> replyTo) {
        this.response = requireNonNull(response);
        this.entity = requireNonNull(entity);
        this.replyTo = requireNonNull(replyTo);
    }

    public @NonNull GetResponse<LWWRegister<String>> getResponse() {
        return response;
    }

    public DOMEntity getEntity() {
        return entity;
    }

    public ActorRef<GetEntityReply> getReplyTo() {
        return replyTo;
    }
}
