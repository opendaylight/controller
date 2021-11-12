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
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.typed.javadsl.Replicator.GetResponse;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public final class GetCandidatesForEntity extends StateCheckerCommand {
    private final @Nullable GetResponse<ORMap<DOMEntity, ORSet<String>>> response;
    private final @NonNull DOMEntity entity;
    private final @NonNull ActorRef<GetEntityReply> replyTo;

    public GetCandidatesForEntity(final GetResponse<ORMap<DOMEntity, ORSet<String>>> response,
                                  final DOMEntity entity, final ActorRef<GetEntityReply> replyTo) {
        this.response = response;
        this.entity = requireNonNull(entity);
        this.replyTo = requireNonNull(replyTo);
    }

    public @Nullable GetResponse<ORMap<DOMEntity, ORSet<String>>> getResponse() {
        return response;
    }

    public @NonNull DOMEntity getEntity() {
        return entity;
    }

    public @NonNull ActorRef<GetEntityReply> getReplyTo() {
        return replyTo;
    }
}
