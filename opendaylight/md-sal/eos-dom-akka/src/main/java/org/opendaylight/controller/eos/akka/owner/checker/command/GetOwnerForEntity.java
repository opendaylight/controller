/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.checker.command;

import akka.actor.typed.ActorRef;
import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.typed.javadsl.Replicator.GetResponse;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public class GetOwnerForEntity extends StateCheckerCommand {
    private final @NonNull GetResponse<LWWRegister<String>> response;
    private final DOMEntity entity;
    private final ActorRef<GetEntityOwnerReply> replyTo;

    public GetOwnerForEntity(final @NonNull GetResponse<LWWRegister<String>> response,
                             final DOMEntity entity, final ActorRef<GetEntityOwnerReply> replyTo) {
        this.response = response;
        this.entity = entity;
        this.replyTo = replyTo;
    }

    public GetResponse<LWWRegister<String>> getResponse() {
        return response;
    }

    public DOMEntity getEntity() {
        return entity;
    }

    public ActorRef<GetEntityOwnerReply> getReplyTo() {
        return replyTo;
    }
}
