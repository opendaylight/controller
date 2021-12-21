/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import akka.actor.typed.ActorRef;
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.typed.javadsl.Replicator;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public class ClearCandidatesUpdateResponse extends OwnerSupervisorCommand {
    private final Replicator.UpdateResponse<ORMap<DOMEntity, ORSet<String>>> response;
    private final AtomicInteger responseCounter;
    private final ActorRef<ClearCandidatesResponse> replyTo;

    public ClearCandidatesUpdateResponse(final Replicator.UpdateResponse<ORMap<DOMEntity, ORSet<String>>> response,
                                         final AtomicInteger responseCounter,
                                         final ActorRef<ClearCandidatesResponse> replyTo) {
        this.response = response;
        this.responseCounter = responseCounter;
        this.replyTo = replyTo;
    }

    public Replicator.UpdateResponse<ORMap<DOMEntity, ORSet<String>>> getResponse() {
        return response;
    }

    public AtomicInteger getResponseCounter() {
        return responseCounter;
    }

    public ActorRef<ClearCandidatesResponse> getReplyTo() {
        return replyTo;
    }
}
