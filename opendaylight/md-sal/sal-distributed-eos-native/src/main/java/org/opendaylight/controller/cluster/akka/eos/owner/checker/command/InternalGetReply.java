/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.eos.owner.checker.command;

import akka.actor.typed.ActorRef;
import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.typed.javadsl.Replicator;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public class InternalGetReply implements StateCheckerCommand {

    private final Replicator.GetResponse<LWWRegister<String>> response;
    private final DOMEntity entity;
    private final ActorRef<GetOwnershipStateReply> replyTo;

    public InternalGetReply(final Replicator.GetResponse<LWWRegister<String>> response,
                            final DOMEntity entity,
                            final ActorRef<GetOwnershipStateReply> replyTo) {
        this.response = response;
        this.entity = entity;
        this.replyTo = replyTo;
    }

    public Replicator.GetResponse<LWWRegister<String>> getResponse() {
        return response;
    }

    public DOMEntity getEntity() {
        return entity;
    }

    public ActorRef<GetOwnershipStateReply> getReplyTo() {
        return replyTo;
    }
}
