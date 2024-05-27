/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.pekko.owner.supervisor.command;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.cluster.ddata.ORMap;
import org.apache.pekko.cluster.ddata.ORSet;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public class ClearCandidatesUpdateResponse extends OwnerSupervisorCommand {
    private final Replicator.UpdateResponse<ORMap<DOMEntity, ORSet<String>>> response;
    private final ActorRef<ClearCandidatesResponse> replyTo;

    public ClearCandidatesUpdateResponse(final Replicator.UpdateResponse<ORMap<DOMEntity, ORSet<String>>> response,
                                         final ActorRef<ClearCandidatesResponse> replyTo) {
        this.response = response;
        this.replyTo = replyTo;
    }

    public Replicator.UpdateResponse<ORMap<DOMEntity, ORSet<String>>> getResponse() {
        return response;
    }


    public ActorRef<ClearCandidatesResponse> getReplyTo() {
        return replyTo;
    }
}
