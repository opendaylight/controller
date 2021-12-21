/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import akka.actor.typed.ActorRef;
import java.io.Serializable;

/**
 * Request sent from Candidate registration actors to clear the candidate from all entities. Issued at start to clear
 * candidates from previous iteration of a node. Owner supervisor responds to this request to notify the registration
 * actor it can start up and process candidate requests.
 */
public class ClearCandidatesForMember extends OwnerSupervisorCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ActorRef<ClearCandidatesResponse> replyTo;
    private final String candidate;

    public ClearCandidatesForMember(final ActorRef<ClearCandidatesResponse> replyTo, final String candidate) {
        this.replyTo = replyTo;
        this.candidate = candidate;
    }

    public ActorRef<ClearCandidatesResponse> getReplyTo() {
        return replyTo;
    }

    public String getCandidate() {
        return candidate;
    }
}
