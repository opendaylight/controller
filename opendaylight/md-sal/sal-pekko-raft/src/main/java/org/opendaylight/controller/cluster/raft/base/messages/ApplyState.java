/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.ControlMessage;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Local message sent by a RaftActor to itself to signal state has been applied to the state machine.
 */
public class ApplyState implements ControlMessage {
    private final ActorRef clientActor;
    private final Identifier identifier;
    private final ReplicatedLogEntry replicatedLogEntry;

    public ApplyState(ActorRef clientActor, Identifier identifier, ReplicatedLogEntry replicatedLogEntry) {
        this.clientActor = clientActor;
        this.identifier = identifier;
        this.replicatedLogEntry = replicatedLogEntry;
    }

    public ActorRef getClientActor() {
        return clientActor;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public ReplicatedLogEntry getReplicatedLogEntry() {
        return replicatedLogEntry;
    }

    @Override
    public String toString() {
        return "ApplyState [identifier=" + identifier + ", replicatedLogEntry=" + replicatedLogEntry + "]";
    }
}
